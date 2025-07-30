package rpg.stats.rpg_stats.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AbilityManager {
    private final Map<String, Ability> abilities = new HashMap<>();
    private final PlayerProgress playerProgress;
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public AbilityManager(@NotNull FileConfiguration config,
                          @NotNull PlayerProgress playerProgress,
                          @NotNull JavaPlugin plugin) {
        this.playerProgress = Objects.requireNonNull(playerProgress);
        this.plugin = Objects.requireNonNull(plugin);
        loadAbilities(Objects.requireNonNull(config));
    }

    private void loadAbilities(@NotNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("abilities");
        if (section == null) {
            plugin.getLogger().warning("No se encontró la sección 'abilities' en config.yml");
            return;
        }

        for (String abilityId : section.getKeys(false)) {
            ConfigurationSection abilitySection = section.getConfigurationSection(abilityId);
            if (abilitySection != null) {
                Ability ability = new Ability(abilityId, abilitySection, playerProgress, plugin);
                abilities.put(abilityId.toLowerCase(), ability);
            }
        }
    }

    public boolean checkAbilityCondition(@NotNull Player player, @NotNull String abilityId) {
        Ability ability = getAbility(abilityId);
        if (ability == null) return false;

        if (isOnCooldown(player, ability)) {
            long remaining = getRemainingCooldown(player, ability);
            player.sendMessage(String.format("§e%s en cooldown (%d segundos restantes)",
                    ability.getName(), TimeUnit.MILLISECONDS.toSeconds(remaining)));
            return false;
        }

        if (playerProgress.getCurrentMana(player) < ability.getManaCost()) {
            player.sendMessage(String.format("§eNo tienes suficiente maná (%d requeridos)", ability.getManaCost()));
            return false;
        }

        return ability.canActivate(player);
    }

    public void activateAbility(@NotNull Player player, @NotNull String abilityId) {
        Ability ability = getAbility(abilityId);
        if (ability == null || !checkAbilityCondition(player, abilityId)) return;

        int manaCost = calculateManaCost(player, ability);
        if (!playerProgress.consumeMana(player, manaCost)) {
            player.sendMessage("§eNo tienes suficiente maná");
            return;
        }

        ability.activate(player);
        applyCooldown(player, ability);
    }

    private int calculateManaCost(Player player, Ability ability) {
        int manaCost = ability.getManaCost();
        String playerClass = playerProgress.getCurrentClass(player.getUniqueId());

        if (playerClass != null) {
            RPGClassManager.RPGClass rpgClass = playerProgress.getClassManager()
                    .getRPGClass(playerClass);
            if (rpgClass != null) {
                manaCost *= (int) rpgClass.getManaCostMultiplier(ability.getType());
            }
        }
        return manaCost;
    }

    public void applyPassiveAbilities(@NotNull Player player) {
        abilities.values().stream()
                .filter(ability -> "passive".equalsIgnoreCase(ability.getType()))
                .forEach(ability -> ability.applyPassiveEffects(player));
    }

    public @NotNull Map<String, Ability> getAbilitiesMap() {
        return Collections.unmodifiableMap(abilities);
    }

    private @Nullable Ability getAbility(@NotNull String abilityId) {
        return abilities.get(abilityId.toLowerCase());
    }

    private boolean isOnCooldown(Player player, Ability ability) {
        if (ability.getCooldown() <= 0) return false;
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        return playerCooldowns != null && playerCooldowns.getOrDefault(ability.getId(), 0L) > System.currentTimeMillis();
    }

    private long getRemainingCooldown(Player player, Ability ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        return Math.max(0, playerCooldowns.getOrDefault(ability.getId(), 0L) - System.currentTimeMillis());
    }

    public void cleanUpPlayer(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    private void applyCooldown(Player player, Ability ability) {
        if (ability.getCooldown() > 0) {
            cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(ability.getId(), System.currentTimeMillis() + (long)(ability.getCooldown() * 1000));
        }
    }

    public static class Ability {
        private final String id;
        private final String name;
        private final String type;
        private final int requiredLevel;
        private final Map<String, Integer> requiredAttributes;
        private final Map<PotionEffectType, Integer> effects;
        private final double cooldown;
        private final int manaCost;
        private final PlayerProgress playerProgress;
        private final JavaPlugin plugin;

        public Ability(@NotNull String id,
                       @NotNull ConfigurationSection config,
                       @NotNull PlayerProgress playerProgress,
                       @NotNull JavaPlugin plugin) {
            this.id = Objects.requireNonNull(id);
            this.name = config.getString("name", id);
            this.type = config.getString("type", "passive").toLowerCase();
            this.requiredLevel = Math.max(1, config.getInt("required-level", 1));
            this.cooldown = Math.max(0, config.getDouble("cooldown", 0.0));
            this.manaCost = Math.max(0, config.getInt("mana-cost", 0));
            this.playerProgress = playerProgress;
            this.plugin = plugin;
            this.requiredAttributes = loadAttributes(config);
            this.effects = loadEffects(config);
        }

        private @NotNull Map<String, Integer> loadAttributes(@NotNull ConfigurationSection config) {
            Map<String, Integer> attributes = new HashMap<>();
            ConfigurationSection section = config.getConfigurationSection("required-attributes");
            if (section != null) {
                section.getKeys(false).forEach(attr ->
                        attributes.put(attr, Math.max(0, section.getInt(attr, 0)))
                );
            }
            return attributes;
        }

        private @NotNull Map<PotionEffectType, Integer> loadEffects(@NotNull ConfigurationSection config) {
            Map<PotionEffectType, Integer> effects = new HashMap<>();
            ConfigurationSection section = config.getConfigurationSection("effects");
            if (section != null) {
                section.getKeys(false).forEach(effect -> {
                    PotionEffectType type = getModernPotionType(effect);
                    if (type != null) {
                        effects.put(type, Math.max(1, section.getInt(effect, 1)));
                    }
                });
            }
            return effects;
        }

        private @Nullable PotionEffectType getModernPotionType(@NotNull String name) {
            return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(name.toLowerCase()));
        }

        public boolean canActivate(@NotNull Player player) {
            if (playerProgress.getLevel(player) < requiredLevel) {
                player.sendMessage("§eNecesitas nivel " + requiredLevel + " para esta habilidad");
                return false;
            }

            for (Map.Entry<String, Integer> entry : requiredAttributes.entrySet()) {
                if (playerProgress.getAttribute(player, entry.getKey()) < entry.getValue()) {
                    player.sendMessage("§eNecesitas " + entry.getKey() + " " + entry.getValue());
                    return false;
                }
            }
            return true;
        }

        public void activate(@NotNull Player player) {
            if (!canActivate(player)) return;
            applyEffectsModern(player);
            player.sendMessage("§a¡Habilidad " + name + " activada!");
            player.sendMessage(String.format("§7Cooldown: %.1fs | Coste de maná: %d", cooldown, manaCost));
        }

        public void applyPassiveEffects(@NotNull Player player) {
            if ("passive".equals(type)) {
                applyEffectsModern(player);
            }
        }

        private void applyEffectsModern(@NotNull Player player) {
            effects.forEach((type, amplifier) -> {
                player.addPotionEffect(new PotionEffect(
                        type,
                        20 * 30, // 30 segundos
                        amplifier - 1,
                        true,
                        true,
                        true
                ));
            });
        }

        public @NotNull String getId() { return id; }
        public @NotNull String getName() { return name; }
        public @NotNull String getType() { return type; }
        public int getRequiredLevel() { return requiredLevel; }
        public double getCooldown() { return cooldown; }
        public int getManaCost() { return manaCost; }
    }
}