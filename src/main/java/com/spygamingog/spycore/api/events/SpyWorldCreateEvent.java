package com.spygamingog.spycore.api.events;

import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a new world is created via SpyCore.
 */
public class SpyWorldCreateEvent extends SpyWorldEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public SpyWorldCreateEvent(World world, String alias) {
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
