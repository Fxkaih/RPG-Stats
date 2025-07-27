package rpg.stats.rpg_stats.managers;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.*;
import rpg.stats.rpg_stats.events.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class PlayerProgress implements Listener {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final String dataFolderPath;
    private final AttributeManager attributeManager;
    private final RPGClassManager classManager;
    private final XPDisplay xpDisplay;

    // Configuración por defecto
    private int defaultLevel = 1;
    private float defaultXP = 0;
    private int defaultAvailablePoints = 0;
    private int defaultStrength = 1;
    private int defaultDexterity = 1;
    private int defaultConstitution = 1;
    private int defaultMana = 100;
    private int defaultMaxMana = 100;
    private String defaultClass = "none";

    // Datos de jugadores
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();

    public PlayerProgress(@NotNull JavaPlugin plugin, @NotNull FileConfiguration config, @NotNull XPDisplay xpDisplay) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = Objects.requireNonNull(config);
        this.xpDisplay = Objects.requireNonNull(xpDisplay);
        this.dataFolderPath = plugin.getDataFolder().getAbsolutePath() + File.separator + "playerdata" + File.separator;
        this.attributeManager = new AttributeManager(config);
        this.classManager = new RPGClassManager(config);

        reloadConfig(config);
        ensureDataFolderExists();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ========== DATOS DEL JUGADOR ==========
    public @NotNull PlayerData getPlayerData(@NotNull Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), k -> createNewPlayerData());
    }

    private @NotNull PlayerData createNewPlayerData() {
        return new PlayerData(
                defaultLevel,
                defaultXP,
                defaultAvailablePoints,
                defaultStrength,
                defaultDexterity,
                defaultConstitution,
                defaultMana,
                defaultMaxMana,
                defaultClass
        );
    }

    // ========== PERSISTENCIA ==========
    public void savePlayerData(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolderPath + uuid + ".yml");
        YamlConfiguration data = new YamlConfiguration();

        PlayerData pd = getPlayerData(player);

        try {
            data.set("level", pd.getLevel());
            data.set("xp", pd.getXp());
            data.set("availablePoints", pd.getAvailablePoints());
            data.set("class", pd.getPlayerClass());
            data.set("mana", pd.getMana());
            data.set("maxMana", pd.getMaxMana());

            ConfigurationSection attributes = data.createSection("attributes");
            attributes.set("fuerza", pd.getStrength());
            attributes.set("destreza", pd.getDexterity());
            attributes.set("constitucion", pd.getConstitution());

            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar datos de " + player.getName() + ": " + e.getMessage());
        }
    }

    public void loadPlayerData(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolderPath + uuid + ".yml");

        if (file.exists()) {
            try {
                YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
                PlayerData pd = new PlayerData(
                        data.getInt("level", defaultLevel),
                        (float) data.getDouble("xp", defaultXP),
                        data.getInt("availablePoints", defaultAvailablePoints),
                        data.getInt("attributes.fuerza", defaultStrength),
                        data.getInt("attributes.destreza", defaultDexterity),
                        data.getInt("attributes.constitucion", defaultConstitution),
                        data.getInt("mana", defaultMana),
                        data.getInt("maxMana", defaultMaxMana),
                        data.getString("class", defaultClass)
                );

                playerData.put(uuid, pd);
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar datos de " + player.getName() + ", usando valores por defecto");
                playerData.put(uuid, createNewPlayerData());
            }
        } else {
            playerData.put(uuid, createNewPlayerData());
        }

        applyAllAttributeEffects(player);
        xpDisplay.updateDisplay(player);
    }

    // ========== SISTEMA DE NIVELES Y XP ==========
    public void addXP(@NotNull Player player, @NotNull String actionType, float amount) {
        if (amount <= 0) return;

        PlayerData pd = getPlayerData(player);
        float multiplier = getXPMultiplier(player, actionType);
        float finalXP = amount * multiplier;

        pd.setXp(pd.getXp() + finalXP);
        checkLevelUp(player);
        xpDisplay.updateDisplay(player);

        if (plugin.getConfig().getBoolean("debug.xp-gain", false)) {
            plugin.getLogger().info(player.getName() + " ganó " + finalXP + " XP (" + amount + " base * " + multiplier + " multiplicador)");
        }
    }

    private void checkLevelUp(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);
        float xpNeeded = getXPToNextLevel(player);

        if (pd.getXp() >= xpNeeded && pd.getLevel() < config.getInt("levels.max-level", 100)) {
            int newLevel = pd.getLevel() + 1;
            pd.setLevel(newLevel);
            pd.setXp(pd.getXp() - xpNeeded);
            grantLevelUpPoints(player, newLevel);

            Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(player, newLevel - 1, newLevel));
            xpDisplay.updateDisplay(player);

            if (plugin.getConfig().getBoolean("debug.level-up", false)) {
                plugin.getLogger().info(player.getName() + " subió al nivel " + newLevel);
            }
        }
    }

    public float getXPToNextLevel(@NotNull Player player) {
        int level = getPlayerData(player).getLevel();
        return calculateXPForNextLevel(level);
    }

    private float calculateXPForNextLevel(int level) {
        return config.getInt("levels.xp-base", 100) + (level * config.getInt("levels.xp-increment", 50));
    }

    private float getXPMultiplier(@NotNull Player player, @NotNull String actionType) {
        float multiplier = 1.0f;
        String playerClass = getPlayerData(player).getPlayerClass();

        // Multiplicador por atributo
        switch (actionType.toLowerCase()) {
            case "mining":
                multiplier *= (1 + (getPlayerData(player).getStrength() * 0.01f));
                break;
            case "combat":
                multiplier *= (1 + (getPlayerData(player).getDexterity() * 0.01f));
                break;
        }

        // Multiplicador por clase
        if (playerClass != null) {
            multiplier *= (float) config.getDouble("classes." + playerClass + ".xp-multipliers." + actionType, 1.0);
        }

        return Math.max(0.1f, multiplier); // Mínimo 10% de ganancia
    }

    private void grantLevelUpPoints(@NotNull Player player, int newLevel) {
        PlayerData pd = getPlayerData(player);
        int basePoints = config.getInt("levels.points-per-level", 1);
        int extraPoints = 0;

        // Puntos extra cada ciertos niveles
        int interval = config.getInt("levels.extra-point-interval", 5);
        if (interval > 0 && newLevel % interval == 0) {
            extraPoints = config.getInt("levels.extra-point-amount", 1);
        }

        pd.setAvailablePoints(pd.getAvailablePoints() + basePoints + extraPoints);
        pd.setMaxMana(pd.getMaxMana() + config.getInt("levels.mana-per-level", 5));
        pd.setMana(pd.getMaxMana()); // Restaurar maná al máximo al subir de nivel

        player.sendMessage(String.format("§a¡Subiste al nivel §e%d§a! §7(Puntos: §6+%d§7, Maná: §b+%d§7)",
                newLevel, basePoints + extraPoints, config.getInt("levels.mana-per-level", 5)));
    }

    // ========== SISTEMA DE MANÁ ==========
    public int getCurrentMana(@NotNull Player player) {
        return getPlayerData(player).getMana();
    }

    public int getMaxMana(@NotNull Player player) {
        return getPlayerData(player).getMaxMana();
    }

    public void setMana(@NotNull Player player, int amount) {
        PlayerData pd = getPlayerData(player);
        pd.setMana(Math.max(0, Math.min(amount, pd.getMaxMana())));
        xpDisplay.updateDisplay(player); // Actualizar la barra de XP que ahora muestra maná
    }

    public boolean consumeMana(@NotNull Player player, int amount) {
        PlayerData pd = getPlayerData(player);
        if (pd.getMana() >= amount) {
            pd.setMana(pd.getMana() - amount);
            xpDisplay.updateDisplay(player);
            return true;
        }
        return false;
    }

    public void regenerateMana(@NotNull Player player, int amount) {
        PlayerData pd = getPlayerData(player);
        pd.setMana(Math.min(pd.getMana() + amount, pd.getMaxMana()));
        xpDisplay.updateDisplay(player);
    }

    // ========== SISTEMA DE ATRIBUTOS ==========
    public int getAttribute(@NotNull Player player, @NotNull String attribute) {
        return switch (attribute.toLowerCase()) {
            case "fuerza" -> getPlayerData(player).getStrength();
            case "destreza" -> getPlayerData(player).getDexterity();
            case "constitucion" -> getPlayerData(player).getConstitution();
            default -> 1;
        };
    }

    public void setAttribute(@NotNull Player player, @NotNull String attribute, int value) {
        PlayerData pd = getPlayerData(player);
        switch (attribute.toLowerCase()) {
            case "fuerza":
                pd.setStrength(value);
                applyStrengthEffects(player, value);
                break;
            case "destreza":
                pd.setDexterity(value);
                applyDexterityEffects(player, value);
                break;
            case "constitucion":
                pd.setConstitution(value);
                applyConstitutionEffects(player, value);
                break;
        }
    }

    public void addAttributePoint(@NotNull Player player, @NotNull String attribute) {
        PlayerData pd = getPlayerData(player);
        if (pd.getAvailablePoints() <= 0) {
            player.sendMessage("§cNo tienes puntos de atributo disponibles");
            return;
        }

        int currentValue = getAttribute(player, attribute);
        int maxValue = attributeManager.getMaxValue(attribute);

        if (currentValue >= maxValue) {
            player.sendMessage("§c¡No puedes aumentar " + attribute + " más allá de " + maxValue + "!");
            return;
        }

        Bukkit.getPluginManager().callEvent(new AttributeChangeEvent(player, attribute, currentValue, currentValue + 1));
        setAttribute(player, attribute, currentValue + 1);
        pd.setAvailablePoints(pd.getAvailablePoints() - 1);

        player.sendMessage("§a¡" + attribute.substring(0, 1).toUpperCase() + attribute.substring(1) +
                " aumentado a §e" + (currentValue + 1) + "§a!");
    }

    private void applyAllAttributeEffects(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);
        applyStrengthEffects(player, pd.getStrength());
        applyDexterityEffects(player, pd.getDexterity());
        applyConstitutionEffects(player, pd.getConstitution());
    }

    private void applyStrengthEffects(@NotNull Player player, int level) {
        Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_DAMAGE))
                .setBaseValue(1.0 + (0.5 * level));
    }

    private void applyDexterityEffects(@NotNull Player player, int level) {
        player.setWalkSpeed((float) Math.min(0.2 + (0.01 * level), 1.0f));
    }

    private void applyConstitutionEffects(@NotNull Player player, int level) {
        double maxHealth = 20.0 + (2.0 * level);
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH))
                .setBaseValue(maxHealth);
        if (player.getHealth() < maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    // ========== SISTEMA DE CLASES ==========
    public String getPlayerClass(@NotNull Player player) {
        return getPlayerData(player).getPlayerClass();
    }

    public boolean setPlayerClass(@NotNull Player player, @NotNull String className) {
        if (!classManager.isValidClass(className)) {
            return false;
        }

        PlayerData pd = getPlayerData(player);
        pd.setPlayerClass(className);

        // Aplicar bonificaciones de clase
        int strengthBonus = config.getInt("classes." + className + ".attribute-bonuses.fuerza", 0);
        int dexterityBonus = config.getInt("classes." + className + ".attribute-bonuses.destreza", 0);
        int constitutionBonus = config.getInt("classes." + className + ".attribute-bonuses.constitucion", 0);

        pd.setStrength(pd.getStrength() + strengthBonus);
        pd.setDexterity(pd.getDexterity() + dexterityBonus);
        pd.setConstitution(pd.getConstitution() + constitutionBonus);

        applyAllAttributeEffects(player);
        return true;
    }

    // ========== MÉTODOS AUXILIARES ==========
    private void ensureDataFolderExists() {
        File folder = new File(dataFolderPath);
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("No se pudo crear la carpeta de datos: " + dataFolderPath);
        }
    }

    public void reloadConfig(@NotNull Configuration config) {
        ConfigurationSection defaults = config.getConfigurationSection("default-stats");
        if (defaults != null) {
            this.defaultLevel = defaults.getInt("level", 1);
            this.defaultXP = (float) defaults.getDouble("xp", 0.0);
            this.defaultAvailablePoints = defaults.getInt("available-points", 0);
            this.defaultMana = defaults.getInt("mana", 100);
            this.defaultMaxMana = defaults.getInt("max-mana", 100);

            ConfigurationSection attrs = defaults.getConfigurationSection("attributes");
            if (attrs != null) {
                this.defaultStrength = attrs.getInt("fuerza", 1);
                this.defaultDexterity = attrs.getInt("destreza", 1);
                this.defaultConstitution = attrs.getInt("constitucion", 1);
            }

            this.defaultClass = defaults.getString("class", "none");
        }
    }

    // ========== GETTERS BÁSICOS ==========
    public int getLevel(@NotNull Player player) {
        return getPlayerData(player).getLevel();
    }

    public void setLevel(@NotNull Player player, int level) {
        getPlayerData(player).setLevel(level);
    }

    public float getCurrentXP(@NotNull Player player) {
        return getPlayerData(player).getXp();
    }

    public void setXP(@NotNull Player player, float xp) {
        getPlayerData(player).setXp(xp);
    }

    public int getAvailablePoints(@NotNull Player player) {
        return getPlayerData(player).getAvailablePoints();
    }

    public void setAvailablePoints(@NotNull Player player, int points) {
        getPlayerData(player).setAvailablePoints(points);
    }

    public AttributeManager getAttributeManager() {
        return attributeManager;
    }


    // ========== CLASE PlayerData INTERNA ==========
    public static class PlayerData {
        private int level;
        private float xp;
        private int availablePoints;
        private int strength;
        private int dexterity;
        private int constitution;
        private int mana;
        private int maxMana;
        private String playerClass;

        public PlayerData(int level, float xp, int availablePoints,
                          int strength, int dexterity, int constitution,
                          int mana, int maxMana, String playerClass) {
            this.level = level;
            this.xp = xp;
            this.availablePoints = availablePoints;
            this.strength = strength;
            this.dexterity = dexterity;
            this.constitution = constitution;
            this.mana = mana;
            this.maxMana = maxMana;
            this.playerClass = playerClass;
        }

        // Getters
        public int getLevel() { return level; }
        public float getXp() { return xp; }
        public int getAvailablePoints() { return availablePoints; }
        public int getStrength() { return strength; }
        public int getDexterity() { return dexterity; }
        public int getConstitution() { return constitution; }
        public int getMana() { return mana; }
        public int getMaxMana() { return maxMana; }
        public String getPlayerClass() { return playerClass; }

        // Setters con validación
        public void setLevel(int level) { this.level = Math.max(1, level); }
        public void setXp(float xp) { this.xp = Math.max(0, xp); }
        public void setAvailablePoints(int points) { this.availablePoints = Math.max(0, points); }
        public void setStrength(int strength) { this.strength = Math.max(1, strength); }
        public void setDexterity(int dexterity) { this.dexterity = Math.max(1, dexterity); }
        public void setConstitution(int constitution) { this.constitution = Math.max(1, constitution); }
        public void setMana(int mana) { this.mana = Math.max(0, Math.min(mana, maxMana)); }
        public void setMaxMana(int maxMana) {
            this.maxMana = Math.max(10, maxMana);
            this.mana = Math.min(mana, maxMana);
        }
        public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    }
}