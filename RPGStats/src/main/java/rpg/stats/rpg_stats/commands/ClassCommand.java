package rpg.stats.rpg_stats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rpg.stats.rpg_stats.managers.RPGClassManager;

public class ClassCommand implements CommandExecutor {
    private final RPGClassManager classManager;

    public ClassCommand(RPGClassManager classManager) {
        this.classManager = classManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUso: /classinfo <clase>");
            return true;
        }

        // Usando el método displayClassInfo
        classManager.displayClassInfo(player, args[0].toLowerCase());
        return true;
    }
}