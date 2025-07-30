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
        this.attributes.clear();
        loadAttributes(newConfig);
        originalValues.clear();
        plugin.getLogger().info("AttributeManager recargado con éxito");
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
        AttributeConfig config = getAttributeConfig(attributeName);
        if (config == null) {
            plugin.getLogger().warning("Intento de aplicar efectos para atributo no encontrado: " + attributeName);
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
    }

    // Método para que PlayerProgress pueda limpiar los efectos
    public void resetPlayerAttributes(@NotNull Player player) {
        for (String attribute : attributes.keySet()) {
            clearAttributeEffects(player, attribute);
        }
        cleanUpPlayer(player);
    }
}