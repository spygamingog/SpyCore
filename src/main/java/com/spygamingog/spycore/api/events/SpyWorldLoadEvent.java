package com.spygamingog.spycore.api.events;

import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a world is loaded via SpyCore.
 */
public class SpyWorldLoadEvent extends SpyWorldEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public SpyWorldLoadEvent(World world, String alias) {
        super(world, alias);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
