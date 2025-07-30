package rpg.stats.rpg_stats.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import rpg.stats.rpg_stats.managers.PlayerProgress;


public class RPGActionsListener implements Listener {
    private final PlayerProgress progress;
    private final JavaPlugin plugin;
    private final FileConfiguration config; // Añade esta línea


    public RPGActionsListener(PlayerProgress progress, JavaPlugin plugin) {
        if (progress == null || plugin == null) {
            throw new IllegalArgumentException("PlayerProgress y JavaPlugin no pueden ser nulos");
        }
        this.progress = progress;
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        progress.loadPlayerData(player);
        progress.applyAllAttributeEffects(player);
    }


    private float calculateXpForEntity(Entity entity) {
        String configPath = "xp-settings.combat.base-xp." + entity.getType();
        if (config.contains(configPath)){
            return (float) config.getDouble(configPath);
        }
        // Valores por defecto si no hay configuración
        return switch (entity.getType()) {
            case ZOMBIE, SKELETON -> 5.0f;
            case CREEPER -> 7.0f;
            case ENDERMAN -> 10.0f;
            case PLAYER -> 20.0f;
            default -> 3.0f;
        };
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        plugin.getLogger().info("[DEBUG] Jugador " + killer.getName() + " mató a " + event.getEntityType());
        float xp = calculateXpForEntity(event.getEntity());
        plugin.getLogger().info("[DEBUG] XP calculado: " + xp);

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        xp = applyWeaponMultiplier(weapon, xp);

        progress.addXP(killer, "combat", xp);
        killer.sendActionBar(
                Component.text("+" + xp + " XP (Combate)")
                        .color(NamedTextColor.GREEN)
        );
        progress.regenerateMana(killer, 2);
    }

    private float applyWeaponMultiplier(ItemStack weapon, float baseXp) {
        String weaponName = weapon.getType().name();
        String configPath = "xp-settings.combat.weapon-multipliers.";
        if (weapon.getType() == Material.AIR) {
            return baseXp * 0.5f; // Reducción si no usa arma
        }


        // Multiplicadores configurables
        if (weaponName.contains("NETHERITE") && config.contains(configPath + "NETHERITE")) {
            return baseXp * (float) config.getDouble(configPath + "NETHERITE");
        } else if (weaponName.contains("DIAMOND") && config.contains(configPath + "DIAMOND")) {
            return baseXp * (float) config.getDouble(configPath + "DIAMOND");
        } else if (weaponName.contains("IRON") && config.contains(configPath + "IRON")) {
            return baseXp * (float) config.getDouble(configPath + "IRON");
        } else if (weaponName.contains("GOLD") && config.contains(configPath + "GOLD")) {
            return baseXp * (float) config.getDouble(configPath + "GOLD");
        } else if (weaponName.contains("STONE") && config.contains(configPath + "STONE")) {
            return baseXp * (float) config.getDouble(configPath + "STONE");
        } else if (weaponName.contains("WOODEN") && config.contains(configPath + "WOODEN")) {
            return baseXp * (float) config.getDouble(configPath + "WOODEN");
        }

        return baseXp; // Sin multiplicador si no coincide
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        progress.regenerateMana(player, 1);

        if (isCorrectToolForBlock(tool.getType(), blockType)) {
            plugin.getLogger().info("[DEBUG] Jugador " + player.getName() + " rompió " + blockType);
            float xp = calculateXpForBlock(blockType);
            plugin.getLogger().info("[DEBUG] XP calculado: " + xp);
            xp = applyToolMultiplier(tool, xp);

            progress.addXP(player, "MINING", xp);
            player.sendActionBar(
                    Component.text("+" + xp + " XP por minar " + blockType.name().toLowerCase())
                            .color(NamedTextColor.GREEN)
            );
        }
    }

    private boolean isCorrectToolForBlock(Material tool, Material block) {
        return switch (block) {
            case GRASS_BLOCK, DIRT, SAND -> tool.toString().contains("SHOVEL");
            case STONE, COBBLESTONE, COBBLED_DEEPSLATE, IRON_ORE, DEEPSLATE_IRON_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, GOLD_ORE, DEEPSLATE_GOLD_ORE ->
                    tool.toString().contains("PICKAXE");
            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 MANGROVE_LOG, CHERRY_LOG -> tool.toString().contains("AXE");
            default -> false;
        };
    }

    private float calculateXpForBlock(Material block) {
        String configPath = "xp-settings.mining.block-xp." + block.toString();
        if (config.contains(configPath)) {
            return (float) config.getDouble(configPath);
        }
        // Valores por defecto si no hay configuración
        return switch (block) {
            case DIAMOND_ORE -> 10.0f;
            case IRON_ORE -> 5.0f;
            case STONE -> 1.0f;
            default -> 0.5f;
        };
    }

    private float applyToolMultiplier(ItemStack tool, float baseXp) {
        String toolName = tool.getType().name();
        float multiplier = 1.0f;

        if (toolName.contains("NETHERITE")) multiplier = 1.5f;
        else if (toolName.contains("DIAMOND")) multiplier = 1.2f;
        else if (toolName.contains("IRON")) multiplier = 1.05f;
        else if (toolName.contains("GOLD")) multiplier = 1.04f;

        return baseXp * multiplier;
    }

}