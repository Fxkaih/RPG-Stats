package rpg.stats.rpg_stats.managers;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class RPGClassManager {
    private final Map<String, RPGClass> classes = new HashMap<>();
    private String defaultClass;

    public RPGClassManager(@NotNull FileConfiguration config) {
        Objects.requireNonNull(config, "Configuración no puede ser nula");
        loadClasses(config);
        this.defaultClass = config.getString("classes.default-class", "warrior").toLowerCase();
    }

    public void displayClassInfo(@NotNull Player player, @NotNull String classId) {
        RPGClass rpgClass = getRPGClass(classId);
        if (rpgClass == null) {
            player.sendMessage("§cClase no encontrada");
            return;
        }

        player.sendMessage("§6=== " + rpgClass.getDisplayName() + " §6===");
        player.sendMessage("§bBonificaciones:");

        // Usando getAllXPMultipliers()
        Map<String, String> xpMultipliers = rpgClass.getAllXPMultipliers();
        if (!xpMultipliers.isEmpty()) {
            player.sendMessage("§aMultiplicadores de XP:");
            xpMultipliers.forEach((action, multiplier) ->
                    player.sendMessage(" §7- " + action + ": §ex" + multiplier)
            );
        }

        // Usando formatActionName() para los atributos
        player.sendMessage("§aAtributos mejorados:");
        rpgClass.getAttributeBonuses().forEach((attr, bonus) ->
                player.sendMessage(" §7- " + formatActionName(attr) + ": §a+" + bonus)
        );

        // Mostrar habilidades especiales
        player.sendMessage("§aHabilidades especiales:");
        player.sendMessage(" §7- Coste de maná reducido para ciertas habilidades");
    }

    // Método usado por PlayerProgress al asignar clase
    public void sendClassBenefitsMessage(@NotNull Player player, @NotNull String classId) {
        RPGClass rpgClass = getRPGClass(classId);
        if (rpgClass != null) {
            player.sendMessage("§6Has seleccionado la clase: §e" + rpgClass.getDisplayName());

            // Usando getClassXPMultipliers()
            Map<String, String> multipliers = getClassXPMultipliers(classId);
            if (!multipliers.isEmpty()) {
                player.sendMessage("§bVentajas de XP:");
                multipliers.forEach((action, mult) ->
                        player.sendMessage(" §7- " + action + ": §ax" + mult)
                );
            }
        }
    }

    public @NotNull Map<String, String> getClassXPMultipliers(@NotNull String classId) {
        RPGClass rpgClass = getRPGClass(classId);
        return rpgClass != null ? rpgClass.getAllXPMultipliers() : new LinkedHashMap<>();
    }

    // Método interno para formatear nombres (usado en varios lugares)
    private String formatActionName(String action) {
        return Arrays.stream(action.split("[_\\s]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public @Nullable RPGClass getRPGClass(@Nullable String classId) {
        return classId != null ? classes.get(classId.toLowerCase()) : null;
    }

    public boolean hasClass(@NotNull String classId) {
        return classes.containsKey(classId.toLowerCase());
    }

    private void loadClasses(@NotNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("classes");
        if (section == null) return;

        for (String classId : section.getKeys(false)) {
            if (classId.equalsIgnoreCase("default-class")) continue;

            ConfigurationSection classSection = section.getConfigurationSection(classId);
            if (classSection != null) {
                try {
                    RPGClass rpgClass = new RPGClass(classId.toLowerCase(), classSection);
                    classes.put(rpgClass.getId(), rpgClass);
                } catch (IllegalArgumentException e) {
                    // Log error but continue loading other classes
                }
            }
        }

        if (!classes.containsKey(defaultClass)) {
            defaultClass = classes.isEmpty() ? null : classes.keySet().iterator().next();
        }
    }

    public boolean isValidClass(@Nullable String classId) {
        return classId != null && classes.containsKey(classId.toLowerCase());
    }

    public boolean hasDefaultClass() {
        return defaultClass != null && classes.containsKey(defaultClass);
    }

    public @NotNull String getDefaultClass() {
        return Objects.requireNonNullElse(defaultClass, "warrior");
    }

    public void checkLevelUpBonuses(@NotNull Player player, @NotNull PlayerProgress progress, int newLevel) {
        String classId = progress.getCurrentClass(player.getUniqueId());
        if (classId != null) {
            RPGClass rpgClass = classes.get(classId.toLowerCase());
            if (rpgClass != null) {
                rpgClass.applyLevelBonuses(player, progress, newLevel);
            }
        }
    }

    public @NotNull String getClassDisplayName(@Nullable String classId) {
        if (classId == null) return "Sin Clase";
        RPGClass rpgClass = classes.get(classId.toLowerCase());
        return rpgClass != null ? rpgClass.getDisplayName() : classId;
    }

    public @NotNull Map<String, String> getAvailableClasses() {
        return classes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getDisplayName()
                ));
    }

    public void reload(@NotNull FileConfiguration newConfig) {
        Objects.requireNonNull(newConfig, "La configuración no puede ser nula");
        this.classes.clear();
        this.defaultClass = newConfig.getString("classes.default-class", "warrior").toLowerCase();
        loadClasses(newConfig);

        // Validar que la clase por defecto exista
        if (!classes.containsKey(defaultClass) && !classes.isEmpty()) {
            defaultClass = classes.keySet().iterator().next();
        }
    }

    public static class RPGClass {
        private final String id;
        private final String displayName;
        private final Map<String, Integer> baseStats;
        private final Map<String, Float> xpMultipliers;
        private final Map<Integer, Map<String, Integer>> levelBonuses;
        private final Map<String, Integer> attributeBonuses;

        public RPGClass(@NotNull String id, @NotNull ConfigurationSection config) {
            this.id = Objects.requireNonNull(id, "ID no puede ser nulo").toLowerCase();
            this.displayName = config.getString("display-name", id);

            this.baseStats = loadStats(config.getConfigurationSection("base-stats"));
            this.xpMultipliers = loadXPMultipliers(config.getConfigurationSection("xp-multipliers"));
            this.levelBonuses = loadLevelBonuses(config.getConfigurationSection("level-bonuses"));
            this.attributeBonuses = loadStats(config.getConfigurationSection("attribute-bonuses"));
        }

        private @NotNull Map<String, Integer> loadStats(@Nullable ConfigurationSection section) {
            Map<String, Integer> stats = new HashMap<>();
            if (section != null) {
                for (String stat : section.getKeys(false)) {
                    stats.put(stat.toLowerCase(), Math.max(0, section.getInt(stat, 0)));
                }
            }
            return stats;
        }

        public float getXPMultiplier(@NotNull String actionType) {
            Objects.requireNonNull(actionType, "El tipo de acción no puede ser nulo");
            return xpMultipliers.getOrDefault(actionType.toLowerCase(), 1.0f);
        }

        // Método para obtener todos los multiplicadores (si es necesario)
        public @NotNull Map<String, String> getAllXPMultipliers() {
            Map<String, String> formatted = new LinkedHashMap<>();

            // Formatear cada entrada (ej. "mining" -> "Mining: 1.5x")
            this.xpMultipliers.forEach((action, multiplier) -> {
                String formattedAction = formatActionName(action);
                formatted.put(formattedAction, String.format("%.1fx", multiplier));
            });

            // Ordenar de mayor a menor multiplicador
            return formatted.entrySet().stream()
                    .sorted(Map.Entry.<String, String>comparingByValue().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
        }

        private String formatActionName(String action) {
            return action.substring(0, 1).toUpperCase() + action.substring(1).toLowerCase()
                    .replace("_", " ");
        }

        public float getManaCostMultiplier(String abilityType) {
            return switch (this.id) {
                case "wizard" -> "magic".equals(abilityType) ? 0.7f : 1.0f;
                case "cleric" -> "divine".equals(abilityType) || abilityType.contains("heal") ? 0.8f : 1.0f;
                case "druid" -> "nature".equals(abilityType) || abilityType.contains("heal") ? 0.8f : 1.0f;
                default -> 1.0f;
            };
        }

        private @NotNull Map<String, Float> loadXPMultipliers(@Nullable ConfigurationSection section) {
            Map<String, Float> multipliers = new HashMap<>();
            if (section != null) {
                for (String action : section.getKeys(false)) {
                    multipliers.put(action.toLowerCase(), (float) Math.max(0.1, section.getDouble(action, 1.0)));
                }
            }
            return multipliers;
        }

        private @NotNull Map<Integer, Map<String, Integer>> loadLevelBonuses(@Nullable ConfigurationSection section) {
            Map<Integer, Map<String, Integer>> bonuses = new TreeMap<>();
            if (section != null) {
                for (String levelStr : section.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelStr);
                        if (level < 1) continue;

                        ConfigurationSection levelSection = section.getConfigurationSection(levelStr);
                        if (levelSection != null) {
                            Map<String, Integer> levelBonuses = new HashMap<>();
                            for (String stat : levelSection.getKeys(false)) {
                                levelBonuses.put(stat.toLowerCase(), Math.max(0, levelSection.getInt(stat, 0)));
                            }
                            bonuses.put(level, levelBonuses);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            return bonuses;
        }

        public void applyEffects(@NotNull Player player,
                                 @NotNull PlayerProgress progress) {
            attributeBonuses.forEach((attribute, bonus) -> {
                int current = progress.getAttribute(player, attribute);
                progress.setAttribute(player, attribute, current + bonus, false);
            });

            baseStats.forEach((stat, value) -> {
                switch (stat.toLowerCase()) {
                    case "health":
                        Attribute maxHealth = Attribute.MAX_HEALTH;
                        if (player.getAttribute(maxHealth) != null) {
                            Objects.requireNonNull(player.getAttribute(maxHealth)).setBaseValue(Objects.requireNonNull(player.getAttribute(maxHealth)).getBaseValue() + value);
                        }
                        break;
                    case "mana":
                        int currentMaxMana = progress.getMaxMana(player);
                        progress.setMaxMana(player, currentMaxMana + value);
                        progress.setMana(player, currentMaxMana + value);
                        break;
                    case "attack":
                        Attribute attackDmg = Attribute.ATTACK_DAMAGE;
                        if (player.getAttribute(attackDmg) != null) {
                            Objects.requireNonNull(player.getAttribute(attackDmg)).setBaseValue(Objects.requireNonNull(player.getAttribute(attackDmg)).getBaseValue() + (value * 0.5));
                        }
                        break;
                    case "defense":
                        Attribute armor = Attribute.ARMOR;
                        if (player.getAttribute(armor) != null) {
                            Objects.requireNonNull(player.getAttribute(armor)).setBaseValue(Objects.requireNonNull(player.getAttribute(armor)).getBaseValue() + value);
                        }
                        break;
                    case "speed":
                        float currentSpeed = player.getWalkSpeed();
                        player.setWalkSpeed((float) Math.min(currentSpeed + (value * 0.01), 1.0f));
                        break;
                }
            });
        }

        public void applyLevelBonuses(@NotNull Player player, @NotNull PlayerProgress progress, int newLevel) {
            levelBonuses.forEach((requiredLevel, bonuses) -> {
                if (newLevel >= requiredLevel) {
                    bonuses.forEach((stat, bonus) -> {
                        switch (stat.toLowerCase()) {
                            case "health":
                                Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH))
                                        .setBaseValue(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue() + bonus);
                                break;
                            case "mana":
                                progress.setMana(player, progress.getMaxMana(player) + bonus);
                                break;
                            default:
                                progress.setAttribute(player, stat, progress.getAttribute(player, stat) + bonus, false);
                        }
                    });
                }
            });
        }

        public @NotNull String getDisplayName() {
            return displayName;
        }

        public @NotNull String getId() {
            return id;
        }

        public Map<String, Integer> getAttributeBonuses() {
            return Collections.unmodifiableMap(attributeBonuses);
        }

    }
}