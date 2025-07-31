package rpg.stats.rpg_stats.managers;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AttributeManager {
    private final Map<String, AttributeConfig> attributes = new HashMap<>();
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Double>> originalValues = new HashMap<>();

    public AttributeManager(FileConfiguration config, JavaPlugin plugin) {
        this.plugin = plugin;
        loadAttributes(config);
    }

    public void reload(@NotNull FileConfiguration newConfig) {
        Objects.requireNonNull(newConfig, "La configuración no puede ser nula");
        this.attributes.clear();
        loadAttributes(newConfig);
        plugin.getLogger().info("Atributos recargados correctamente");
    }

    private void loadAttributes(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        if (section == null) {
            plugin.getLogger().warning("No se encontró la sección 'attributes' en config.yml");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection attrSection = section.getConfigurationSection(key);
            if (attrSection != null) {
                attributes.put(key.toLowerCase(), new AttributeConfig(attrSection));
            }
        }
    }

    public String getAttributeBonusInfo(String attribute, int value) {
        AttributeConfig config = getAttributeConfig(attribute.toLowerCase());
        if (config == null) return "";

        // Obtener fórmulas desde la configuración
        Map<String, String> bonusFormulas = config.getBonusFormulas();

        // Si hay fórmulas definidas en config.yml, usarlas
        if (!bonusFormulas.isEmpty()) {
            return formatDynamicBonuses(bonusFormulas, value);
        }

        // Sistema de respaldo para atributos conocidos
        return getDefaultBonusInfo(attribute.toLowerCase(), value);

    }
    private String formatDynamicBonuses(Map<String, String> formulas, int value) {
        StringBuilder bonuses = new StringBuilder();
        formulas.forEach((description, formula) -> {
            try {
                double result = evaluateFormula(formula, value);
                bonuses.append(String.format("%s: %.1f\n", description, result));
            } catch (Exception e) {
                plugin.getLogger().warning("Error en fórmula '" + description +
                        "': " + formula + " - " + e.getMessage());
            }
        });
        return bonuses.toString().trim();
    }

    private double evaluateFormula(String formula, int value) throws IllegalArgumentException {
        // Reemplazar el valor y quitar espacios
        String expr = formula.replace("value", String.valueOf(value)).replace(" ", "");

        // Implementación básica de evaluador (puedes mejorarla)
        if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        }
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        }
        if (expr.contains("-")) {
            String[] parts = expr.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        }
        if (expr.contains("/")) {
            String[] parts = expr.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }

        // Si no hay operador, asumir que es un valor directo
        return Double.parseDouble(expr);
    }

    private String getDefaultBonusInfo(String attribute, int value) {
        // Mantener el sistema actual como respaldo
        return switch (attribute) {
            case "inteligencia" -> String.format("Maná: +%.1f regen, +%.1f%% poder mágico",
                    value * 0.5, value * 8.0);
            case "sabiduria" -> String.format("Maná: +%d, Reducción cooldown: %.1f%%",
                    value * 2, Math.min(50, value * 1.0));
            case "precision" -> String.format("Crítico: %.1f%%, Daño a distancia: +%.1f%%",
                    Math.min(50, value * 1.0), value * 4.0);
            case "agilidad" -> String.format("Velocidad ataque: +%.1f%%, Esquive: %.1f%%",
                    value * 1.0, Math.min(25, value * 0.5));
            default -> "";
        };
    }

    public boolean isValidAttribute(String name) {
        return attributes.containsKey(name.toLowerCase());
    }

    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }

    public int getMaxValue(String attribute) {
        AttributeConfig config = getAttributeConfig(attribute);
        return config != null ? config.getMaxValue() : 50;
    }

    public String getAttributeDisplayName(String attribute) {
        AttributeConfig config = getAttributeConfig(attribute);
        return config != null ? config.getDisplayName() : attribute;
    }

    public @Nullable AttributeConfig getAttributeConfig(String name) {
        return attributes.get(name.toLowerCase());
    }

    public void applyAttributeEffects(@NotNull Player player, @NotNull String attributeName, int value) {
                    String normalizedName = attributeName.toLowerCase();
                    /*.replace("strength", "fuerza")
                    .replace("dexterity", "destreza")
                    .replace("constitution", "constitucion")
                    .replace("intelligence", "inteligencia")
                    .replace("wisdom", "sabiduria")
                    .replace("precision", "precision")
                    .replace("agility", "agilidad"); */

            AttributeConfig config = getAttributeConfig(normalizedName);
            if (config == null) {
                plugin.getLogger().warning("Atributo '" + normalizedName + "' no encontrado. ¿Está definido en config.yml?");
                return;
            }

        saveOriginalValues(player);
        clearAttributeEffects(player, attributeName);

        for (AttributeEffect effect : config.getEffects()) {
            effect.apply(player, value);
        }
    }

    private void saveOriginalValues(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (!originalValues.containsKey(uuid)) {
            Map<String, Double> values = new HashMap<>();

            // Guardar valores originales con manejo de null
            if (player.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                values.put("damage", Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_DAMAGE)).getBaseValue());
            }
            values.put("speed", (double) player.getWalkSpeed());
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                values.put("health", Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
            }

            originalValues.put(uuid, values);
        }
    }

    private void clearAttributeEffects(@NotNull Player player, @NotNull String attributeName) {
        AttributeConfig config = getAttributeConfig(attributeName);
        if (config == null) return;

        UUID uuid = player.getUniqueId();
        Map<String, Double> playerValues = originalValues.getOrDefault(uuid, new HashMap<>());

        // Restaurar valores con manejo seguro de null
        for (AttributeEffect effect : config.getEffects()) {
            switch (effect.getEffectType().toUpperCase()) {
                case "DAMAGE":
                    if (player.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                        Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_DAMAGE))
                                .setBaseValue(playerValues.getOrDefault("damage", 1.0));
                    }
                    break;
                case "MOVEMENT_SPEED":
                    player.setWalkSpeed(playerValues.getOrDefault("speed", 0.2).floatValue());
                    break;
                case "MAX_HEALTH":
                    if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                        double health = playerValues.getOrDefault("health", 20.0);
                        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health);
                        player.setHealthScale(health);
                    }
                    break;
                case "KNOCKBACK_RESISTANCE":
                    if (player.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
                        Objects.requireNonNull(player.getAttribute(Attribute.KNOCKBACK_RESISTANCE))
                                .setBaseValue(playerValues.getOrDefault("knockback", 0.0));
                    }
                    break;
                case "ATTACK_SPEED":
                    if (player.getAttribute(Attribute.ATTACK_SPEED) != null) {
                        Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED))
                                .setBaseValue(playerValues.getOrDefault("attack_speed", 4.0));
                    }
                    break;
                // Nuevos tipos de efectos para los nuevos atributos
                case "MANA_REGEN":
                    // Este efecto se maneja en PlayerProgress
                    break;
                case "SPELL_POWER":
                    // Este efecto se maneja en AbilityManager
                    break;
                case "MAX_MANA":
                    // Este efecto se maneja en PlayerProgress
                    break;
                case "COOLDOWN_REDUCTION":
                    // Este efecto se maneja en AbilityManager
                    break;
                case "CRITICAL_CHANCE":
                    // Este efecto se maneja en RPGActionsListener
                    break;
                case "RANGED_DAMAGE":
                    // Este efecto se maneja en RPGActionsListener
                    break;
                case "DODGE_CHANCE":
                    // Este efecto se maneja en RPGActionsListener
                    break;
            }
        }
    }

    public void cleanUpPlayer(@NotNull Player player) {
        originalValues.remove(player.getUniqueId());

        // Limpiar metadatos de atributos
        for (String metaKey : Arrays.asList(
                "mana_regen_bonus",
                "spell_power",
                "max_mana_bonus",
                "cooldown_reduction",
                "critical_chance",
                "ranged_damage",
                "dodge_chance"
        )) {
            player.removeMetadata(metaKey, plugin);
        }
    }

    /**
     * Reinicia todos los efectos de atributos para un jugador
     * @param player Jugador a resetear
     */
    public void resetPlayerAttributes(@NotNull Player player) {
        // Limpiar efectos de todos los atributos conocidos
        for (String attribute : attributes.keySet()) {
            clearAttributeEffects(player, attribute);
        }
        cleanUpPlayer(player);

        // Restaurar valores base
        try {
            Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(1.0);
            Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED)).setBaseValue(4.0);
            player.setWalkSpeed(0.2f);
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            }
        } catch (NullPointerException e) {
            plugin.getLogger().warning("Error al resetear atributos para " + player.getName());
        }
    }
}