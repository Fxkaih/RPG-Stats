package rpg.stats.rpg_stats.managers;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.List;

public class AttributeConfig {
    private final String displayName;
    private final String icon;
    private final String description;
    private final int maxValue;
    private final List<AttributeEffect> effects;

    public AttributeConfig(ConfigurationSection config) {
        this.displayName = config.getString("display-name", "");
        this.icon = config.getString("icon", "BARRIER");
        this.description = config.getString("description", "");
        this.maxValue = config.getInt("max-value", 50);
        this.effects = loadEffects(config.getConfigurationSection("effects"));
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public List<AttributeEffect> getEffects() {
        return effects;
    }

    private List<AttributeEffect> loadEffects(ConfigurationSection effectsSection) {
        List<AttributeEffect> effects = new ArrayList<>();
        if (effectsSection == null) return effects;

        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(key);
            if (effectSection != null) {
                effects.add(new AttributeEffect(effectSection));
            }
        }
        return effects;
    }
}