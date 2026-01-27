package com.spygamingog.spycore.api.events;

import org.bukkit.World;

/**
 * Fired when a world is unloaded via SpyCore.
 */
public class SpyWorldUnloadEvent extends SpyWorldEvent {
    public SpyWorldUnloadEvent(World world, String alias) {
        super(world, alias);
    }
}
