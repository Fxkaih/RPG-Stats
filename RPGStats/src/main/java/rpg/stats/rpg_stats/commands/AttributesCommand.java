package rpg.stats.rpg_stats.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            statsGUI.openStatsMenu(player);
            progress.showAvailableClasses(player);
            return true;
        }

        String attribute = args[0].toLowerCase();
        AttributeManager attributeManager = progress.getAttributeManager();

        if (!attributeManager.isValidAttribute(attribute)) {
            player.sendMessage(Component.text("Atributo no válido. Opciones: ", NamedTextColor.RED)
                    .append(Component.text(String.join(", ", attributeManager.getAttributeNames()), NamedTextColor.YELLOW)));
            return false;
        }

        int availablePoints = progress.getAvailablePoints(player);
        if (availablePoints <= 0) {
            player.sendMessage(Component.text("No tienes puntos disponibles.", NamedTextColor.RED));
            return false;
        }

        int currentValue = progress.getAttribute(player, attribute);
        int maxValue = attributeManager.getMaxValue(attribute);
        if (currentValue >= maxValue) {
            player.sendMessage(Component.text("¡Máximo nivel alcanzado!", NamedTextColor.RED));
            return false;
        }

        progress.addAttributePoint(player, attribute);

        player.sendMessage(Component.text()
                .append(Component.text("Atributo mejorado: ", NamedTextColor.GREEN))
                .append(Component.text(attributeManager.getAttributeDisplayName(attribute), NamedTextColor.YELLOW))
                .append(Component.text(" (Nivel ", NamedTextColor.GREEN))
                .append(Component.text(currentValue + 1, NamedTextColor.GOLD))
                .append(Component.text(")", NamedTextColor.GREEN)));

        // Llamada actualizada al método renombrado
        progress.refreshPlayerDisplay(player);
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