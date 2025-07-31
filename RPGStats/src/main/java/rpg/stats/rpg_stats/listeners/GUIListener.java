package rpg.stats.rpg_stats.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;
import rpg.stats.rpg_stats.gui.StatsGUI;

import java.util.Arrays;

public class GUIListener implements Listener {
    private final StatsGUI statsGUI;

    public GUIListener(StatsGUI statsGUI) {
        this.statsGUI = statsGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();

        if (!view.getTitle().equals("Gestión de Atributos")) return;

        event.setCancelled(true); // Cancelar siempre el evento

        // Solo procesar clicks en el inventario superior
        if (event.getClickedInventory() != view.getTopInventory()) return;

        int slot = event.getRawSlot();
        boolean isRightClick = event.isRightClick();

        // Verificar slots válidos
        if (Arrays.asList(11, 13, 15, 14, 16, 17, 22, 18).contains(slot)) {
            statsGUI.handleClick(player, slot, isRightClick);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("Gestión de Atributos")) {
            statsGUI.clearSelections(event.getPlayer().getUniqueId());
        }
    }
}