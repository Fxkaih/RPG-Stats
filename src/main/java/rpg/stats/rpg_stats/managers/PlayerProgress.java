package rpg.stats.rpg_stats.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import rpg.stats.rpg_stats.events.AttributeChangeEvent;
import rpg.stats.rpg_stats.events.PlayerLevelUpEvent;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerProgress {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private XPDisplay xpDisplay;
    private final AttributeManager attributeManager;
    private final RPGClassManager classManager;
    private final String dataFolderPath;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerClasses = new HashMap<>();

    private int defaultLevel = 1;
    private float defaultXP = 0;
    private int defaultAvailablePoints = 0;
    private int defaultStrength = 1;
    private int defaultDexterity = 1;
    private int defaultConstitution = 1;
    private int defaultMana = 100;
    private int defaultMaxMana = 100;
    private String defaultClass = "none";

    public PlayerProgress(@NotNull JavaPlugin plugin, @NotNull FileConfiguration config, XPDisplay xpDisplay) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = Objects.requireNonNull(config);
        this.dataFolderPath = plugin.getDataFolder().getAbsolutePath() + File.separator + "playerdata" + File.separator;
        this.attributeManager = new AttributeManager(config, plugin);
        this.classManager = new RPGClassManager(config);
        this.xpDisplay = xpDisplay;

        reloadConfig(config);
        ensureDataFolderExists();
    }

    public void setXpDisplay(@NotNull XPDisplay xpDisplay) {
        this.xpDisplay = Objects.requireNonNull(xpDisplay);
    }

    public @NotNull PlayerData getPlayerData(@NotNull Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), k -> createNewPlayerData());
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

    public void savePlayerData(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolderPath + uuid + ".yml");
        YamlConfiguration data = new YamlConfiguration();

        PlayerData pd = getPlayerData(player);
        ConfigurationSection section = data.createSection("player-data");

        // Datos básicos
        section.set("level", pd.getLevel());
        section.set("xp", pd.getXp());
        section.set("available-points", pd.getAvailablePoints());

        // Atributos
        ConfigurationSection attrs = section.createSection("attributes");
        attrs.set("strength", pd.getStrength());
        attrs.set("dexterity", pd.getDexterity());
        attrs.set("constitution", pd.getConstitution());

        // Maná
        section.set("mana", pd.getMana());
        section.set("max-mana", pd.getMaxMana());

        // Clase
        section.set("class", pd.getPlayerClass());

        // Metadata adicional
        if (!pd.getMetadata().isEmpty()) {
            section.set("metadata", pd.getMetadata());
        }

        try {
            data.save(file);
            plugin.getLogger().info("[SAVE] Datos de " + player.getName() + " guardados");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar datos de " + player.getName() + ": " + e.getMessage());
        }
    }

    public void loadPlayerData(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        File file = new File(dataFolderPath + playerId + ".yml");

        if (!file.exists()) {
            handleNewPlayerSetup(player);
            return;
        }

        try {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = data.getConfigurationSection("player-data");

            if (section == null) {
                throw new IllegalStateException("Sección 'player-data' no encontrada");
            }

            PlayerData pd = new PlayerData(
                    section.getInt("level", defaultLevel),
                    (float) section.getDouble("xp", defaultXP),
                    section.getInt("available-points", defaultAvailablePoints),
                    section.getInt("attributes.strength", defaultStrength),
                    section.getInt("attributes.dexterity", defaultDexterity),
                    section.getInt("attributes.constitution", defaultConstitution),
                    section.getInt("mana", defaultMana),
                    section.getInt("max-mana", defaultMaxMana),
                    section.getString("class", defaultClass)
            );

            // Cargar metadata adicional
            if (section.isConfigurationSection("metadata")) {
                section.getConfigurationSection("metadata").getValues(false)
                        .forEach((key, value) -> pd.setMetadata(key, (int) value));
            }

            playerDataMap.put(playerId, pd);
            plugin.getLogger().info("[LOAD] Datos de " + player.getName() + " cargados");

        } catch (Exception e) {
            plugin.getLogger().warning("Error al cargar datos de " + player.getName() + ": " + e.getMessage());
            playerDataMap.put(playerId, createNewPlayerData());
        }

        applyAllAttributeEffects(player);
        updatePlayerDisplay(player);
    }

    public void handleNewPlayerSetup(@NotNull Player player) {
        assignDefaultClass(player);
        player.sendMessage(Component.text("¡Bienvenido! Se te ha asignado una clase por defecto.", NamedTextColor.GREEN));
        showAvailableClasses(player);

        PlayerData pd = getPlayerData(player);
        pd.setLevel(1);
        pd.setXp(0);
    }

    public void updatePlayerDisplay(@NotNull Player player) {
        if (xpDisplay != null) {
            xpDisplay.updateDisplay(player);
        }
    }

    public void addXP(@NotNull Player player, @NotNull String actionType, float amount) {
        if (amount <= 0) return;

        PlayerData pd = getPlayerData(player);
        float multiplier = getXPMultiplier(player, actionType);
        float finalXP = amount * multiplier;

        // Debug logging
        plugin.getLogger().info("[XP] " + player.getName() + " ganó " + finalXP + " XP (" + actionType + ")");

        pd.setXp(pd.getXp() + finalXP);
        updatePlayerDisplay(player);

        // Verificar inmediatamente si subió de nivel
        while (pd.getXp() >= getXPToNextLevel(player) && pd.getLevel() < config.getInt("levels.max-level", 100)) {
            checkLevelUp(player);
        }
    }

    private void checkLevelUp(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);
        float xpNeeded = getXPToNextLevel(player);

        if (pd.getXp() >= xpNeeded) {
            int oldLevel = pd.getLevel();
            int newLevel = oldLevel + 1;

            pd.setLevel(newLevel);
            pd.setXp(pd.getXp() - xpNeeded);

            int pointsGained = calculatePointsGained(newLevel);
            int manaIncrease = config.getInt("levels.mana-per-level", 5);

            pd.setAvailablePoints(pd.getAvailablePoints() + pointsGained);
            pd.setMaxMana(pd.getMaxMana() + manaIncrease);
            pd.setMana(pd.getMaxMana());

            // Debug logging
            plugin.getLogger().info("[LEVEL] " + player.getName() + " subió a nivel " + newLevel);

            plugin.getServer().getPluginManager()
                    .callEvent(new PlayerLevelUpEvent(player, oldLevel, newLevel, pointsGained, manaIncrease));

            onLevelUp(player, newLevel, player.getUniqueId(), pointsGained, manaIncrease);
            updatePlayerDisplay(player);
        }
    }

    private int calculatePointsGained(int newLevel) {
        int basePoints = config.getInt("levels.points-per-level", 1);
        int extraPoints = 0;

        int interval = config.getInt("levels.extra-point-interval", 5);
        if (interval > 0 && newLevel % interval == 0) {
            extraPoints = config.getInt("levels.extra-point-amount", 1);
        }
        return basePoints + extraPoints;
    }

    public float getXPToNextLevel(@NotNull Player player) {
        int level = getPlayerData(player).getLevel();
        int base = config.getInt("levels.xp-base", 100);
        int increment = config.getInt("levels.xp-increment", 50);
        float scaling = (float) config.getDouble("levels.xp-scaling", 1.1);

        // Fórmula exponencial para requerimientos de XP
        return base + (increment * level) * (float) Math.pow(scaling, level);
    }


    private float calculateXPForNextLevel(int level) {
        return config.getInt("levels.xp-base", 100) + (level * config.getInt("levels.xp-increment", 50));
    }

    private float getXPMultiplier(@NotNull Player player, @NotNull String actionType) {
        PlayerData pd = getPlayerData(player);
        float multiplier = 1.0f;

        // Multiplicadores por atributos
        switch (actionType.toLowerCase()) {
            case "mining":
                multiplier *= (1 + (pd.getStrength() * 0.02f));  // +2% por punto de fuerza
                break;
            case "combat":
                multiplier *= (1 + (pd.getDexterity() * 0.015f)); // +1.5% por punto de destreza
                break;
            case "farming":
                multiplier *= (1 + (pd.getConstitution() * 0.01f)); // +1% por punto de constitución
                break;
        }

        // Multiplicador por clase
        RPGClassManager.RPGClass rpgClass = classManager.getRPGClass(pd.getPlayerClass());
        if (rpgClass != null) {
            multiplier *= rpgClass.getXPMultiplier(actionType);
        }

        // Multiplicador global de configuración
        multiplier *= (float) config.getDouble("xp-settings.global-multiplier", 1.0);

        return Math.max(0.1f, Math.min(multiplier, 5.0f)); // Limitar entre 0.1x y 5.0x
    }

    public void onLevelUp(@NotNull Player player, int newLevel, @NotNull UUID playerId,
                          int pointsGained, int manaIncrease) {
        if (!player.getUniqueId().equals(playerId)){
            plugin.getLogger().warning("ID de jugador no coincide en onLevelUp");
            return;
        }
        PlayerData pd = getPlayerData(player);
        pd.setAvailablePoints(pd.getAvailablePoints() + pointsGained);
        pd.setMaxMana(pd.getMaxMana() + manaIncrease);
        pd.setMana(pd.getMaxMana());

        player.showTitle(Title.title(
                Component.text("¡Nivel " + newLevel + "!", NamedTextColor.GOLD),
                Component.text("Felicidades", NamedTextColor.YELLOW),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(3500),
                        Duration.ofMillis(1000)
                )));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);

        if (newLevel % 5 == 0){
            player.sendMessage(Component.text("¡Has alcanzado el nivel " + newLevel + " ¡Felicidades", NamedTextColor.GOLD));
        }

        RPGClassManager classManager = getClassManager();
        String playerClass = getCurrentClass(playerId);
        if (playerClass != null) {
            player.sendMessage(Component.text("§bBonificación de nivel para " + playerClass, NamedTextColor.AQUA));
            classManager.checkLevelUpBonuses(player, this, newLevel);
        }

        player.sendMessage(String.format("§a¡Subiste al nivel §e%d§a! §7(Puntos: §6+%d§7, Maná: §b+%d§7)",
                newLevel, pointsGained, manaIncrease));

        if (getClassManager().hasDefaultClass() && playerClass == null) {
            setPlayerClass(player, getClassManager().getDefaultClass());
        }
    }

    public int getCurrentMana(@NotNull Player player) {
        return getPlayerData(player).getMana();
    }

    public int getMaxMana(@NotNull Player player) {
        return getPlayerData(player).getMaxMana();
    }

    public void setMaxMana(@NotNull Player player, int maxMana) {
        PlayerData pd = getPlayerData(player);
        int oldMaxMana = pd.getMaxMana();

        int newMaxMana = Math.max(10, maxMana);
        pd.setMaxMana(newMaxMana);

        if (pd.getMana() > newMaxMana) {
            pd.setMana(newMaxMana);
        }

        if (oldMaxMana != newMaxMana) {
            plugin.getServer().getPluginManager().callEvent(
                    new AttributeChangeEvent(player, "max_mana", oldMaxMana, newMaxMana, false));
        }

        updatePlayerDisplay(player);
    }

    public void setMana(@NotNull Player player, int amount) {
        PlayerData pd = getPlayerData(player);
        pd.setMana(Math.max(0, Math.min(amount, pd.getMaxMana())));
        updatePlayerDisplay(player);
    }

    public boolean consumeMana(@NotNull Player player, int amount) {
        PlayerData pd = getPlayerData(player);
        if (pd.getMana() >= amount) {
            pd.setMana(pd.getMana() - amount);
            updatePlayerDisplay(player);
            return true;
        }
        return false;
    }

    public void regenerateMana(@NotNull Player player, int amount) {
        PlayerData pd = getPlayerData(player);
        pd.setMana(Math.min(pd.getMana() + amount, pd.getMaxMana()));
        updatePlayerDisplay(player);
    }

    public int getAttribute(@NotNull Player player, @NotNull String attribute) {
        PlayerData pd = getPlayerData(player);
        return switch (attribute.toLowerCase()) {
            case "fuerza" -> pd.getStrength();
            case "destreza" -> pd.getDexterity();
            case "constitucion" -> pd.getConstitution();
            case "inteligencia" -> pd.getMetadata("inteligencia", 1);
            case "sabiduria" -> pd.getMetadata("sabiduria", 1);
            case "precision" -> pd.getMetadata("precision", 1);
            case "agilidad" -> pd.getMetadata("agilidad", 1);
            default -> 1;
        };
    }

    public void setAttribute(@NotNull Player player, @NotNull String attribute,
                             int value, boolean isLevelUp) {
        PlayerData pd = getPlayerData(player);
        int oldValue = getAttribute(player, attribute);
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
            default:
                pd.setMetadata(attribute, value);
                attributeManager.applyAttributeEffects(player, attribute, value);
        }
        plugin.getServer().getPluginManager().callEvent(
                new AttributeChangeEvent(player, attribute, oldValue, value, isLevelUp));

        updatePlayerDisplay(player);
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

        setAttribute(player, attribute, currentValue + 1, false);
        pd.setAvailablePoints(pd.getAvailablePoints() - 1);
        player.sendMessage("§a¡" + attribute.substring(0, 1).toUpperCase() + attribute.substring(1) +
                " aumentado a §e" + (currentValue + 1) + "§a!");
    }

    public void applyAllAttributeEffects(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);
        setAttribute(player, "fuerza", pd.getStrength(), false);
        setAttribute(player, "destreza", pd.getDexterity(), false);
        setAttribute(player, "constitucion", pd.getConstitution(), false);
        attributeManager.applyAttributeEffects(player, "fuerza", pd.getStrength());
        attributeManager.applyAttributeEffects(player, "destreza", pd.getDexterity());
        attributeManager.applyAttributeEffects(player, "constitucion", pd.getConstitution());
        attributeManager.applyAttributeEffects(player, "inteligencia", pd.getMetadata("inteligencia", 1));
        attributeManager.applyAttributeEffects(player, "sabiduria", pd.getMetadata("sabiduria", 1));
        attributeManager.applyAttributeEffects(player, "precision", pd.getMetadata("precision", 1));
        attributeManager.applyAttributeEffects(player, "agilidad", pd.getMetadata("agilidad", 1));
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

    public String getCurrentClass(@NotNull UUID playerId) {
        return playerClasses.get(playerId);
    }

    public boolean setPlayerClass(@NotNull Player player, @NotNull String classId) {
        Objects.requireNonNull(player, "Player no puede ser nulo");
        String lowerClassId = classId.toLowerCase();
        RPGClassManager classManager = getClassManager();

        if (!classManager.hasClass(lowerClassId)) {
            player.sendMessage(Component.text("Clase no válida: " + classId, NamedTextColor.RED));
            return false;
        }

        PlayerData pd = getPlayerData(player);
        String currentClass = pd.getPlayerClass();

        if (lowerClassId.equals(currentClass)) {
            player.sendMessage(Component.text("Ya tienes esta clase asignada", NamedTextColor.YELLOW));
            return true;
        }

        if (classManager.isValidClass(currentClass)) {
            removeClassBonuses(player, currentClass);
        }

        pd.setPlayerClass(lowerClassId);
        playerClasses.put(player.getUniqueId(), lowerClassId);

        classManager.sendClassBenefitsMessage(player, lowerClassId);
        applyClassBonuses(player, lowerClassId);

        player.showTitle(Title.title(
                Component.text("Nueva Clase", NamedTextColor.YELLOW),
                Component.text(classManager.getClassDisplayName(lowerClassId), NamedTextColor.GREEN),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(3500),
                        Duration.ofMillis(1000)
                )));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        applyAllAttributeEffects(player);

        return true;
    }

    public void assignDefaultClass(@NotNull Player player) {
        RPGClassManager classManager = getClassManager();
        if (classManager.hasDefaultClass()) {
            boolean success = setPlayerClass(player, classManager.getDefaultClass());
            if (!success) {
                player.sendMessage(Component.text("No se pudo asignar la clase por defecto", NamedTextColor.RED));
            }
        }
    }

    private void applyClassBonuses(@NotNull Player player, @NotNull String className) {
        RPGClassManager.RPGClass rpgClass = classManager.getRPGClass(className);
        if (rpgClass != null) {
            rpgClass.getAttributeBonuses().forEach((attr, bonus) -> {
                int current = getAttribute(player, attr);
                setAttribute(player, attr, current + bonus, false);
            });
            player.sendMessage(Component.text("§aBonificaciones de " + className + " aplicadas"));
            rpgClass.applyEffects(player, this);
        }
    }

    private void removeClassBonuses(@NotNull Player player, @NotNull String className) {
        RPGClassManager.RPGClass rpgClass = classManager.getRPGClass(className);
        if (rpgClass != null) {
            rpgClass.getAttributeBonuses().forEach((attr, bonus) -> {
                int current = getAttribute(player, attr);
                setAttribute(player, attr, current - bonus, false);
            });
        }
    }

    public List<String> getAvailableClasses(@NotNull Player player) {
        Objects.requireNonNull(player, "El jugador no puede ser nulo");
        return classManager.getAvailableClasses().keySet().stream()
                .filter(classId -> player.hasPermission("rpgstats.class." + classId) ||
                        classId.equals(classManager.getDefaultClass()))
                .collect(Collectors.toList());
    }

    public void showAvailableClasses(@NotNull Player player) {
        List<String> availableClasses = getAvailableClasses(player);
        if (availableClasses.isEmpty()) {
            player.sendMessage(Component.text("No hay clases disponibles", NamedTextColor.RED));
            return;
        }

        Component message = Component.text("Clases disponibles: ", NamedTextColor.GOLD)
                .append(Component.text(String.join(", ", availableClasses), NamedTextColor.GREEN));
        player.sendMessage(message);
    }

    public void showDetailedStats(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);

        player.sendMessage("§6=== ESTADÍSTICAS DETALLADAS ===");
        player.sendMessage(String.format("§eNivel: §a%d §7(%.1f/%.1f XP)",
                pd.getLevel(), pd.getXp(), getXPToNextLevel(player)));
        player.sendMessage(String.format("§ePuntos disponibles: §a%d", pd.getAvailablePoints()));
        player.sendMessage(String.format("§eManá: §b%d/%d", pd.getMana(), pd.getMaxMana()));

        player.sendMessage("§6Atributos:");
        player.sendMessage(String.format("§e- Fuerza: §a%d §7(Bonus: +%.1f daño)",
                pd.getStrength(), pd.getStrength() * 0.5));
        player.sendMessage(String.format("§e- Destreza: §a%d §7(Velocidad: +%.1f%%)",
                pd.getDexterity(), pd.getDexterity() * 1.0));
        player.sendMessage(String.format("§e- Constitución: §a%d §7(Vida: +%.1f corazones)",
                pd.getConstitution(), pd.getConstitution() * 1.0));
        player.sendMessage(String.format("§e- Inteligencia: §a%d §7(Maná: +%.1f regen, +%.1f%% poder mágico)",
                pd.getMetadata("inteligencia", 1),
                pd.getMetadata("inteligencia", 1) * 0.5,
                pd.getMetadata("inteligencia", 1) * 8.0));
        player.sendMessage(String.format("§e- Sabiduría: §a%d §7(Maná: +%d, Reducción cooldown: %.1f%%)",
                pd.getMetadata("sabiduria", 1),
                pd.getMetadata("sabiduria", 1) * 2,
                Math.min(50, pd.getMetadata("sabiduria", 1) * 1.0)));
        player.sendMessage(String.format("§e- Precisión: §a%d §7(Crítico: %.1f%%, Daño a distancia: +%.1f%%)",
                pd.getMetadata("precision", 1),
                Math.min(50, pd.getMetadata("precision", 1) * 1.0),
                pd.getMetadata("precision", 1) * 4.0));
        player.sendMessage(String.format("§e- Agilidad: §a%d §7(Velocidad ataque: +%.1f%%, Esquive: %.1f%%)",
                pd.getMetadata("agilidad", 1),
                pd.getMetadata("agilidad", 1) * 1.0,
                Math.min(25, pd.getMetadata("agilidad", 1) * 0.5)));

        if (playerClasses.containsKey(player.getUniqueId())) {
            player.sendMessage(String.format("§6Clase: §e%s",
                    classManager.getClassDisplayName(pd.getPlayerClass())));
        }
    }

    public void refreshPlayerDisplay(@NotNull Player player) {
        updatePlayerDisplay(player);
        applyAllAttributeEffects(player);
    }

    public void resetPlayerStats(@NotNull Player player) {
        attributeManager.resetPlayerAttributes(player);
        PlayerData pd = getPlayerData(player);
        String currentClass = pd.getPlayerClass();

        resetPlayerData(pd);
        setMaxMana(player, defaultMaxMana);

        if (classManager.isValidClass(currentClass)) {
            setPlayerClass(player, currentClass);
        } else {
            pd.setPlayerClass(defaultClass);
        }

        applyAllAttributeEffects(player);
        updatePlayerDisplay(player);
        savePlayerData(player);

        player.sendMessage("§a¡Tus estadísticas han sido reiniciadas!");
    }

    private void resetPlayerData(PlayerData pd) {
        pd.setLevel(defaultLevel);
        pd.setXp(defaultXP);
        pd.setAvailablePoints(defaultAvailablePoints);
        pd.setStrength(defaultStrength);
        pd.setDexterity(defaultDexterity);
        pd.setConstitution(defaultConstitution);
        pd.setMaxMana(defaultMaxMana);
        pd.setMana(defaultMana);
    }

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

            classManager.reload((FileConfiguration) config);
        }
    }

    public int getLevel(@NotNull Player player) {
        return getPlayerData(player).getLevel();
    }

    public void setLevel(@NotNull Player player, int level) {
        getPlayerData(player).setLevel(level);
        updatePlayerDisplay(player);
    }

    public float getCurrentXP(@NotNull Player player) {
        return getPlayerData(player).getXp();
    }

    public void setXP(@NotNull Player player, float xp) {
        getPlayerData(player).setXp(xp);
        updatePlayerDisplay(player);
    }

    public int getAvailablePoints(@NotNull Player player) {
        return getPlayerData(player).getAvailablePoints();
    }

    public void setAvailablePoints(@NotNull Player player, int points) {
        getPlayerData(player).setAvailablePoints(points);
        updatePlayerDisplay(player);
    }

    public AttributeManager getAttributeManager() {
        return attributeManager;
    }

    public RPGClassManager getClassManager() {
        return classManager;
    }
}