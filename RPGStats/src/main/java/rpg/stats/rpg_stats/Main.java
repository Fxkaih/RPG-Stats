package rpg.stats.rpg_stats;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import rpg.stats.rpg_stats.commands.*;
import rpg.stats.rpg_stats.gui.StatsGUI;
import rpg.stats.rpg_stats.managers.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends JavaPlugin {
    private PlayerProgress playerProgress;
    private AbilityManager abilityManager;
    private XPDisplay xpDisplay;
    private StatsGUI statsGUI;
    private RPGClassManager classManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration config = getConfig();

        // Inicialización de managers en orden correcto
        this.xpDisplay = new XPDisplay(this);
        this.playerProgress = new PlayerProgress(this, config, xpDisplay);
        this.abilityManager = new AbilityManager(config, playerProgress, this);
        this.statsGUI = new StatsGUI(playerProgress, this);
        this.classManager = new RPGClassManager(config); // Pasar la configuración correctamente

        playerProgress.setXpDisplay(xpDisplay);

        // Registro de eventos
        Bukkit.getPluginManager().registerEvents(new DataSaveListener(playerProgress), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(playerProgress, abilityManager), this);

        // Registro de comandos mejorado
        registerCommands();
    }

    private void registerCommands() {
        // Mapa de comandos y sus executors (usando diamond operator <>)
        Map<String, CommandExecutor> commands = new LinkedHashMap<>();
        commands.put("habilidad", new AbilityCommand(abilityManager));
        commands.put("rpgadmin", new AdminCommand(this, playerProgress));
        commands.put("atributos", new AttributesCommand(statsGUI, playerProgress));
        commands.put("classinfo", new ClassCommand(classManager));
        commands.put("progreso", new ProgressCommand(playerProgress, xpDisplay));

        // Registrar cada comando
        commands.forEach((name, executor) -> {
            try {
                PluginCommand command = getCommand(name);
                if (command == null) {
                    getLogger().warning("El comando /" + name + " no está definido en plugin.yml!");
                    return;
                }

                command.setExecutor(executor);

                // Registrar TabCompleter si corresponde
                if (executor instanceof TabCompleter) {
                    command.setTabCompleter((TabCompleter) executor);
                    getLogger().info("TabCompleter para /" + name + " registrado");
                }

                getLogger().info("Comando /" + name + " registrado correctamente");
            } catch (Exception e) {
                getLogger().severe("Error al registrar el comando /" + name + ": " + e);
            }
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        if ("abilities".equalsIgnoreCase(command.getName())) {
            showAbilitiesList(player);
            return true;
        }
        return false;
    }

    private void showAbilitiesList(@NotNull Player player) {
        player.sendMessage("§6=== HABILIDADES DISPONIBLES ===");
        abilityManager.getAbilitiesMap().forEach((id, ability) ->
                player.sendMessage(String.format("§e- %s §7(ID: %s, Nivel requerido: %d)",
                        ability.getName(),
                        id,
                        ability.getRequiredLevel()))
        );
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(playerProgress::savePlayerData);
    }
}

// Listeners se mantienen igual
record DataSaveListener(PlayerProgress playerProgress) implements org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        playerProgress.savePlayerData(event.getPlayer());
    }
}

record PlayerListener(PlayerProgress playerProgress, AbilityManager abilityManager) implements org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerProgress.loadPlayerData(player);
    }
}