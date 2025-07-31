package rpg.stats.rpg_stats.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ConfirmationGUI implements Listener {
    private final Inventory inventory;
    private final Consumer<Boolean> responseConsumer;

    public ConfirmationGUI(String question, Consumer<Boolean> responseConsumer, JavaPlugin plugin) {
        this.responseConsumer = responseConsumer;
        this.inventory = Bukkit.createInventory(null, 27,
                Component.text("Confirmar: " + question));

        // Rellenar todo el inventario con paneles
        ItemStack filler = createFillerItem();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Botón de confirmación (centrado)
        ItemStack confirmItem = createConfirmItem();
        inventory.setItem(11, confirmItem);

        // Botón de cancelación (centrado)
        ItemStack cancelItem = createCancelItem();
        inventory.setItem(15, cancelItem);

        // Texto descriptivo
        ItemStack infoItem = createInfoItem(question);
        inventory.setItem(13, infoItem);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }
    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§aConfirmar"));
        meta.lore(Arrays.asList(
                Component.text("§7Click para confirmar la acción"),
                Component.text("§7Esta acción no se puede deshacer")
        ));
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.LURE, 1);
        return item;
    }

    private ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§cCancelar"));
        meta.lore(List.of(
                Component.text("§7Click para cancelar la acción")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(String question) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e" + question));
        meta.lore(Arrays.asList(
                Component.text("§7¿Estás seguro de que quieres"),
                Component.text("§7realizar esta acción?")
        ));
        item.setItemMeta(meta);
        return item;
    }


    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            switch (clickedItem.getType()) {
                case LIME_WOOL:
                    responseConsumer.accept(true);
                    player.closeInventory();
                    break;
                case RED_WOOL:
                    responseConsumer.accept(false);
                    player.closeInventory();
                    break;
            }

            // Desregistrar el listener después de usar
            InventoryClickEvent.getHandlerList().unregister(this);
        }
    }
}