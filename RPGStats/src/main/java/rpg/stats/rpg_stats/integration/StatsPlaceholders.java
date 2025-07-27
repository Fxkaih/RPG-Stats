package rpg.stats.rpg_stats.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rpg.stats.rpg_stats.managers.PlayerProgress;

public class StatsPlaceholders extends PlaceholderExpansion {
    private final PlayerProgress progress;

    public StatsPlaceholders(PlayerProgress progress) {
        this.progress = progress;
    }

    // Métodos requeridos
    @Override
    public @NotNull String getIdentifier() {
        return "rpgstats";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TuNombre";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // Persiste después de recargar
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    // Método que faltaba para el registro
    @Override
    public boolean register() {
        return super.register();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return null;

        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(progress.getLevel(player));
            case "xp":
                return String.format("%.1f", progress.getCurrentXP(player));
            case "next_level_xp":
                return String.format("%.1f", progress.getXPToNextLevel(player));
            case "attribute_points":
                return String.valueOf(progress.getAvailablePoints(player));
            default:
                return null;
        }
    }
}