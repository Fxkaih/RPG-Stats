package rpg.stats.rpg_stats.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import rpg.stats.rpg_stats.managers.PlayerProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

public class StatsGUI {
    private final PlayerProgress progress;
    private final JavaPlugin plugin;
    private final HashMap<UUID, Integer> pendingPoints = new HashMap<>();

    private final ItemStack backgroundItem;
    private final ItemStack confirmButton;
    private final ItemStack resetButton;

    public StatsGUI(PlayerProgress progress, JavaPlugin plugin) {
        this.progress = Objects.requireNonNull(progress);
        this.plugin = Objects.requireNonNull(plugin);
        this.backgroundItem = createBackgroundItem();
        this.confirmButton = createConfirmButton();
        this.resetButton = createResetButton();
    }

    private ItemStack createBackgroundItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConfirmButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§aConfirmar cambios"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createResetButton() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§cReiniciar Estadísticas"));
        meta.lore(Arrays.asList(
                Component.text("§7Click para reiniciar todas tus"),
                Component.text("§7estadísticas a valores iniciales"),
                Component.text(""),
                Component.text("§e¡Esta acción no se puede deshacer!")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public void openStatsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Gestión de Atributos"));

        Arrays.asList(0,1,2,3,4,5,6,7,8,9,17,18,19,20,21,22,23,24,25,26)
                .forEach(slot -> gui.setItem(slot, backgroundItem));

        gui.setItem(4, createPlayerInfoItem(player));

        gui.setItem(11, createAttributeItem(player, "fuerza"));
        gui.setItem(13, createAttributeItem(player, "destreza"));
        gui.setItem(15, createAttributeItem(player, "constitucion"));

        gui.setItem(22, confirmButton);

        if (player.hasPermission("rpgstats.reset")) {
            gui.setItem(18, resetButton);
        }

        player.openInventory(gui);
    }

    private ItemStack createPlayerInfoItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§eTus Estadísticas"));

        int availablePoints = progress.getAvailablePoints(player);
        int pending = pendingPoints.getOrDefault(player.getUniqueId(), 0);
        float xp = progress.getCurrentXP(player);
        float neededXP = progress.getXPToNextLevel(player);
        int level = progress.getLevel(player);

        List<Component> lore = Arrays.asList(
                Component.text("Nivel: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(level).color(TextColor.color(0x55FF55))),
                Component.text("Progreso: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(String.format("%.1f/%.1f", xp, neededXP)).color(TextColor.color(0x55FFFF))),
                Component.text("Puntos disponibles: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(availablePoints).color(TextColor.color(0xFFFF55))),
                Component.text("Puntos pendientes: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(pending).color(TextColor.color(0xFFAA00))),
                Component.empty(),
                Component.text("¡Selecciona un atributo y confirma!").color(TextColor.color(0x55FF55)),
                Component.text("Click izquierdo: +1 punto").color(TextColor.color(0x55FF55)),
                Component.text("Click derecho: +todos los puntos").color(TextColor.color(0x55FF55))
        );

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributeItem(Player player, String attribute) {
        Material material = switch (attribute) {
            case "fuerza" -> Material.IRON_SWORD;
            case "destreza" -> Material.FEATHER;
            case "constitucion" -> Material.APPLE;
            default -> Material.BARRIER;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6" + capitalizeFirstLetter(attribute)));

        int currentValue = progress.getAttribute(player, attribute);
        int pending = pendingPoints.getOrDefault(player.getUniqueId(), 0);

        List<Component> lore = Arrays.asList(
                Component.text("Nivel actual: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(currentValue).color(TextColor.color(0x55FF55))),
                Component.text("Puntos pendientes: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(pending).color(TextColor.color(0xFFFF55))),
                Component.empty(),
                Component.text("Click izquierdo: +1 punto").color(TextColor.color(0x55FF55)),
                Component.text("Click derecho: +todos los puntos").color(TextColor.color(0x55FF55))
        );

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(Player player, int slot, boolean isRightClick) {
        switch (slot) {
            case 11 -> selectAttribute(player, "fuerza", isRightClick);
            case 13 -> selectAttribute(player, "destreza", isRightClick);
            case 15 -> selectAttribute(player, "constitucion", isRightClick);
            case 18 -> handleResetRequest(player);
            case 22 -> confirmChanges(player);
        }
    }

    private void selectAttribute(Player player, String attribute, boolean addAllPoints) {
        int availablePoints = progress.getAvailablePoints(player);
        if (availablePoints <= 0) {
            player.sendMessage("§cNo tienes puntos disponibles.");
            return;
        }

        int pointsToAdd = addAllPoints ? availablePoints : 1;
        pendingPoints.put(player.getUniqueId(), pointsToAdd);
        player.sendMessage("§aAtributo seleccionado: §e" + attribute + " §a(+" + pointsToAdd + " punto(s))");
        openStatsMenu(player);
    }

    private void confirmChanges(Player player) {
        Integer points = pendingPoints.get(player.getUniqueId());
        if (points == null || points <= 0) {
            player.sendMessage("§cSelecciona un atributo y asigna puntos primero.");
            return;
        }

        int availablePoints = progress.getAvailablePoints(player);
        if (availablePoints < points) {
            player.sendMessage("§cNo tienes suficientes puntos disponibles.");
            return;
        }

        // Aquí se implementaría la lógica para aplicar los puntos al atributo correspondiente
        // Por ejemplo:
        // progress.addAttributePoints(player, getSelectedAttribute(player), points);

        pendingPoints.remove(player.getUniqueId());
        openStatsMenu(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 2, 0), 20);
    }

    private void handleResetRequest(Player player) {
        new ConfirmationGUI(
                "¿Seguro que quieres reiniciar tus estadísticas?",
                confirmed -> {
                    if (confirmed) {
                        progress.resetPlayerStats(player);
                        player.sendMessage("§a¡Tus estadísticas han sido reiniciadas!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        Bukkit.getScheduler().runTask(plugin, () -> openStatsMenu(player));
                    } else {
                        player.sendMessage("§cReinicio cancelado");
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    }
                },
                plugin
        ).open(player);
    }

    public void clearSelections(UUID uuid) {
        pendingPoints.remove(uuid);
    }

    private String capitalizeFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}