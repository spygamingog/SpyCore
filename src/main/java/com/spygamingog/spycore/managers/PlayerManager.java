package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spycore.api.events.SpyPlayerRespawnEvent;
import com.spygamingog.spycore.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.io.IOException;
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
        String baseName = getGroupName(deathWorldName);

        // ALWAYS redirect to the base world of the group
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

        // Redirect respawn to base world safe location
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Force 1.21 Reach for Entities (Hitting players/mobs)
        // Default: 3.0 blocks
        if (player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE) != null) {
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(3.0);
        }

        // Force 1.21 Reach for Blocks (Building/Breaking)
        // Default: 4.5 blocks
        if (player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE) != null) {
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5);
        }

        // Load profile from individual file
        PlayerProfile profile = new PlayerProfile(player.getUniqueId(), player.getName());
        File playerDir = new File(plugin.getDataFolder(), "players" + File.separator + player.getUniqueId());
        File profileFile = new File(playerDir, "profile.yml");
        
        if (profileFile.exists()) {
             FileConfiguration config = YamlConfiguration.loadConfiguration(profileFile);
             if (config.getConfigurationSection("last_locations") != null) {
                 profile.getData().put("last_locations", config.getConfigurationSection("last_locations").getValues(true));
             }
         }
        
        profiles.put(player.getUniqueId(), profile);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();
        String worldName = world.getName();
        
        String baseName = getGroupName(worldName);

        PlayerProfile profile = getProfile(player.getUniqueId());
        if (profile != null) {
            // When a player dies, we reset their last location for that world group to the base world's spawn
            // This ensures they don't spawn back at their death location (or a stale one) when re-entering the group
            World baseWorld = SpyAPI.getWorld(baseName);
            if (baseWorld != null) {
                Location spawnLoc = baseWorld.getSpawnLocation();
                profile.setLastLocation(baseName, spawnLoc);
                plugin.getLogger().info("Resetting last location for " + player.getName() + " in group " + baseName + " due to death.");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        trackLocation(event.getPlayer(), event.getFrom());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        trackLocation(player, player.getLocation());
        
        PlayerProfile profile = getProfile(player.getUniqueId());
        if (profile != null) {
            // Save profile to individual file
            File playerDir = new File(plugin.getDataFolder(), "players" + File.separator + player.getUniqueId());
            if (!playerDir.exists()) playerDir.mkdirs();
            
            File profileFile = new File(playerDir, "profile.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(profileFile);
            
            // We only save navigation data (last_locations) here.
            config.set("last_locations", profile.getData().get("last_locations"));
            
            try {
                config.save(profileFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save profile for " + player.getName() + ": " + e.getMessage());
            }
        }
        
        profiles.remove(player.getUniqueId());
    }

    public String getGroupName(String worldName) {
        if (worldName.endsWith("_nether")) {
            return worldName.substring(0, worldName.length() - 7);
        } else if (worldName.endsWith("_the_end")) {
            return worldName.substring(0, worldName.length() - 8);
        }
        return worldName;
    }

    private void trackLocation(Player player, Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Use alias for consistency
        String alias = plugin.getWorldManager().getAliasForWorld(world);
        if (alias == null) alias = world.getName();
        
        // Skip tracking if they are in the lobby or a world that isn't part of a group we care about
        if (alias.equalsIgnoreCase("lobby")) return;

        PlayerProfile profile = getProfile(player.getUniqueId());
        if (profile != null) {
            profile.setLastLocation(alias, location);
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            trackLocation(player, player.getLocation());

            PlayerProfile profile = getProfile(player.getUniqueId());
            if (profile != null) {
                File playerDir = new File(plugin.getDataFolder(), "players" + File.separator + player.getUniqueId());
                if (!playerDir.exists()) playerDir.mkdirs();

                File profileFile = new File(playerDir, "profile.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(profileFile);
                config.set("last_locations", profile.getData().get("last_locations"));
                try {
                    config.save(profileFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save profile on shutdown for " + player.getName() + ": " + e.getMessage());
                }
            }
        }
        profiles.clear();
    }
}
