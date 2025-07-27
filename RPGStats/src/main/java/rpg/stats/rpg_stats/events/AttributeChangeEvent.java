package rpg.stats.rpg_stats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AttributeChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String attribute;
    private final int oldValue;
    private final int newValue;

    public AttributeChangeEvent(Player player, String attribute, int oldValue, int newValue) {
        this.player = player;
        this.attribute = attribute;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Player getPlayer() { return player; }
    public String getAttribute() { return attribute; }
    public int getOldValue() { return oldValue; }
    public int getNewValue() { return newValue; }

    @NotNull
    public static HandlerList getHandlerList() { return handlers; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }
}