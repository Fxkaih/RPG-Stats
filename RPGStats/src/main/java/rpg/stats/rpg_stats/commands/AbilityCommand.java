package rpg.stats.rpg_stats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rpg.stats.rpg_stats.managers.AbilityManager;
import java.util.Map;
public class AbilityCommand implements CommandExecutor {
    private final AbilityManager abilityManager;

    public AbilityCommand(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando");
            return true;
        }

        Player player = (Player) sender; // Corrección: Convertir sender a Player

        if (args.length == 0) {
            listAvailableAbilities(player);
            return true;
        }

        String abilityId = args[0].toLowerCase();
        handleAbilityActivation(player, abilityId);
        return true;
    }
    private void listAvailableAbilities(Player player) {
        player.sendMessage("§6=== Habilidades Disponibles ===");

        // Obtener todas las habilidades del manager
        Map<String, AbilityManager.Ability> abilities = abilityManager.getAbilitiesMap();

        for (Map.Entry<String, AbilityManager.Ability> entry : abilities.entrySet()) {
            AbilityManager.Ability ability = entry.getValue();
            if (abilityManager.checkAbilityCondition(player, entry.getKey())) {
                player.sendMessage(String.format(
                        "§a- %s §7(/habilidad %s) §8(Nivel %d)",
                        ability.getName(),
                        entry.getKey(),
                        ability.getRequiredLevel()
                ));
            }
        }
    }

    private void handleAbilityActivation(Player player, String abilityId) {
        if (abilityManager.checkAbilityCondition(player, abilityId)) {
            abilityManager.activateAbility(player, abilityId);
            player.sendMessage("§a¡Habilidad activada con éxito!");
        } else {
            player.sendMessage("§cNo cumples los requisitos para esta habilidad");
        }
    }
}