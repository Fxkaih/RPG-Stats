package rpg.stats.rpg_stats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rpg.stats.rpg_stats.gui.StatsGUI;
import rpg.stats.rpg_stats.managers.AttributeManager;
import rpg.stats.rpg_stats.managers.PlayerProgress;

import java.util.ArrayList;
import java.util.List;

public class AttributesCommand implements CommandExecutor, TabCompleter {
    private final PlayerProgress progress;
    private final StatsGUI statsGUI;

    public AttributesCommand(StatsGUI statsGUI, PlayerProgress progress) {
        if (progress == null || statsGUI == null) {
            throw new IllegalArgumentException("PlayerProgress and StatsGUI cannot be null");
        }
        this.progress = progress;
        this.statsGUI = statsGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo lo puede ejecutar un jugador.");
            return false;
        }

        if (args.length == 0) {
            statsGUI.openStatsMenu(player);
            return true;
        }

        String attribute = args[0].toLowerCase();

        AttributeManager attributeManager = progress.getAttributeManager(); // Ahora debería reconocer getAttributeManager
        if (!attributeManager.isValidAttribute(attribute)) {
            player.sendMessage("§cInvalid attribute. Available attributes: " +
                    String.join(", ", attributeManager.getAttributeNames()));
            return false;
        }

        int availablePoints = progress.getAvailablePoints(player);
        if (availablePoints <= 0) {
            player.sendMessage("§cNo tienes puntos disponibles.");
            return false;
        }

        if (progress.getAttribute(player, attribute) >= progress.getAttributeManager().getMaxValue(attribute)) {
            player.sendMessage("§c¡Ya has alcanzado el máximo nivel en este atributo!");
            return false;
        }

        progress.addAttributePoint(player, attribute);
        player.sendMessage(String.format(
                "§a¡Mejorado %s a §6%d§a! Puntos restantes: §e%d",
                progress.getAttributeManager().getAttributeDisplayName(attribute),
                progress.getAttribute(player, attribute),
                progress.getAvailablePoints(player)
        ));

        statsGUI.openStatsMenu(player);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(progress.getAttributeManager().getAttributeNames());
        }
        return null;
    }
}