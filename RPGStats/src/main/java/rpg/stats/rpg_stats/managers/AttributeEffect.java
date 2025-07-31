package rpg.stats.rpg_stats.managers;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AttributeEffect {
    private final String effectType;
    private final double multiplier;
    private final Double maxBonus;
    private final  Plugin plugin;

    public AttributeEffect(ConfigurationSection config) {
        this.effectType = config.getString("type", "").toUpperCase();
        this.multiplier = config.getDouble("multiplier", 1.0);
        this.maxBonus = config.isDouble("max-bonus") ? config.getDouble("max-bonus") : null;
        this.plugin =  Bukkit.getPluginManager().getPlugin("RPG_Stats");
    }

    public String getEffectType() {
        return effectType;
    }

    public void apply(@NotNull Player player, int attributeValue) {
        switch (effectType) {
            case "DAMAGE":
                applyDamageBonus(player, attributeValue);
                break;
            case "MOVEMENT_SPEED":
                applySpeedBonus(player, attributeValue);
                break;
            case "MAX_HEALTH":
                applyHealthBonus(player, attributeValue);
                break;
            case "KNOCKBACK_RESISTANCE":
                applyKnockbackResistance(player, attributeValue);
                break;
            case "ATTACK_SPEED":
                applyAttackSpeed(player, attributeValue);
                break;
            case "MANA_REGEN":
                storePlayerAttributeEffect(player, "mana_regen_bonus", attributeValue * multiplier);
                break;
            case "SPELL_POWER":
                storePlayerAttributeEffect(player, "spell_power", 1.0 + (attributeValue * multiplier));
                break;
            case "MAX_MANA":
                storePlayerAttributeEffect(player, "max_mana_bonus", attributeValue * multiplier);
                break;
            case "COOLDOWN_REDUCTION":
                double cdReduction = applyMaxBonus(attributeValue * multiplier);
                storePlayerAttributeEffect(player, "cooldown_reduction", cdReduction);
                break;
            case "CRITICAL_CHANCE":
                double critChance = applyMaxBonus(attributeValue * multiplier);
                storePlayerAttributeEffect(player, "critical_chance", critChance);
                break;
            case "RANGED_DAMAGE":
                storePlayerAttributeEffect(player, "ranged_damage", 1.0 + (attributeValue * multiplier));
                break;
            case "DODGE_CHANCE":
                double dodgeChance = applyMaxBonus(attributeValue * multiplier);
                storePlayerAttributeEffect(player, "dodge_chance", dodgeChance);
                break;
            default:
                player.sendMessage("Efecto de atributo desconocido: " + effectType);
        }
    }

    private double applyMaxBonus(double value) {
        return maxBonus != null ? Math.min(value, maxBonus) : value;
    }

    private void storePlayerAttributeEffect(@NotNull Player player, String effectKey, double value) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("RPG_Stats");
        if (plugin == null) {
            throw new IllegalStateException("Plugin RPG_Stats no encontrado");
        }
        player.setMetadata(effectKey, new FixedMetadataValue(plugin, value));
    }

    private void applyDamageBonus(@NotNull Player player, int attributeValue) {
        try {
            Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_DAMAGE))
                    .setBaseValue(1.0 + (attributeValue * multiplier));
        } catch (NullPointerException e) {
            logAttributeError(player, "ATTACK_DAMAGE");
        }
    }

    private void applySpeedBonus(@NotNull Player player, int attributeValue) {
        try {
            double bonus = applyMaxBonus(attributeValue * multiplier);
            player.setWalkSpeed((float) Math.min(0.2 + bonus, 1.0));
        } catch (IllegalArgumentException e) {
            logAttributeError(player, "MOVEMENT_SPEED");
        }
    }

    private void applyHealthBonus(@NotNull Player player, int attributeValue) {
        try {
            double newHealth = 20.0 + (attributeValue * multiplier);
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH))
                    .setBaseValue(newHealth);
            player.setHealthScale(newHealth);
        } catch (NullPointerException e) {
            logAttributeError(player, "MAX_HEALTH");
        }
    }

    private void applyKnockbackResistance(@NotNull Player player, int attributeValue) {
        try {
            double resistance = Math.min(1.0, attributeValue * multiplier * 0.1);
            Objects.requireNonNull(player.getAttribute(Attribute.KNOCKBACK_RESISTANCE))
                    .setBaseValue(resistance);
        } catch (NullPointerException e) {
            logAttributeError(player, "KNOCKBACK_RESISTANCE");
        }
    }

    private void applyAttackSpeed(@NotNull Player player, int attributeValue) {
        try {
            double speed = 4.0 + (attributeValue * multiplier * 0.1);
            Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED))
                    .setBaseValue(speed);
        } catch (NullPointerException e) {
            logAttributeError(player, "ATTACK_SPEED");
        }
    }

    private void logAttributeError(@NotNull Player player, String attributeType) {
        plugin.getLogger().warning("Error al aplicar atributo " + attributeType + " a " + player.getName());
    }
}