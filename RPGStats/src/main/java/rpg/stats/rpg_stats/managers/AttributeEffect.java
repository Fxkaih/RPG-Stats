package rpg.stats.rpg_stats.managers;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Objects;

public class AttributeEffect {
    private final String type;
    private final double multiplier;
    private final Double maxBonus;

    public AttributeEffect(ConfigurationSection config) {
        this.type = config.getString("type", "").toUpperCase();
        this.multiplier = config.getDouble("multiplier", 1.0);
        this.maxBonus = config.isDouble("max-bonus") ? config.getDouble("max-bonus") : null;
    }

    public void apply(Player player, int attributeValue) {
        switch (type) {
            case "DAMAGE":
                applyDamageBonus(player, attributeValue);
                break;
            case "MOVEMENT_SPEED":
                applySpeedBonus(player, attributeValue);
                break;
            case "MAX_HEALTH":
                applyHealthBonus(player, attributeValue);
                break;
            default:
                player.sendMessage("Efecto de atributo desconocido: " + type);
        }
    }

    private void applyDamageBonus(Player player, int attributeValue) {
        double bonus = attributeValue * multiplier;
        Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_DAMAGE))
                .setBaseValue(1.0 + bonus);
    }

    private void applySpeedBonus(Player player, int attributeValue) {
        double bonus = attributeValue * multiplier;
        if (maxBonus != null && bonus > maxBonus) {
            bonus = maxBonus;
        }
        player.setWalkSpeed((float) Math.min(0.2 + bonus, 1.0));
    }

    private void applyHealthBonus(Player player, int attributeValue) {
        double bonus = attributeValue * multiplier;
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH))
                .setBaseValue(20.0 + bonus);
        player.setHealthScale(20.0 + bonus);
        if (player.getHealth() > 20.0) {
            player.setHealth(20.0 + bonus);
        }
    }
}