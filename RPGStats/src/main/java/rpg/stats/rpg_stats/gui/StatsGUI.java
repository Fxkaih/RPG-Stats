package rpg.stats.rpg_stats.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rpg.stats.rpg_stats.managers.PlayerProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class StatsGUI {
    private final PlayerProgress progress;
    private final HashMap<UUID, String> selectedAttributes = new HashMap<>();
    private final HashMap<UUID, Integer> pendingPoints = new HashMap<>();

    // Items reutilizables
    private final ItemStack backgroundItem;
    private final ItemStack confirmButton;

    public StatsGUI(PlayerProgress progress) {
        this.progress = progress;
        this.backgroundItem = createBackgroundItem();
        this.confirmButton = createConfirmButton();
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

    public void openStatsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Gestión de Atributos"));

        // Fondo
        Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26)
                .forEach(slot -> gui.setItem(slot, backgroundItem));

        // Información del jugador
        gui.setItem(4, createPlayerInfoItem(player));

        // Botones de atributos
        gui.setItem(11, createAttributeItem(player, "fuerza"));
        gui.setItem(13, createAttributeItem(player, "destreza"));
        gui.setItem(15, createAttributeItem(player, "constitucion"));

        // Botón de confirmación
        gui.setItem(22, confirmButton);
    if (player.hasPermission("rpgstats.admin")){
        ItemStack resetButton = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = resetButton.getItemMeta();
        meta.displayName(Component.text("§cReinicio temporal (Admin)"));
        resetButton.setItemMeta(meta);
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

        List<Component> lore = Arrays.asList(
                Component.text("Nivel: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(progress.getLevel(player)).color(TextColor.color(0x55FF55))),
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

        List<Component> lore = Arrays.asList(
                Component.text("Nivel actual: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(progress.getAttribute(player, attribute)).color(TextColor.color(0x55FF55))),
                Component.text("Puntos pendientes: ").color(TextColor.color(0xAAAAAA))
                        .append(Component.text(pendingPoints.getOrDefault(player.getUniqueId(), 0)).color(TextColor.color(0xFFFF55))),
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
            case 18 -> {
                if (player.hasPermission("rpgstats.admin")){
                    progress.resetPlayerStats(player);
                    player.sendMessage("§aEstadisticas reiniciadas temporalmente (no guardado)");
                    openStatsMenu(player);
                }
            }
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
        pendingPoints.merge(player.getUniqueId(), pointsToAdd, Integer::sum);
        selectedAttributes.put(player.getUniqueId(), attribute);

        player.sendMessage("§aAtributo seleccionado: §e" + attribute + " §a(+" + pointsToAdd + " punto(s))");
    }

    private void confirmChanges(Player player) {
        UUID uuid = player.getUniqueId();
        String attribute = selectedAttributes.get(uuid);
        Integer points = pendingPoints.get(uuid);

        if (attribute == null || points == null || points <= 0) {
            player.sendMessage("§cSelecciona un atributo y asigna puntos primero.");
            return;
        }

        int availablePoints = progress.getAvailablePoints(player);
        if (availablePoints < points) {
            player.sendMessage("§cNo tienes suficientes puntos disponibles.");
            return;
        }

        // Aplicar los puntos
        for (int i = 0; i < points; i++) {
            progress.addAttributePoint(player, attribute);
        }

        player.sendMessage("§a¡" + points + " punto(s) asignados a §6" + attribute + "§a!");

        // Limpiar selecciones
        pendingPoints.remove(uuid);
        selectedAttributes.remove(uuid);

        // Actualizar GUI
        openStatsMenu(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 2, 0),20);
    }

    public void clearSelections(UUID uuid) {
        pendingPoints.remove(uuid);
        selectedAttributes.remove(uuid);
    }

    private String capitalizeFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}