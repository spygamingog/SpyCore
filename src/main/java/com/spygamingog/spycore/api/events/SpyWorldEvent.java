package com.spygamingog.spycore.api.events;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base event for all SpyCore world-related events.
 */
public abstract class SpyWorldEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final World world;
    private final String alias;

    public SpyWorldEvent(World world, String alias) {
        this.world = world;
        this.alias = alias;
    }

    public World getWorld() {
        return world;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
