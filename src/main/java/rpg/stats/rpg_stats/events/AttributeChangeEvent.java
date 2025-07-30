package rpg.stats.rpg_stats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AttributeChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String attribute;
    private final int oldValue;
    private final int newValue;
    private final boolean isLevelUp;

    public AttributeChangeEvent(@NotNull Player player, @NotNull String attribute,
                                int oldValue, int newValue, boolean isLevelUp) {
        this.player = Objects.requireNonNull(player);
        this.attribute = Objects.requireNonNull(attribute);
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.isLevelUp = isLevelUp;
    }

    @NotNull
    public Player getPlayer() { return player; }

    @NotNull
    public String getAttribute() { return attribute; }

    public int getOldValue() { return oldValue; }

    public int getNewValue() { return newValue; }

    public boolean isLevelUp() { return isLevelUp; }

    @NotNull
    public static HandlerList getHandlerList() { return handlers; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }
}