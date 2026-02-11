package com.spygamingog.spycore.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when SpyCore determines a respawn location for a player.
 * Other plugins can use this to override where players spawn after death.
 */
@Getter
@Setter
public class SpyPlayerRespawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final World deathWorld;
    private final World targetBaseWorld;
    private Location respawnLocation;

    public SpyPlayerRespawnEvent(Player player, World deathWorld, World targetBaseWorld, Location respawnLocation) {
        this.player = player;
        this.deathWorld = deathWorld;
        this.targetBaseWorld = targetBaseWorld;
        this.respawnLocation = respawnLocation;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
