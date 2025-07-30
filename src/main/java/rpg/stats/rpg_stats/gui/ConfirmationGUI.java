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
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.function.Consumer;

public class ConfirmationGUI implements Listener {
    private final Inventory inventory;
    private final Consumer<Boolean> responseConsumer;
    private final JavaPlugin plugin;

    public ConfirmationGUI(String question, Consumer<Boolean> responseConsumer, JavaPlugin plugin) {
        this.plugin = plugin;
        this.responseConsumer = responseConsumer;
        this.inventory = Bukkit.createInventory(null, 9, Component.text("Confirmar: " + question));

        // Botón de confirmación (verde)
        ItemStack confirmItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(Component.text("Confirmar", NamedTextColor.GREEN));
        confirmItem.setItemMeta(confirmMeta);

        // Botón de cancelación (rojo)
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text("Cancelar", NamedTextColor.RED));
        cancelItem.setItemMeta(cancelMeta);

        // Rellenar el inventario
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 9; i++) {
            if (i == 3) {
                inventory.setItem(i, confirmItem);
            } else if (i == 5) {
                inventory.setItem(i, cancelItem);
            } else {
                inventory.setItem(i, filler);
            }
        }

        // Registrar eventos
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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