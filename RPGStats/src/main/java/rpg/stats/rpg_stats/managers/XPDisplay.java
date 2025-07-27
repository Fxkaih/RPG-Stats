package rpg.stats.rpg_stats.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.UUID;

public class XPDisplay {
    private final PlayerProgress progress;
    private final HashMap<UUID, BossBar> bossBars = new HashMap<>();
    private final JavaPlugin plugin;

    public XPDisplay(@NotNull PlayerProgress progress, @NotNull JavaPlugin plugin) {
        this.progress = progress;
        this.plugin = plugin;
    }

    public void updateDisplay(@NotNull Player player) {
        if (player == null || !player.isOnline()) return;

        try {
            BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(),
                    uuid -> Bukkit.createBossBar(getTitle(player), BarColor.GREEN, BarStyle.SEGMENTED_20));

            float progressPercent = calculateProgress(player);
            bar.setTitle(getTitle(player));
            bar.setProgress(progressPercent);
            bar.setColor(getProgressColor(progressPercent));
            bar.addPlayer(player);

            if (plugin.getConfig().getBoolean("debug.xp-display", false)) {
                plugin.getLogger().info("BossBar actualizada para " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al actualizar BossBar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private @NotNull String getTitle(@NotNull Player player) {
        try {
            float progressPercent = calculateProgress(player) * 100;
            return String.format("Nivel %d | %.1f/%.1f XP (%.1f%%) | Maná: %d/%d",
                    progress.getLevel(player),
                    progress.getCurrentXP(player),
                    progress.getXPToNextLevel(player),
                    progressPercent,
                    progress.getCurrentMana(player),
                    progress.getMaxMana(player));
        } catch (Exception e) {
            return "Nivel " + progress.getLevel(player); // Fallback simple
        }
    }

    private float calculateProgress(@NotNull Player player) {
        float xpNeeded = progress.getXPToNextLevel(player);
        return xpNeeded > 0 ? Math.min(progress.getCurrentXP(player) / xpNeeded, 1.0f) : 1.0f;
    }

    private @NotNull BarColor getProgressColor(float progress) {
        if (progress < 0.3) return BarColor.RED;
        if (progress < 0.7) return BarColor.YELLOW;
        return BarColor.GREEN;
    }

    public void removePlayer(@NotNull Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
        }
    }

    public void cleanup() {
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
    }
}