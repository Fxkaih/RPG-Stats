package rpg.stats.rpg_stats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rpg.stats.rpg_stats.managers.AbilityManager;
import java.util.Objects;

public class AbilityCommand implements CommandExecutor {
    private final AbilityManager abilityManager;

    public AbilityCommand(AbilityManager abilityManager) {
        this.abilityManager = Objects.requireNonNull(abilityManager, "AbilityManager no puede ser nulo");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando");
            return true;
        }

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
        abilityManager.getAbilitiesMap().forEach((id, ability) -> {
            if (abilityManager.checkAbilityCondition(player, id)) {
                player.sendMessage(String.format(
                        "§a- %s §7(/habilidad %s) §8(Nivel %d)",
                        ability.getName(),
                        id,
                        ability.getRequiredLevel()
                ));
            }
        });
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