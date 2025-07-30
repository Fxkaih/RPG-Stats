package rpg.stats.rpg_stats.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rpg.stats.rpg_stats.managers.PlayerProgress;
import rpg.stats.rpg_stats.managers.XPDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProgressCommand implements CommandExecutor, TabCompleter {
    private final PlayerProgress progress;
    private final XPDisplay xpDisplay;
    private static final List<String> SUB_COMMANDS = Arrays.asList("ver", "nivel");

    public ProgressCommand(PlayerProgress progress, XPDisplay xpDisplay) {
        this.progress = progress;
        this.xpDisplay = xpDisplay;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("ver"))) {
            return showProgress(sender);
        }

        if (args[0].equalsIgnoreCase("nivel")) {
            return handleLevelCommand(sender, args);
        }

        sender.sendMessage(Component.text("Uso: /progreso [ver|nivel]", NamedTextColor.RED));
        return false;
    }

    private boolean showProgress(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED));
            return false;
        }

        xpDisplay.updateDisplay(player);
        int level = progress.getLevel(player);
        float currentXP = progress.getCurrentXP(player);
        float neededXP = progress.getXPToNextLevel(player);
        float progressPercent = (currentXP / neededXP) * 100;

        player.sendMessage(Component.text("=== TU PROGRESO ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text()
                .content("Nivel: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(level, NamedTextColor.GREEN)));
        player.sendMessage(Component.text()
                .content("XP: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.1f/%.1f (%.1f%%)", currentXP, neededXP, progressPercent), NamedTextColor.GREEN)));
        player.sendMessage(Component.text()
                .content("Puntos de atributo: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(progress.getAvailablePoints(player), NamedTextColor.GREEN)));

        return true;
    }

    private boolean handleLevelCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Uso: /progreso nivel <jugador> <nivel>");
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("Jugador no encontrado");
            return true;
        }

        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1 || level > 100) {
                sender.sendMessage("Nivel debe estar entre 1 y 100");
                return true;
            }

            progress.setLevel(target, level);
            progress.setXP(target, 0);
            progress.applyAllAttributeEffects(target);

            sender.sendMessage("Nivel de " + target.getName() + " actualizado a " + level);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("Nivel debe ser un número");
            return true;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Filtra los subcomandos que coincidan con lo que el jugador está escribiendo
            for (String subCmd : SUB_COMMANDS) {
                if (subCmd.startsWith(args[0].toLowerCase())) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("nivel")) {
            // Autocompletar nombres de jugadores para el subcomando "nivel"
            String partialName = args[1].toLowerCase();
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .forEach(completions::add);
        }

        return completions.isEmpty() ? null : completions;
    }
}