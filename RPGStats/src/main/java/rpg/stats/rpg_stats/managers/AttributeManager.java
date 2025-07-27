package rpg.stats.rpg_stats.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AttributeManager {
    private final Map<String, AttributeConfig> attributes = new HashMap<>();

    public AttributeManager(FileConfiguration config) {
        loadAttributes(config);
    }

    public void reload(FileConfiguration newConfig) {
        this.attributes.clear();
        loadAttributes(newConfig);
    }

    private void loadAttributes(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        if (section == null) return;

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
        return attributes.keySet();
    }

    public int getMaxValue(String attribute) {
        AttributeConfig config = attributes.get(attribute.toLowerCase());
        return config != null ? config.getMaxValue() : 50; // Valor por defecto
    }

    public String getAttributeDisplayName(String attribute) {
        AttributeConfig config = attributes.get(attribute.toLowerCase());
        return config != null ? config.getDisplayName() : attribute;
    }

    public AttributeConfig getAttributeConfig(String name) {
        return attributes.get(name.toLowerCase());
    }

    public void applyAttributeEffects(Player player, String attributeName, int value) {
        AttributeConfig config = attributes.get(attributeName.toLowerCase());
        if (config == null) return;

        for (AttributeEffect effect : config.getEffects()) {
            effect.apply(player, value);
        }
    }
}