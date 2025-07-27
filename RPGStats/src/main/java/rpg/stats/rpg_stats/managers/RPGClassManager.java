package rpg.stats.rpg_stats.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Attribute;

public class RPGClassManager {
    private final Map<String, RPGClass> classes = new HashMap<>();
    private String defaultClass;

    public RPGClassManager(@NotNull FileConfiguration config) {
        loadClasses(config);
        this.defaultClass = config.getString("classes.default-class", "warrior");
    }

    private void loadClasses(@NotNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("classes");
        if (section == null) return;

        for (String classId : section.getKeys(false)) {
            if (classId.equals("default-class")) continue;
            ConfigurationSection classSection = section.getConfigurationSection(classId);
            if (classSection != null) {
                classes.put(classId.toLowerCase(), new RPGClass(classId, classSection));
            }
        }
    }

    public boolean isValidClass(@Nullable String classId) {
        return classId != null && classes.containsKey(classId.toLowerCase());
    }

    public boolean hasDefaultClass() {
        return defaultClass != null && classes.containsKey(defaultClass.toLowerCase());
    }

    public @NotNull String getDefaultClass() {
        return Objects.requireNonNullElse(defaultClass, "warrior");
    }

    public void applyClassEffects(@NotNull Player player, @NotNull PlayerProgress progress, @NotNull String classId) {
        RPGClass rpgClass = classes.get(classId.toLowerCase());
        if (rpgClass != null) {
            rpgClass.applyEffects(player, progress);
        }
    }

    public void checkLevelUpBonuses(@NotNull Player player, @NotNull PlayerProgress progress, int newLevel) {
        String classId = progress.getPlayerClass(player);
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
        Map<String, String> available = new HashMap<>();
        classes.forEach((id, rpgClass) -> available.put(id, rpgClass.getDisplayName()));
        return available;
    }

    public void reload(@NotNull FileConfiguration newConfig) {
        this.classes.clear();
        this.defaultClass = newConfig.getString("classes.default-class", "warrior");
        loadClasses(newConfig);
    }

    public static class RPGClass {
        private final String id;
        private final String displayName;
        private final Map<String, Integer> baseStats;
        private final Map<String, Float> xpMultipliers;
        private final Map<Integer, Map<String, Integer>> levelBonuses;
        private final Map<String, Integer> attributeBonuses;

        public RPGClass(@NotNull String id, @NotNull ConfigurationSection config) {
            this.id = id;
            this.displayName = config.getString("display-name", id);
            this.baseStats = loadStats(config.getConfigurationSection("base-stats"));
            this.xpMultipliers = loadXPMultipliers(config.getConfigurationSection("xp-multipliers"));
            this.levelBonuses = loadLevelBonuses(config.getConfigurationSection("level-bonuses"));
            this.attributeBonuses = loadStats(config.getConfigurationSection("attribute-bonuses"));
        }

        private @NotNull Map<String, Integer> loadStats(@Nullable ConfigurationSection section) {
            Map<String, Integer> stats = new HashMap<>();
            if (section != null) {
                section.getKeys(false).forEach(stat ->
                        stats.put(stat, section.getInt(stat, 0))
                );
            }
            return stats;
        }

        private @NotNull Map<String, Float> loadXPMultipliers(@Nullable ConfigurationSection section) {
            Map<String, Float> multipliers = new HashMap<>();
            if (section != null) {
                section.getKeys(false).forEach(action ->
                        multipliers.put(action, (float) section.getDouble(action, 1.0))
                );
            }
            return multipliers;
        }

        private @NotNull Map<Integer, Map<String, Integer>> loadLevelBonuses(@Nullable ConfigurationSection section) {
            Map<Integer, Map<String, Integer>> bonuses = new HashMap<>();
            if (section != null) {
                section.getKeys(false).forEach(levelStr -> {
                    try {
                        int level = Integer.parseInt(levelStr);
                        ConfigurationSection levelSection = section.getConfigurationSection(levelStr);
                        if (levelSection != null) {
                            Map<String, Integer> levelBonuses = new HashMap<>();
                            levelSection.getKeys(false).forEach(stat ->
                                    levelBonuses.put(stat, levelSection.getInt(stat, 0))
                            );
                            bonuses.put(level, levelBonuses);
                        }
                    } catch (NumberFormatException ignored) {}
                });
            }
            return bonuses;
        }

        public void applyEffects(@NotNull Player player, @NotNull PlayerProgress progress) {
            // Aplicar bonificaciones de atributos
            attributeBonuses.forEach((attribute, bonus) -> {
                int current = progress.getAttribute(player, attribute);
                progress.setAttribute(player, attribute, current + bonus);
            });

            // Aplicar stats base
            baseStats.forEach((stat, value) -> {
                switch (stat.toLowerCase()) {
                    case "health" ->
                            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
                                    .setBaseValue(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() + value);
                    case "mana" ->
                            progress.getMaxMana(player, progress.getMaxMana(player) + value);
                }
            });
        }

        public void applyLevelBonuses(@NotNull Player player, @NotNull PlayerProgress progress, int newLevel) {
            levelBonuses.forEach((requiredLevel, bonuses) -> {
                if (newLevel >= requiredLevel) {
                    bonuses.forEach((stat, bonus) -> {
                        switch (stat.toLowerCase()) {
                            case "health" ->
                                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
                                            .setBaseValue(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() + bonus);
                            case "mana" ->
                                    progress.setMaxMana(player, progress.getMaxMana(player) + bonus);
                            default ->
                                    progress.setAttribute(player, stat, progress.getAttribute(player, stat) + bonus);
                        }
                    });
                }
            });
        }

        public float getXPMultiplier(@NotNull String actionType) {
            return xpMultipliers.getOrDefault(actionType.toLowerCase(), 1.0f);
        }

        public @NotNull String getDisplayName() {
            return displayName;
        }

        public @NotNull String getId() {
            return id;
        }
    }
}