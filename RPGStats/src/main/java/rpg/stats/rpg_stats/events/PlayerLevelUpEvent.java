package rpg.stats.rpg_stats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerLevelUpEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int oldLevel;
    private final int newLevel;

    public PlayerLevelUpEvent(@NotNull Player player, int oldLevel, int newLevel) {
        super(player);
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @NotNull
    public static HandlerList getHandlerList() { return handlers; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }
}