package im5lb.interactiveimage.api.event;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class InteractiveFrameClickEvent extends Event implements Cancellable {

    public enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemFrame itemFrame;
    private final ClickType clickType;
    private final String providerId;
    private final String mapName;
    private final String title;

    private boolean cancelled = false;

    public InteractiveFrameClickEvent(Player player, ItemFrame itemFrame, ClickType clickType, String providerId, String mapName, String title) {
        this.player = player;
        this.itemFrame = itemFrame;
        this.clickType = clickType;
        this.providerId = providerId;
        this.mapName = mapName;
        this.title = title;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }

    public ClickType getClickType() {
        return clickType;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getMapName() {
        return mapName;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
