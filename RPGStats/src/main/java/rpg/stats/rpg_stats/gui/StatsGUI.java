package rpg.stats.rpg_stats.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import rpg.stats.rpg_stats.managers.PlayerProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

import java.util.*;

public class StatsGUI implements Listener {
    private final PlayerProgress progress;
    private final JavaPlugin plugin;
    private final HashMap<UUID, Integer> pendingPoints = new HashMap<>();
    private final HashMap<UUID, String>selectedAttributes = new HashMap<>();

    private final ItemStack InfoItem;
    private final Map<String, Material>attributeIcons = Map.of(
            "fuerza", Material.IRON_SWORD,
            "destreza", Material.FEATHER,
            "constitucion", Material.APPLE,
            "inteligencia", Material.ENCHANTED_BOOK,
            "sabiduria", Material.EXPERIENCE_BOTTLE,
            "agilidad", Material.LEATHER_BOOTS,
            "precision", Material.ARROW
    );

    private final ItemStack backgroundItem;
    private final ItemStack confirmButton;
    private final ItemStack resetButton;

    public StatsGUI(PlayerProgress progress, JavaPlugin plugin) {
        this.progress = Objects.requireNonNull(progress);
        this.plugin = Objects.requireNonNull(plugin);
        this.InfoItem = createInfoItem();
        this.backgroundItem = createBackgroundItem();
        this.confirmButton = createConfirmButton();
        this.resetButton = createResetButton();
    }

    private ItemStack createInfoItem(){
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§eInformación de Atributos"));
        meta.lore(Arrays.asList(
                Component.text("§7Fuerza: §aAumenta el daño cuerpo a cuerpo"),
                Component.text("§7Destreza: §aAumenta la velocidad de ataque"),
                Component.text("§7Constitución: §aAumenta la vida máxima"),
                Component.text("§7Inteligencia: §aAumenta el poder mágico"),
                Component.text("§7Sabiduría: §aReduce cooldowns"),
                Component.text("§7Precisión: §aAumenta daño a distancia"),
                Component.text("§7Agilidad: §aAumenta velocidad de movimiento")
        ));
        item.setItemMeta(meta);
        return item;
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
        Inventory gui = Bukkit.createInventory(null, 27,
                Component.text("Gestión de Atributos"));

        Arrays.asList(0,1,2,3,4,5,6,7,8,9,17,18,19,20,21,22,23,24,25,26)
                .forEach(slot -> gui.setItem(slot, backgroundItem));

        gui.setItem(4, createPlayerInfoItem(player));
        gui.setItem(0, InfoItem);

        gui.setItem(11, createAttributeItem(player, "fuerza"));
        gui.setItem(13, createAttributeItem(player, "destreza"));
        gui.setItem(15, createAttributeItem(player, "constitucion"));

        // Atributos secundarios
        gui.setItem(14, createAttributeItem(player, "inteligencia"));
        gui.setItem(16, createAttributeItem(player, "sabiduria"));
        gui.setItem(17, createAttributeItem(player, "precision"));

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
        ItemStack item = new ItemStack(attributeIcons.getOrDefault(attribute, Material.BARRIER));
        ItemMeta meta = item.getItemMeta();

        int currentValue = progress.getAttribute(player, attribute);
        int pending = pendingPoints.getOrDefault(player.getUniqueId(), 0);
        boolean isSelected = attribute.equals(selectedAttributes.get(player.getUniqueId()));

        // Color diferente si está seleccionado
        TextColor nameColor = isSelected ? TextColor.color(0xFFAA00) : TextColor.color(0x55FF55);
        meta.displayName(Component.text(capitalizeFirstLetter(attribute)).color(nameColor));

        // Descripción del atributo
        String bonusInfo = progress.getAttributeManager().getAttributeBonusInfo(attribute, currentValue);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Nivel actual: ").color(TextColor.color(0xAAAAAA))
                .append(Component.text(currentValue).color(TextColor.color(0x55FF55))));

        if (pending > 0 && isSelected) {
            lore.add(Component.text("Puntos pendientes: ").color(TextColor.color(0xAAAAAA))
                    .append(Component.text(pending).color(TextColor.color(0xFFAA00))));
        }

        lore.add(Component.text("Efectos: ").color(TextColor.color(0xAAAAAA))
                .append(Component.text(bonusInfo).color(TextColor.color(0x55FFFF))));

        lore.add(Component.empty());
        lore.add(Component.text("Click para seleccionar").color(TextColor.color(0x55FF55)));

        meta.lore(lore);
        item.setItemMeta(meta);

        // Añadir glow si está seleccionado
        if (isSelected) {
            item.addUnsafeEnchantment(Enchantment.BLAST_PROTECTION, 1);
        }

        return item;
    }

    public void handleClick(Player player, int slot, boolean isRightClick) {
        switch (slot) {
            case 11 -> handleAttributeClick(player, "fuerza", isRightClick);
            case 13 -> handleAttributeClick(player, "destreza", isRightClick);
            case 15 -> handleAttributeClick(player, "constitucion", isRightClick);
            case 14 -> handleAttributeClick(player, "inteligencia", isRightClick);
            case 16 -> handleAttributeClick(player, "sabiduria", isRightClick);
            case 17 -> handleAttributeClick(player, "precision", isRightClick);
            case 22 -> confirmChanges(player);
            case 18 -> handleResetRequest(player);
            default -> {
                // No hacer nada para otros slots
            }
        }
    }

    private void handleAttributeClick(Player player, String attribute, boolean isRightClick) {
        int availablePoints = progress.getAvailablePoints(player);
        if (availablePoints <= 0) {
            player.sendMessage("§cNo tienes puntos disponibles.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            return;
        }

        UUID uuid = player.getUniqueId();
        int pointsToAdd = isRightClick ? Math.min(availablePoints, 5) : 1; // Máximo 5 puntos con click derecho

        if (attribute.equals(selectedAttributes.get(uuid))) {
            int newPoints = pendingPoints.getOrDefault(uuid, 0) + pointsToAdd;
            if (newPoints > availablePoints) {
                player.sendMessage("§cNo tienes suficientes puntos.");
                return;
            }
            pendingPoints.put(uuid, newPoints);
        } else {
            selectedAttributes.put(uuid, attribute);
            pendingPoints.put(uuid, pointsToAdd);
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        updateInventory(player);
    }

    // Actualiza el inventario sin cerrarlo
    private void updateInventory(Player player) {
        InventoryView view = player.getOpenInventory();

        // Usar Adventure Component para comparar títulos
        Component guiTitle = Component.text("Gestión de Atributos");

        if (view.title().equals(guiTitle)) {
            Inventory inv = view.getTopInventory();

            // Actualizar solo los ítems necesarios
            inv.setItem(4, createPlayerInfoItem(player));
            inv.setItem(11, createAttributeItem(player, "fuerza"));
            inv.setItem(13, createAttributeItem(player, "destreza"));
            inv.setItem(15, createAttributeItem(player, "constitucion"));
            inv.setItem(14, createAttributeItem(player, "inteligencia"));
            inv.setItem(16, createAttributeItem(player, "sabiduria"));
            inv.setItem(17, createAttributeItem(player, "precision"));

            player.updateInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }


    private void confirmChanges(Player player) {
        UUID uuid = player.getUniqueId();
        String selectedAttribute = selectedAttributes.get(uuid);
        Integer points = pendingPoints.get(uuid);

        if (selectedAttribute == null || points == null || points <= 0) {
            player.sendMessage("§cSelecciona un atributo y asigna puntos primero.");
            return;
        }

        if (progress.getAvailablePoints(player) < points) {
            player.sendMessage("§cNo tienes suficientes puntos disponibles.");
            return;
        }

        // Aplicar todos los puntos de una vez
        progress.setAttribute(player, selectedAttribute,
                progress.getAttribute(player, selectedAttribute) + points, false);
        progress.setAvailablePoints(player, progress.getAvailablePoints(player) - points);

        // Resetear selección
        selectedAttributes.remove(uuid);
        pendingPoints.remove(uuid);

        // Efectos
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 5);
        openStatsMenu(player);
    }

    // Añade este método para prevenir el drag and drop
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().title().equals(Component.text("Gestión de Atributos"))) {
            event.setCancelled(true);
        }
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