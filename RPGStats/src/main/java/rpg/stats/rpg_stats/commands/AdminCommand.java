package rpg.stats.rpg_stats.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rpg.stats.rpg_stats.gui.ConfirmationGUI;
import rpg.stats.rpg_stats.managers.PlayerProgress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final PlayerProgress progress;
    private final JavaPlugin plugin;

    public AdminCommand(JavaPlugin plugin, PlayerProgress progress) {
        this.plugin = Objects.requireNonNull(plugin);
        this.progress = Objects.requireNonNull(progress);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("rpgstats.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "setlevel":
                handleSetLevel(sender, args);
                break;
            case "setxp":
                handleSetXP(sender, args);
                break;
            case "setstats":
                handleSetStats(sender, args);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            default:
                sendHelp(sender);
        }
        return true;
    }

    private void handleReload(@NotNull CommandSender sender) {
        plugin.reloadConfig();
        progress.getAttributeManager().reload(plugin.getConfig());
        sender.sendMessage("§aConfiguración recargada correctamente.");
        logAction(sender, "reload", "config");
    }

    private void handleSetLevel(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUso: /rpgadmin setlevel <jugador> <nivel>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }

        try {
            int level = Integer.parseInt(args[2]);
            progress.setLevel(target, level);
            sender.sendMessage(String.format("§aNivel de %s establecido a %d", target.getName(), level));
            target.sendMessage(String.format("§eUn admin ha establecido tu nivel a §6%d", level));
            logAction(sender, "setlevel", target.getName() + " a " + level);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cEl nivel debe ser un número válido.");
        }
    }

    private void handleSetXP(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUso: /rpgadmin setxp <jugador> <xp>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }

        try {
            float xp = Float.parseFloat(args[2]);
            float currentXP = progress.getCurrentXP(target);

            if (xp > currentXP) {
                progress.addXP(target, "admin-command", xp - currentXP);
            } else {
                progress.setXP(target, xp);
            }

            sender.sendMessage(String.format("§aXP de %s establecido a %.1f", target.getName(), xp));
            target.sendMessage(String.format("§eUn admin ha establecido tu XP a §6%.1f", xp));
            logAction(sender, "setxp", target.getName() + " a " + xp);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cEl XP debe ser un número válido.");
        }
    }

    private void handleSetStats(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUso: /rpgadmin setstats <jugador> <atributo> <valor>");
            sender.sendMessage("§cAtributos disponibles: fuerza, destreza, constitucion");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }

        String attribute = args[2].toLowerCase();
        if (!Arrays.asList("fuerza", "destreza", "constitucion").contains(attribute)) {
            sender.sendMessage("§cAtributo no válido. Opciones: fuerza, destreza, constitucion");
            return;
        }

        try {
            int value = Integer.parseInt(args[3]);
            progress.setAttribute(target, attribute, value, false);
            progress.setAvailablePoints(target, 0);
            sender.sendMessage(String.format("§a%s de %s establecido a %d", attribute, target.getName(), value));
            target.sendMessage(String.format("§eUn admin ha establecido tu %s a §6%d", attribute, value));
            logAction(sender, "setstats", target.getName() + " " + attribute + " a " + value);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cEl valor debe ser un número entero.");
        }
    }

    private void handleReset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("§cEste comando solo puede usarse en el juego para la GUI");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /rpgadmin reset <jugador>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }

        if (target.hasPermission("rpgstats.admin")) {
            sender.sendMessage("§cNo puedes resetear a otro admin.");
            return;
        }

        new ConfirmationGUI(
                "Resetear a " + target.getName() + "?",
                confirmed -> {
                    if (confirmed) {
                        progress.resetPlayerStats(target);
                        admin.sendMessage("§aStats de " + target.getName() + " reseteadas");
                        target.sendMessage("§eTus stats fueron reseteadas por un admin");
                        logAction(admin, "reset", target.getName());
                    } else {
                        admin.sendMessage("§cOperación cancelada");
                    }
                },
                plugin
        ).open(admin);
    }

    private void logAction(@NotNull CommandSender sender, @NotNull String action, @NotNull String target) {
        String message = String.format("[Admin Action] %s executed '%s' on %s",
                sender.getName(), action, target);

        plugin.getLogger().info(message);

        try {
            Path path = Path.of(plugin.getDataFolder().getAbsolutePath(), "admin_actions.log");
            Files.writeString(path, LocalDateTime.now() + " - " + message + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Error saving log: " + e.getMessage());
        }
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage("§6=== RPGAdmin Comandos ===");
        sender.sendMessage("§e/rpgadmin reload §7- Recarga la configuración");
        sender.sendMessage("§e/rpgadmin setlevel <jugador> <nivel> §7- Establece nivel");
        sender.sendMessage("§e/rpgadmin setxp <jugador> <xp> §7- Establece XP");
        sender.sendMessage("§e/rpgadmin setstats <jugador> <atributo> <valor> §7- Modifica atributos");
        sender.sendMessage("§e/rpgadmin reset <jugador> §7- Resetea progreso");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("rpgstats.admin")) return null;

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "setlevel", "setxp", "setstats", "reset"));
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setstats")) {
            completions.addAll(progress.getAttributeManager().getAttributeNames());
        }

        return completions;
    }
}