package com.spygamingog.spycore.api.events;

import org.bukkit.World;

/**
 * Fired when a world is loaded via SpyCore.
 */
public class SpyWorldLoadEvent extends SpyWorldEvent {
    public SpyWorldLoadEvent(World world, String alias) {
        super(world, alias);
    }
}
