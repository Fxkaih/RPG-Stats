package rpg.stats.rpg_stats.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import rpg.stats.rpg_stats.gui.StatsGUI;

import java.util.Arrays;

public class GUIListener implements Listener {
    private final StatsGUI statsGUI;

    public GUIListener(StatsGUI statsGUI) {
        this.statsGUI = statsGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(Component.text("Gestión de Atributos"))) return;

        event.setCancelled(true);

        if (Arrays.asList(11, 13, 15, 22).contains(event.getRawSlot())) {
            boolean isRightClick = event.isRightClick();
            statsGUI.handleClick(player, event.getRawSlot(), isRightClick);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        statsGUI.clearSelections(event.getPlayer().getUniqueId());
    }
}