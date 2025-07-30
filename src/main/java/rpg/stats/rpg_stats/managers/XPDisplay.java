package rpg.stats.rpg_stats.managers;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XPDisplay {
    private final JavaPlugin plugin;
    private PlayerProgress playerProgress;
    private final Map<UUID, Long> lastUpdate = new HashMap<>();

    public XPDisplay(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setPlayerProgress(@NotNull PlayerProgress playerProgress) {
        this.playerProgress = playerProgress;
    }

    public void updateDisplay(@NotNull Player player) {
        if (playerProgress == null) return;

        long now = System.currentTimeMillis();
        if (now - lastUpdate.getOrDefault(player.getUniqueId(), 0L) < 1000) {
            return;
        }
        lastUpdate.put(player.getUniqueId(), now);

        float currentXP = playerProgress.getCurrentXP(player);
        float neededXP = playerProgress.getXPToNextLevel(player);
        int level = playerProgress.getLevel(player);

        String progressBar = createProgressBar(currentXP / neededXP, 10);
        String message = String.format("§eNivel %d §7| §b%.1f§7/§b%.1f XP §7[%s§7]",
                level, currentXP, neededXP, progressBar);

        player.sendActionBar(message);
        player.setLevel(level);
        player.setExp(Math.min(0.999F, currentXP / neededXP));
    }

    private String createProgressBar(double progress, int length) {
        int filled = (int) (progress * length);
        return "§a" + "|".repeat(Math.max(0, filled)) +
                "§7" + "|".repeat(Math.max(0, length - filled));
    }

    public void removePlayer(@NotNull Player player) {
        lastUpdate.remove(player.getUniqueId());
        player.sendActionBar("");
    }

    public void cleanup() {
        lastUpdate.clear();
    }
}