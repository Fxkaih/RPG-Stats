package rpg.stats.rpg_stats;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import rpg.stats.rpg_stats.commands.*;
import rpg.stats.rpg_stats.events.PlayerLevelUpEvent;
import rpg.stats.rpg_stats.listeners.GUIListener;
import rpg.stats.rpg_stats.gui.StatsGUI;
import rpg.stats.rpg_stats.listeners.RPGActionsListener;
import rpg.stats.rpg_stats.managers.*;
import rpg.stats.rpg_stats.integration.StatsPlaceholders;

import java.util.Objects;

public class Main extends JavaPlugin implements Listener{
    private PlayerProgress playerProgress;
    private XPDisplay xpDisplay;
    private StatsGUI statsGUI;
    private AbilityManager abilityManager;

    @Override
    public void onEnable() {
        try {
            // 1. Configuración inicial
            saveDefaultConfig();
            getConfig().options().copyDefaults(true);

            // 2. Inicialización de componentes
            this.xpDisplay = new XPDisplay(playerProgress, this);
            this.playerProgress = new PlayerProgress(this, getConfig(), xpDisplay);
            this.statsGUI = new StatsGUI(playerProgress);
            ConfigurationSerialization.registerClass(PlayerData.class);


            // Inicializar AbilityManager con los 3 parámetros requeridos
            this.abilityManager = new AbilityManager(getConfig(), playerProgress, this);

            // 3. Registro de eventos
            getServer().getPluginManager().registerEvents(
                    new RPGActionsListener(playerProgress, this), this);
            getServer().getPluginManager().registerEvents(new GUIListener(statsGUI), this);
            getServer().getPluginManager().registerEvents(new RPGActionsListener(playerProgress, this), this);
            getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onPlayerQuit(PlayerQuitEvent event) {
                    playerProgress.savePlayerData(event.getPlayer());
                }

                @EventHandler
                public void onPlayerKick(PlayerKickEvent event) {
                    playerProgress.savePlayerData(event.getPlayer());
                }
            }, this);


            // Registrar eventos de habilidades
            getServer().getPluginManager().registerEvents(new AbilityListener(), this);

            // 4. Registro de comandos
            registerCommands();

            // 5. Integración con PlaceholderAPI
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new StatsPlaceholders(playerProgress).register();
                getLogger().info("PlaceholderAPI integrado correctamente");
            } else {
                getLogger().warning("PlaceholderAPI no está instalado. Algunas funciones pueden no estar disponibles.");
            }
            // 6. Carga de jugadores online
            Bukkit.getOnlinePlayers().forEach(player -> {
                playerProgress.loadPlayerData(player);
                xpDisplay.updateDisplay(player);
                abilityManager.applyPassiveAbilities(player); // Aplicar habilidades pasivas
            });

            getLogger().info("Plugin habilitado correctamente");

        } catch (Exception e) {
            getLogger().severe("Error crítico al iniciar el plugin: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
    
    // Clase interna para manejar eventos de habilidades
    private class AbilityListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            abilityManager.applyPassiveAbilities(player);
        }

        @EventHandler
        public void onPlayerLevelUp(PlayerLevelUpEvent event) {
            Player player = event.getPlayer();
            abilityManager.applyPassiveAbilities(player);
        }
    }

    private void registerCommands() {
        try {
            // Registrar cada comando individualmente
            Objects.requireNonNull(getCommand("atributos")).setExecutor(new AttributesCommand(statsGUI, playerProgress));
            Objects.requireNonNull(getCommand("progreso")).setExecutor(new ProgressCommand(playerProgress, xpDisplay));
            Objects.requireNonNull(getCommand("rpgadmin")).setExecutor(new AdminCommand(this, playerProgress));
            Objects.requireNonNull(getCommand("stats")).setExecutor(new AttributesCommand(statsGUI, playerProgress));
            Objects.requireNonNull(getCommand("habilidad")).setExecutor(new AbilityCommand(abilityManager));
        } catch (Exception e) {
            getLogger().severe("Error al registrar comandos: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            playerProgress.savePlayerData(player);
            getLogger().info("Datos de " + player.getName() + " guardados (onDisable)");
        });
    }


}