package rpg.stats.rpg_stats.managers;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeConfig {
    private final String displayName;
    private final int maxValue;
    private final List<AttributeEffect> effects;
    private final ConfigurationSection config;

    public AttributeConfig(ConfigurationSection config) {
        this.config = config;
        this.displayName = config.getString("display-name", "");
        this.maxValue = config.getInt("max-value", 50);
        this.effects = loadEffects(config.getConfigurationSection("effects"));
    }

    public Map<String, String> getBonusFormulas() {
        Map<String, String> formulas = new HashMap<>();
        ConfigurationSection bonuses = config.getConfigurationSection("bonuses");

        if (bonuses != null) {
            bonuses.getKeys(false).forEach(key ->
                    formulas.put(key, bonuses.getString(key))
            );
        }
        return formulas;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return config.getString("icon", "BARRIER");
    }

    public String getDescription() {
        return config.getString("description", "");
    }

    public String getFullAttributeInfo(int currentValue) {

        StringBuilder info = new StringBuilder();
        info.append("=== ").append(getDisplayName()).append(" ===\n")
                .append("Icono: ").append(getIcon()).append("\n")
                .append("Descripción: ").append(getDescription()).append("\n\n");

        if (!getBonusFormulas().isEmpty()) {
            info.append("Bonificaciones:\n");
            getBonusFormulas().forEach((desc, formula) ->
                    info.append("- ").append(desc).append("\n")
            );
        }

        return info.append("\nNivel actual: ")
                .append(currentValue)
                .append("/")
                .append(getMaxValue())
                .toString();
    }

    public int getMaxValue() {
        return maxValue;
    }

    public List<AttributeEffect> getEffects() {
        return new ArrayList<>(effects);
    }

    private List<AttributeEffect> loadEffects(ConfigurationSection effectsSection) {
        List<AttributeEffect> effects = new ArrayList<>();
        if (effectsSection == null) return effects;

        effectsSection.getKeys(false).forEach(key -> {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(key);
            if (effectSection != null) {
                effects.add(new AttributeEffect(effectSection));
            }
        });
        return effects;
    }
}