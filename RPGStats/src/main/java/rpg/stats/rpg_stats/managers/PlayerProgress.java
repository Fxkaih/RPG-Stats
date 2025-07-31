package rpg.stats.rpg_stats.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
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
    private final Set<UUID> pendingSaves = ConcurrentHashMap.newKeySet();

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

        ensureDataFolderExists();
        if (!isDataFolderWritable()) {
            plugin.getLogger().severe("NO HAY PERMISOS DE ESCRITURA EN: " + dataFolderPath);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

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
        if (pendingSaves.contains(player.getUniqueId())) {
            return;
        }
        pendingSaves.add(player.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID uuid = player.getUniqueId();
                File file = new File(dataFolderPath + uuid + ".yml");

                if (!file.getParentFile().exists() && !file.getParentFile().
                        mkdirs()) {
                    throw new IOException("No se pudo crear el directorio de datos");
                }

                // Crear directorio si no existe
                //file.getParentFile().mkdirs();

                YamlConfiguration data = new YamlConfiguration();
                PlayerData pd = getPlayerData(player);

                // Guardar datos básicos
                data.set("player-data.level", pd.getLevel());
                data.set("player-data.xp", pd.getXp());
                data.set("player-data.available-points", pd.getAvailablePoints());

                // Guardar atributos
                data.set("player-data.attributes.strength", pd.getStrength());
                data.set("player-data.attributes.dexterity", pd.getDexterity());
                data.set("player-data.attributes.constitution", pd.getConstitution());

                // Guardar maná
                data.set("player-data.mana", pd.getMana());
                data.set("player-data.max-mana", pd.getMaxMana());

                // Guardar clase
                data.set("player-data.class", pd.getPlayerClass());

                // Guardar metadata
                if (!pd.getAllMetadata().isEmpty()) {
                    data.set("player-data.metadata", new HashMap<>(pd.getAllMetadata()));
                }

                // Guardar archivo
                data.save(file);
                plugin.getLogger().info("Datos de " + player.getName() + " guardados correctamente");
            } catch (IOException e) {
                plugin.getLogger().severe("Error crítico al guardar datos: " + e.getMessage());
            } finally {
                pendingSaves.remove(player.getUniqueId());
            }
        });

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

            // Verificar estructura básica
            if (!data.isConfigurationSection("player-data")) {
                throw new IllegalStateException("Estructura de datos inválida");
            }

            // Cargar datos básicos
            int level = data.getInt("player-data.level", defaultLevel);
            float xp = (float) data.getDouble("player-data.xp", defaultXP);
            int availablePoints = data.getInt("player-data.available-points", defaultAvailablePoints);

            // Cargar atributos
            int strength = data.getInt("player-data.attributes.strength", defaultStrength);
            int dexterity = data.getInt("player-data.attributes.dexterity", defaultDexterity);
            int constitution = data.getInt("player-data.attributes.constitution", defaultConstitution);

            // Cargar maná
            int mana = data.getInt("player-data.mana", defaultMana);
            int maxMana = data.getInt("player-data.max-mana", defaultMaxMana);

            // Cargar clase
            String playerClass = data.getString("player-data.class", defaultClass);

            // Crear PlayerData
            PlayerData pd = new PlayerData(level, xp, availablePoints, strength, dexterity,
                    constitution, mana, maxMana, playerClass);

            // Cargar metadata
            if (data.isConfigurationSection("player-data.metadata")) {
                Objects.requireNonNull(data.getConfigurationSection("player-data.metadata")).getValues(false)
                        .forEach((key, value) -> pd.setMetadata(key, (int) value));
            }

            playerDataMap.put(playerId, pd);
            plugin.getLogger().info("Datos de " + player.getName() + " cargados correctamente");

        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar datos de " + player.getName() + ": " + e.getMessage());
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

        // Añadir XP directamente sin recursión
        pd.setXp(pd.getXp() + finalXP);
        updatePlayerDisplay(player);

        player.sendActionBar(Component.text("+" + finalXP + "XP (" + actionType + ")").color(NamedTextColor.GREEN));

        // Verificar subida de nivel
        checkLevelUp(player);
    }

    private void checkLevelUp(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);
        float xpNeeded = calculateXPForNextLevel(pd.getLevel());

        while (pd.getXp() >= xpNeeded && pd.getLevel() < config.getInt("levels.max-level", 100)) {
            levelUp(player);
            xpNeeded = calculateXPForNextLevel(pd.getLevel()); // Recalcular para el nuevo nivel
        }
    }

    private void levelUp(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);
        int oldLevel = pd.getLevel();
        int newLevel = oldLevel + 1;

        int pointsGained = calculatePointsGained(newLevel);
        int manaIncrease = config.getInt("levels.mana-per-level", 5);

        // Aplicar cambios
        pd.setLevel(newLevel);
        pd.setAvailablePoints(pd.getAvailablePoints() + pointsGained);
        pd.setMaxMana(pd.getMaxMana() + manaIncrease);
        pd.setMana(pd.getMaxMana());

        // Solo restar XP si no fue un setLevel por comando
        if (pd.getXp() >= calculateXPForNextLevel(oldLevel)) {
            pd.setXp(pd.getXp() - calculateXPForNextLevel(oldLevel));
        } else {
            pd.setXp(0);
        }

        // Llamar evento
        Bukkit.getPluginManager().callEvent(
                new PlayerLevelUpEvent(player, oldLevel, newLevel,
                        pointsGained, manaIncrease,
                        pd.getXp(), calculateXPForNextLevel(newLevel))
        );

        // Guardar cambios
        savePlayerData(player);

        // Llamar a onLevelUp con la nueva lógica
        onLevelUp(player, pd.getLevel(), player.getUniqueId(), pointsGained, manaIncrease);
    }

    private void onLevelUp(@NotNull Player player, int newLevel, @NotNull UUID playerId,
                           int pointsGained, int manaIncrease) {
        // Efectos visuales y de sonido
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        player.showTitle(Title.title(
                Component.text("§6¡Nivel " + newLevel + "!"),
                Component.text("§eFelicidades"),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                )));

        // Mensajes especiales cada 5 niveles
        if (newLevel % 5 == 0) {
            player.sendMessage(Component.text("¡Has alcanzado el nivel " + newLevel + "! ¡Felicidades", NamedTextColor.GOLD));
        }

        // Bonificaciones de clase
        RPGClassManager classManager = getClassManager();
        String playerClass = getCurrentClass(playerId);
        if (playerClass != null) {
            player.sendMessage(Component.text("§bBonificación de nivel para " + playerClass, NamedTextColor.AQUA));
            classManager.checkLevelUpBonuses(player, this, newLevel);
        }

        player.sendMessage(Component.text(String.format(
                "§a¡Subiste al nivel §e%d§a! §7(Puntos: §6+%d§7, Maná: §b+%d§7)",
                newLevel, pointsGained, manaIncrease)));
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
        return calculateXPForNextLevel(getPlayerData(player).getLevel());
    }


    private float calculateXPForNextLevel(int level) {
        int base = config.getInt("levels.xp-base", 100);
        int increment = config.getInt("levels.xp-increment", 50);
        float scaling = (float) config.getDouble("levels.xp-scaling", 1.1);

        // Fórmula exponencial para requerimientos de XP
        return base + (increment * level) * (float) Math.pow(scaling, level);

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

        // Usar spendPoints de PlayerData
        if (!pd.spendPoints(1)) {
            player.sendMessage("§cNo tienes puntos de atributo disponibles");
            return;
        }

        int newValue = getAttribute(player, attribute) + 1;
        setAttribute(player, attribute, newValue, false);

        player.sendMessage(String.format("§a¡%s aumentado a §e%d§a!",
                attribute.substring(0, 1).toUpperCase() + attribute.substring(1),
                newValue));
    }

    public void applyAllAttributeEffects(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);
        setAttribute(player, "fuerza", pd.getStrength(), false);
        setAttribute(player, "destreza", pd.getDexterity(), false);
        setAttribute(player, "constitucion", pd.getConstitution(), false);
        pd.getAllMetadata().forEach((attr, value) -> attributeManager.applyAttributeEffects(player, attr, value));
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

    // Método showDetailedStats integrado con PlayerData
    public void showDetailedStats(@NotNull Player player) {
        PlayerData pd = getPlayerData(player);

        // Mostrar información básica
        player.sendMessage("§6=== ESTADÍSTICAS DETALLADAS ===");
        player.sendMessage(String.format("§eNivel: §a%d §7(%.1f/%.1f XP)",
                pd.getLevel(), pd.getXp(), calculateXPForNextLevel(pd.getLevel())));
        player.sendMessage(String.format("§ePuntos disponibles: §a%d", pd.getAvailablePoints()));
        player.sendMessage(String.format("§eManá: §b%d/%d", pd.getMana(), pd.getMaxMana()));

        // Mostrar atributos principales
        player.sendMessage("§6Atributos principales:");
        player.sendMessage(String.format("§e- Fuerza: §a%d §7(+%.1f daño)",
                pd.getStrength(), pd.getStrength() * 0.5));
        player.sendMessage(String.format("§e- Destreza: §a%d §7(+%.1f%% velocidad)",
                pd.getDexterity(), pd.getDexterity() * 1.0));
        player.sendMessage(String.format("§e- Constitución: §a%d §7(+%.1f corazones)",
                pd.getConstitution(), pd.getConstitution() * 1.0));

        // Mostrar metadata usando getAllMetadata()
        player.sendMessage("§6Atributos secundarios:");
        pd.getAllMetadata().forEach((key, value) -> {
            String displayName = attributeManager.getAttributeDisplayName(key);
            String bonusInfo = attributeManager.getAttributeBonusInfo(key, value);
            player.sendMessage(String.format("§e- %s: §a%d §7(%s)",
                    displayName, value, bonusInfo));
        });

        // Mostrar clase si tiene
        if (!pd.getPlayerClass().equals("none")) {
            player.sendMessage(String.format("§6Clase: §e%s",
                    classManager.getClassDisplayName(pd.getPlayerClass())));
        }
    }

    public void refreshPlayerDisplay(@NotNull Player player) {
        updatePlayerDisplay(player);
        applyAllAttributeEffects(player);
    }

    public void resetPlayerStats(@NotNull Player player) {
        // 1. Resetear atributos en memoria
        attributeManager.resetPlayerAttributes(player);
        PlayerData pd = getPlayerData(player);
        String currentClass = pd.getPlayerClass();
        pd.resetStats();

        // 2. Eliminar archivo persistente
        File playerFile = new File(dataFolderPath, player.getUniqueId() + ".yml");
        if (playerFile.exists() && !safeDelete(playerFile)) {
            plugin.getLogger().severe("Error crítico al resetear datos de " + player.getName());
            player.sendMessage("§c¡Error grave! Contacta con un administrador");
            return;
        }

        // 3. Restaurar clase si era válida
        if (classManager.isValidClass(currentClass)) {
            setPlayerClass(player, currentClass);
        }

        // 4. Aplicar cambios
        applyAllAttributeEffects(player);
        updatePlayerDisplay(player);
        savePlayerData(player);
        player.sendMessage("§a¡Tus estadísticas han sido reiniciadas!");
    }


    private void ensureDataFolderExists() {
        File folder = new File(dataFolderPath);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                plugin.getLogger().severe("No se pudo crear el directorio de datos: " + dataFolderPath);
                plugin.getLogger().severe("Verifica los permisos de escritura en la carpeta del plugin.");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            } else {
                plugin.getLogger().info("Directorio de datos creado: " + dataFolderPath);
            }
        }

    }
    private boolean isDataFolderWritable() {
        File testFile = new File(dataFolderPath, ".perms_test");
        try {
            if (testFile.createNewFile()) {
                boolean deleted = testFile.delete();
                if (!deleted) {
                    plugin.getLogger().warning("No se pudo borrar archivo de prueba: " + testFile.getPath());
                }
                return deleted;
            }
            return false;
        } catch (IOException e) {
            plugin.getLogger().warning("Error al verificar permisos: " + e.getMessage());
            return false;
        }
    }

    private boolean safeDelete(@NotNull File file) {
        if (!file.exists()) {
            return true;
        }

        int attempts = 0;
        while (attempts < 3) {
            if (file.delete()) {
                return true;
            }
            attempts++;
            try {
                Thread.sleep(1000); // Espera 1 segundo entre intentos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        plugin.getLogger().warning("No se pudo eliminar el archivo después de 3 intentos: " + file.getPath());
        return false;
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
        PlayerData pd = getPlayerData(player);
        int oldLevel = pd.getLevel();

        if (level > oldLevel) {
            // Calcular puntos ganados por los niveles subidos
            int pointsGained = 0;
            for (int i = oldLevel + 1; i <= level; i++) {
                pointsGained += calculatePointsGained(i);
            }

            pd.setAvailablePoints(pd.getAvailablePoints() + pointsGained);
        }

        pd.setLevel(level);
        applyAllAttributeEffects(player); // ¡Importante!
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
    public float getMiningXPBonus(Player player) {
        float bonus = 1.0f;
        PlayerData pd = getPlayerData(player);

        // Bonus por atributos
        bonus += pd.getStrength() * 0.01f; // +1% por punto de fuerza
        bonus += pd.getDexterity() * 0.005f; // +0.5% por punto de destreza

        // Bonus por clase
        RPGClassManager.RPGClass playerClass = classManager.getRPGClass(pd.getPlayerClass());
        if (playerClass != null) {
            bonus *= playerClass.getXPMultiplier("mining");
        }

        return Math.min(bonus, 1.5f); // Máximo 50% de bonus
    }

    public float getCombatXPBonus(Player player) {
        float bonus = 1.0f;
        PlayerData pd = getPlayerData(player);

        // Bonus por atributos
        bonus += pd.getStrength() * 0.015f; // +1.5% por punto de fuerza
        bonus += pd.getDexterity() * 0.02f; // +2% por punto de destreza

        // Bonus por clase
        RPGClassManager.RPGClass playerClass = classManager.getRPGClass(pd.getPlayerClass());
        if (playerClass != null) {
            bonus *= playerClass.getXPMultiplier("combat");
        }

        return Math.min(bonus, 2.0f); // Máximo 100% de bonus
    }
}