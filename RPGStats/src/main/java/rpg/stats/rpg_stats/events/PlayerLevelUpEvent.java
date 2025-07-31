package rpg.stats.rpg_stats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PlayerLevelUpEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    private final int pointsGained;
    private final int manaIncreased;
    private final float currentXP;
    private final float xpToNextLevel;

    public PlayerLevelUpEvent(@NotNull Player player, int oldLevel,
                              int newLevel, int pointsGained, int manaIncrease,
                              float currentXP, float xpToNextLevel) {
        this.player = Objects.requireNonNull(player);
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.pointsGained = pointsGained;
        this.manaIncreased = manaIncrease;
        this.currentXP = currentXP;
        this.xpToNextLevel = xpToNextLevel;
    }

    @NotNull
    public Player getPlayer() { return player; }

    public int getOldLevel() { return oldLevel; }

    public int getNewLevel() { return newLevel; }

    public int getPointsGained() { return pointsGained; }

    public int getManaIncreased() { return manaIncreased; }

    public float getCurrentXP() { return currentXP; }

    public float getXpToNextLevel() { return xpToNextLevel; }

    public boolean isMilestoneLevel() {
        return newLevel % 5 == 0;
    }

    public int getLevelsGained() {
        return newLevel - oldLevel;
    }

    @NotNull
    public static HandlerList getHandlerList() { return handlers; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }
}