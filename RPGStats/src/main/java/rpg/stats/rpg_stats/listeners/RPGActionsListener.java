package rpg.stats.rpg_stats.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import rpg.stats.rpg_stats.events.PlayerLevelUpEvent;
import rpg.stats.rpg_stats.managers.PlayerProgress;
import rpg.stats.rpg_stats.managers.XPDisplay;

import java.util.HashMap;
import java.util.Map;


public class RPGActionsListener implements Listener {
    private final PlayerProgress progress;
    private final Map<Material, Float> blockXPValues;
    private final Map<EntityType, Float> entityXPValues;
    private final Map<Material, Float> toolMultipliers;
    private final JavaPlugin plugin;
    private final XPDisplay xpDisplay;

    private final FileConfiguration config; // Añade esta línea


    public RPGActionsListener(PlayerProgress progress,
                              XPDisplay xpDisplay,
                              JavaPlugin plugin
                              ) {
        if (progress == null || plugin == null) {
            throw new IllegalArgumentException("PlayerProgress y JavaPlugin no pueden ser nulos");
        }
        this.progress = progress;
        this.xpDisplay = xpDisplay;
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.blockXPValues = setupBlockXPValues();
        this.entityXPValues = setupEntityXPValues();
        this.toolMultipliers = setupToolMultipliers();

        setupAutoSave(plugin);
    }

    private Map<Material, Float> setupBlockXPValues() {
        Map<Material, Float> values = new HashMap<>();
        // Bloques de minería
        values.put(Material.DIAMOND_ORE, 10.0f);
        values.put(Material.DEEPSLATE_DIAMOND_ORE, 12.0f);
        values.put(Material.IRON_ORE, 5.0f);
        values.put(Material.DEEPSLATE_IRON_ORE, 6.0f);
        values.put(Material.GOLD_ORE, 7.0f);
        values.put(Material.DEEPSLATE_GOLD_ORE, 8.0f);
        values.put(Material.COAL_ORE, 3.0f);
        values.put(Material.DEEPSLATE_COAL_ORE, 3.5f);
        values.put(Material.REDSTONE_ORE, 4.0f);
        values.put(Material.DEEPSLATE_REDSTONE_ORE, 4.5f);
        values.put(Material.LAPIS_ORE, 4.0f);
        values.put(Material.DEEPSLATE_LAPIS_ORE, 4.5f);
        values.put(Material.EMERALD_ORE, 15.0f);
        values.put(Material.DEEPSLATE_EMERALD_ORE, 17.0f);
        values.put(Material.NETHER_GOLD_ORE, 5.0f);
        values.put(Material.NETHER_QUARTZ_ORE, 3.0f);
        values.put(Material.ANCIENT_DEBRIS, 20.0f);
        values.put(Material.STONE, 1.0f);
        values.put(Material.COBBLESTONE, 0.5f);
        values.put(Material.DEEPSLATE, 1.2f);
        values.put(Material.ANDESITE, 0.8f);
        values.put(Material.DIORITE, 0.8f);
        values.put(Material.GRANITE, 0.8f);
        values.put(Material.BASALT, 1.0f);
        values.put(Material.BLACKSTONE, 1.0f);
        values.put(Material.NETHERRACK, 0.5f);
        values.put(Material.END_STONE, 1.5f);
        values.put(Material.DIRT, 1.0f);
        values.put(Material.GRASS_BLOCK, 1.2f);
        values.put(Material.PODZOL, 1.3f);
        values.put(Material.COARSE_DIRT, 0.8f);
        values.put(Material.ROOTED_DIRT, 1.5f);
        values.put(Material.DIRT_PATH, 0.5f);
        values.put(Material.FARMLAND, 1.0f);
        values.put(Material.MYCELIUM, 1.8f);

        // Bloques de madera
        values.put(Material.OAK_LOG, 2.0f);
        values.put(Material.SPRUCE_LOG, 2.0f);
        values.put(Material.BIRCH_LOG, 2.0f);
        values.put(Material.JUNGLE_LOG, 2.0f);
        values.put(Material.ACACIA_LOG, 2.0f);
        values.put(Material.DARK_OAK_LOG, 2.0f);
        values.put(Material.MANGROVE_LOG, 2.0f);
        values.put(Material.CHERRY_LOG, 2.5f);

        return values;
    }

    private Map<EntityType, Float> setupEntityXPValues() {
        Map<EntityType, Float> values = new HashMap<>();
        // Mobs hostiles
        values.put(EntityType.ZOMBIE, 5.0f);
        values.put(EntityType.SKELETON, 5.0f);
        values.put(EntityType.CREEPER, 7.0f);
        values.put(EntityType.SPIDER, 4.0f);
        values.put(EntityType.ENDERMAN, 10.0f);
        values.put(EntityType.WITCH, 12.0f);
        values.put(EntityType.BLAZE, 8.0f);
        values.put(EntityType.GHAST, 15.0f);
        values.put(EntityType.WITHER_SKELETON, 12.0f);
        values.put(EntityType.PHANTOM, 9.0f);

        // Mobs pasivos
        values.put(EntityType.COW, 2.0f);
        values.put(EntityType.PIG, 2.0f);
        values.put(EntityType.SHEEP, 2.0f);
        values.put(EntityType.CHICKEN, 1.5f);
        values.put(EntityType.RABBIT, 1.5f);

        // Jefes
        values.put(EntityType.ENDER_DRAGON, 100.0f);
        values.put(EntityType.WITHER, 50.0f);

        return values;
    }

    private Map<Material, Float> setupToolMultipliers() {
        Map<Material, Float> multipliers = new HashMap<>();
        // Herramientas de minería
        multipliers.put(Material.WOODEN_PICKAXE, 0.8f);
        multipliers.put(Material.STONE_PICKAXE, 1.0f);
        multipliers.put(Material.IRON_PICKAXE, 1.2f);
        multipliers.put(Material.GOLDEN_PICKAXE, 1.5f); // Oro da más XP pero se gasta rápido
        multipliers.put(Material.DIAMOND_PICKAXE, 1.3f);
        multipliers.put(Material.NETHERITE_PICKAXE, 1.5f);

        // Hachas
        multipliers.put(Material.WOODEN_AXE, 0.8f);
        multipliers.put(Material.STONE_AXE, 1.0f);
        multipliers.put(Material.IRON_AXE, 1.1f);
        multipliers.put(Material.GOLDEN_AXE, 1.4f);
        multipliers.put(Material.DIAMOND_AXE, 1.2f);
        multipliers.put(Material.NETHERITE_AXE, 1.4f);

        // Espadas (para mobs)
        multipliers.put(Material.WOODEN_SWORD, 0.9f);
        multipliers.put(Material.STONE_SWORD, 1.0f);
        multipliers.put(Material.IRON_SWORD, 1.3f);
        multipliers.put(Material.GOLDEN_SWORD, 1.6f);
        multipliers.put(Material.DIAMOND_SWORD, 1.4f);
        multipliers.put(Material.NETHERITE_SWORD, 1.6f);

        // Palas
        multipliers.put(Material.WOODEN_SHOVEL, 0.8f);
        multipliers.put(Material.STONE_SHOVEL, 1.0f);
        multipliers.put(Material.IRON_SHOVEL, 1.2f);
        multipliers.put(Material.GOLDEN_SHOVEL, 1.5f);
        multipliers.put(Material.DIAMOND_SHOVEL, 1.3f);
        multipliers.put(Material.NETHERITE_SHOVEL, 1.5f);


        return multipliers;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        progress.loadPlayerData(player);
        progress.applyAllAttributeEffects(player);
        xpDisplay.updateDisplay(player);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        float xp = calculateXpForEntity(event.getEntity());
        ItemStack weapon = killer.getInventory().getItemInMainHand();

        killer.sendMessage("XP base: " + xp);

        xp = applyWeaponMultiplier(weapon, xp);

        killer.sendMessage("XP con multiplicador de arma; " + xp);

        if (weapon.getType().toString().contains("_SWORD") && killer.isJumping()) {
            xp *= 1.5f;

            killer.sendMessage("XP con bonus de salto: " + xp);
        }

        xp *= getEnchantmentBonus(weapon);
        killer.sendMessage("XP con encantamientos: " + xp);
        xp *= progress.getCombatXPBonus(killer);
        killer.sendMessage("XP final: " + xp);

        progress.addXP(killer, "combat", xp);
        killer.sendActionBar(Component.text("+" + String.format("%.1f", xp) + " XP (Combate)", NamedTextColor.GREEN));
        progress.regenerateMana(killer, 2);

        Bukkit.getScheduler().runTaskLater(plugin, () -> progress.savePlayerData(killer), 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        ItemStack tool = player.getInventory().getItemInMainHand();

        float xp = calculateXpForBlock(blockType);
        player.sendMessage("XP base por bloque: " + xp);
        if (xp <= 0 || !isCorrectToolForBlock(tool.getType(), blockType)) {
            player.sendMessage("No se otorgó xp - Razón: " + (xp <= 0 ? "XP cero" : "Herramienta incorrecta"));
            return;
        }

        xp = applyToolMultiplier(tool, xp);
        player.sendMessage("XP con multiplicador de herramienta: " + xp);
        xp *= getEnchantmentBonus(tool);
        player.sendMessage("XP con encantamientos: " + xp);
        xp *= progress.getMiningXPBonus(player);
        player.sendMessage("XP final: " + xp);

        progress.addXP(player, "mining", xp);
        player.sendActionBar(Component.text("+" + String.format("%.1f", xp) + " XP (Minería)", NamedTextColor.GREEN));
        progress.regenerateMana(player, 1);

        Bukkit.getScheduler().runTaskLater(plugin, () -> progress.savePlayerData(player), 5L);
    }

    private float calculateXpForEntity(Entity entity) {
        String configPath = "xp-settings.combat.base-xp." + entity.getType();
        if (config.isSet(configPath)) {
            return (float) config.getDouble(configPath);
        }
        return entityXPValues.getOrDefault(entity.getType(), 3.0f);
    }

    private float calculateXpForBlock(Material block) {
        String configPath = "xp-settings.mining.block-xp." + block.toString();
        if (config.isSet(configPath)) {
            return (float) config.getDouble(configPath);
        }
        return blockXPValues.getOrDefault(block, 0.0f);
    }

    private float applyWeaponMultiplier(ItemStack weapon, float baseXp) {
        if (weapon.getType() == Material.AIR) {
            return baseXp * 0.5f;
        }

        String configPath = "xp-settings.combat.weapon-multipliers." + weapon.getType();
        if (config.isSet(configPath)) {
            return baseXp * (float) config.getDouble(configPath);
        }
        return baseXp * toolMultipliers.getOrDefault(weapon.getType(), 1.0f);
    }

    private float applyToolMultiplier(ItemStack tool, float baseXp) {
        String configPath = "xp-settings.mining.tool-multipliers." + tool.getType();
        if (config.isSet(configPath)) {
            return baseXp * (float) config.getDouble(configPath);
        }

        float multiplier = toolMultipliers.getOrDefault(tool.getType(), 1.0f);

        if (tool.getItemMeta() instanceof Damageable meta) {
            double durabilityRatio = 1.0 - ((double) meta.getDamage() / tool.getType().getMaxDurability());
            multiplier *= (0.8f + (float) (durabilityRatio * 0.4));
        }

        return baseXp * multiplier;
    }

    private float getEnchantmentBonus(ItemStack item) {
        float bonus = 1.0f;

        if (item.containsEnchantment(Enchantment.FORTUNE)) {
            bonus += 0.15f * item.getEnchantmentLevel(Enchantment.EFFICIENCY);
        }
        if (item.containsEnchantment(Enchantment.LOOTING)) {
            bonus += 0.2f * item.getEnchantmentLevel(Enchantment.LOOTING);
        }
        if (item.containsEnchantment(Enchantment.EFFICIENCY)) {
            bonus += 0.1f * item.getEnchantmentLevel(Enchantment.EFFICIENCY);
        }

        return Math.min(bonus, 2.0f);
    }

    private boolean isCorrectToolForBlock(Material tool, Material block) {
        String toolType = tool.toString();

        // Picos (minerales, piedras y bloques duros)
        if (toolType.contains("_PICKAXE")) {
            return isOre(block) ||
                    block == Material.STONE ||
                    block == Material.DEEPSLATE ||
                    block == Material.COBBLESTONE ||
                    block == Material.BLACKSTONE ||
                    block == Material.BASALT ||
                    block == Material.ANDESITE ||
                    block == Material.DIORITE ||
                    block == Material.GRANITE ||
                    block == Material.INFESTED_STONE ||
                    block == Material.INFESTED_COBBLESTONE ||
                    block == Material.INFESTED_DEEPSLATE ||
                    block == Material.CALCITE ||
                    block == Material.TUFF ||
                    block == Material.DRIPSTONE_BLOCK ||
                    block == Material.PRISMARINE ||
                    block == Material.PRISMARINE_BRICKS ||
                    block == Material.DARK_PRISMARINE ||
                    block == Material.END_STONE ||
                    block == Material.PURPUR_BLOCK ||
                    block == Material.NETHER_BRICKS ||
                    block == Material.RED_NETHER_BRICKS ||
                    block == Material.QUARTZ_BLOCK ||
                    block == Material.SMOOTH_QUARTZ ||
                    block == Material.CHISELED_QUARTZ_BLOCK ||
                    block == Material.QUARTZ_PILLAR ||
                    block == Material.MAGMA_BLOCK;
        }

        // Hachas (madera y derivados)
        if (toolType.contains("_AXE")) {
            return block.toString().contains("_LOG") ||
                    block.toString().contains("_WOOD") ||
                    block.toString().contains("_STEM") ||
                    block.toString().contains("_HYPHAE") ||
                    block == Material.CRIMSON_HYPHAE ||
                    block == Material.WARPED_HYPHAE ||
                    block == Material.CRIMSON_STEM ||
                    block == Material.WARPED_STEM ||
                    block == Material.BOOKSHELF ||
                    block == Material.CHEST ||
                    block == Material.TRAPPED_CHEST ||
                    block == Material.CRAFTING_TABLE ||
                    block == Material.LADDER ||
                    block == Material.JUKEBOX ||
                    block == Material.NOTE_BLOCK ||
                    block == Material.BEEHIVE ||
                    block == Material.BEE_NEST ||
                    block == Material.PUMPKIN ||
                    block == Material.CARVED_PUMPKIN ||
                    block == Material.MELON;
        }

        // Palas (tierra, arena y nieve)
        if (toolType.contains("_SHOVEL")) {
            return block == Material.DIRT ||
                    block == Material.GRASS_BLOCK ||
                    block == Material.DIRT_PATH ||
                    block == Material.COARSE_DIRT ||
                    block == Material.PODZOL ||
                    block == Material.ROOTED_DIRT ||
                    block == Material.SAND ||
                    block == Material.RED_SAND ||
                    block == Material.GRAVEL ||
                    block == Material.CLAY ||
                    block == Material.SOUL_SAND ||
                    block == Material.SOUL_SOIL ||
                    block == Material.SNOW_BLOCK ||
                    block == Material.SNOW ||
                    block == Material.FARMLAND ||
                    block == Material.MYCELIUM;
        }

        // Tijeras (hojas, telarañas, vegetación)
        if (tool == Material.SHEARS) {
            return block.toString().contains("_LEAVES") ||
                    block == Material.COBWEB ||
                    block == Material.GRASS_BLOCK ||
                    block == Material.FERN ||
                    block == Material.SEAGRASS ||
                    block == Material.TALL_GRASS ||
                    block == Material.TALL_SEAGRASS ||
                    block == Material.DEAD_BUSH ||
                    block == Material.VINE;
        }

        // Azadas (cultivos y bloques especiales)
        if (toolType.contains("_HOE")) {
            return block == Material.HAY_BLOCK ||
                    block.toString().contains("_LEAVES") ||
                    block == Material.SCULK ||
                    block == Material.SCULK_VEIN ||
                    block == Material.SCULK_CATALYST ||
                    block == Material.SCULK_SHRIEKER ||
                    block == Material.MOSS_BLOCK ||
                    block == Material.MOSS_CARPET ||
                    block == Material.SPONGE ||
                    block == Material.WET_SPONGE;
        }

        return false;
    }

    private boolean isOre(Material block) {
        return block.toString().contains("_ORE") ||
                block == Material.ANCIENT_DEBRIS ||
                block == Material.NETHER_QUARTZ_ORE ||
                block == Material.GILDED_BLACKSTONE;
    }



    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        progress.savePlayerData(player);
        xpDisplay.removePlayer(player);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            progress.savePlayerData(event.getPlayer());
            xpDisplay.removePlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerShutdown(PluginDisableEvent event) {
        // Guardado durante el apagado
        Bukkit.getOnlinePlayers().forEach(progress::savePlayerData);
    }
    // Método para configurar el guardado automático (llamar esto en el constructor)
    private void setupAutoSave(JavaPlugin plugin) {
        // Guardado cada 5 minutos
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            plugin.getLogger().info("Guardando datos de todos los jugadores...");
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    progress.savePlayerData(player);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error al guardar datos de " + player.getName() + ": " + e.getMessage());
                }
            }
        }, 20L * 60 * 5, 20L * 60 * 5); // 5 minutos en ticks
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand().toLowerCase().startsWith("stop") ||
                event.getCommand().toLowerCase().startsWith("restart")) {
            Bukkit.getOnlinePlayers().forEach(progress::savePlayerData);
        }
    }
    @EventHandler
    public void onPlayerLevelUp(PlayerLevelUpEvent event) {
        Player player = event.getPlayer();

        // Mensaje de nivel mejorado
        player.sendMessage(Component.text()
                .content("¡Subiste de nivel ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(event.getOldLevel()).color(NamedTextColor.GRAY))
                .append(Component.text(" → ").color(NamedTextColor.WHITE))
                .append(Component.text(event.getNewLevel()).color(NamedTextColor.GOLD)));

        // Mostrar puntos y mana ganados
        if (event.getPointsGained() > 0) {
            player.sendMessage(Component.text()
                    .content("Has obtenido ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(event.getPointsGained() + " puntos de atributo")
                            .color(NamedTextColor.AQUA))
                    .append(Component.text(" y "))
                    .append(Component.text("+" + event.getManaIncreased() + " mana máximo")
                            .color(NamedTextColor.BLUE)));
        }

        // Barra de progreso (opcional)
        player.sendActionBar(Component.text()
                .content("Progreso: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", event.getCurrentXP())))
                .append(Component.text("/"))
                .append(Component.text(String.format("%.1f", event.getXpToNextLevel()))));

        // Efectos especiales para hitos
        if (event.isMilestoneLevel()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
            player.spawnParticle(Particle.FIREWORK, player.getLocation(), 20);
            player.sendMessage(Component.text()
                    .content("¡Hito alcanzado! (Nivel ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text(event.getNewLevel()))
                    .append(Component.text(")")));
        }

        // Si subió varios niveles de una vez
        if (event.getLevelsGained() > 1) {
            player.sendMessage(Component.text()
                    .content("¡Subiste ")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(event.getLevelsGained() + " niveles"))
                    .append(Component.text(" de una vez!")));
        }
    }
}