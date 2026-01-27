package com.spygamingog.spycore.api.events;

import org.bukkit.World;

/**
 * Fired when a new world is created via SpyCore.
 */
public class SpyWorldCreateEvent extends SpyWorldEvent {
    public SpyWorldCreateEvent(World world, String alias) {
        super(world, alias);
    }
}
