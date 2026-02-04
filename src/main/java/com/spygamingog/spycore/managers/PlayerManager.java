package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spycore.api.events.SpyPlayerRespawnEvent;
import com.spygamingog.spycore.models.PlayerProfile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager implements Listener {
    private final SpyCore plugin;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();

    public PlayerManager(SpyCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void initialize() {
        // Initialize database connection here
        plugin.getLogger().info("Initializing PlayerManager...");
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World deathWorld = player.getWorld();
        String deathWorldName = deathWorld.getName();

        // Get the base name (remove _nether or _the_end)
        String baseName = deathWorldName;
        if (baseName.endsWith("_nether")) {
            baseName = baseName.substring(0, baseName.length() - 7);
        } else if (baseName.endsWith("_the_end")) {
            baseName = baseName.substring(0, baseName.length() - 8);
        }

        // Only redirect if they died in a suffix world
        if (!baseName.equals(deathWorldName)) {
            World targetWorld = SpyAPI.getWorld(baseName);
            if (targetWorld != null) {
                // Find safe location in the base world
                Location respawnLoc = plugin.getWorldManager().findSafeLocation(targetWorld);

                // Call custom event for other plugins to hook into
                SpyPlayerRespawnEvent spyEvent = new SpyPlayerRespawnEvent(player, deathWorld, targetWorld, respawnLoc);
                Bukkit.getPluginManager().callEvent(spyEvent);

                // Update the actual respawn location
                event.setRespawnLocation(spyEvent.getRespawnLocation());
                plugin.getLogger().info("Redirecting respawn for " + player.getName() + " from " + deathWorldName + " to " + targetWorld.getName());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load profile from database
        PlayerProfile profile = new PlayerProfile(player.getUniqueId(), player.getName());
        profiles.put(player.getUniqueId(), profile);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Save profile to database
        profiles.remove(uuid);
    }

    public void shutdown() {
        // Save all profiles
        profiles.clear();
    }
}
