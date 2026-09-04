package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.api.DataService;
import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spycore.api.events.SpyPlayerRespawnEvent;
import com.spygamingog.spycore.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.potion.PotionEffect;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;

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

        // If respawning in a different world group, load the target world group's inventory.
        // Do NOT save the current inventory because the player died (items dropped/cleared on death).
        if (!getGroupName(event.getRespawnLocation().getWorld().getName()).equals(baseName)) {
            loadInventory(player, event.getRespawnLocation().getWorld().getName());
        }
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

        // Load inventory for current world
        loadInventory(player, player.getWorld().getName());
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
        
        // If teleporting between world groups, save/load inventory
        if (event.getTo() != null && event.getTo().getWorld() != null && !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            String fromGroup = getGroupName(event.getFrom().getWorld().getName());
            String toGroup = getGroupName(event.getTo().getWorld().getName());
            
            if (!fromGroup.equals(toGroup)) {
                // Save current items to the OLD group before teleport happens
                saveInventory(event.getPlayer(), event.getFrom().getWorld().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Double check inventory loading on world change to catch any teleports that didn't trigger TeleportEvent properly
        // or cases where the world was changed via other means
        Player player = event.getPlayer();
        String currentWorld = player.getWorld().getName();
        String currentGroup = getGroupName(currentWorld);
        
        // We don't save here because the player is already in the new world (too late to save to old group)
        // TeleportEvent handles the save. This is just a safety load.
        loadInventory(player, currentWorld);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        trackLocation(player, player.getLocation());
        saveInventory(player, player.getWorld().getName());
        
        PlayerProfile profile = getProfile(player.getUniqueId());
        if (profile != null) {
            // Save profile to individual file
            File playerDir = new File(plugin.getDataFolder(), "players" + File.separator + player.getUniqueId());
            if (!playerDir.exists()) playerDir.mkdirs();
            
            File profileFile = new File(playerDir, "profile.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(profileFile);
            
            // We only save general data (like last_locations) here. 
            // Inventories are saved in their own files via saveInventory().
            config.set("last_locations", profile.getData().get("last_locations"));
            
            try {
                config.save(profileFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save profile for " + player.getName() + ": " + e.getMessage());
            }
        }
        
        profiles.remove(player.getUniqueId());
    }

    public void saveInventory(Player player, String worldName) {
        // If SpyInventories is enabled, let it handle inventory management completely
        if (Bukkit.getPluginManager().isPluginEnabled("SpyInventories")) {
            return;
        }

        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return; // Don't save spectator inventory

        World world = Bukkit.getWorld(worldName);
        String alias = world != null ? plugin.getWorldManager().getAliasForWorld(world) : worldName;
        String lowerAlias = alias.toLowerCase();

        // EXCLUSION CHECK - Now uses alias
        if (lowerAlias.contains("match_") || lowerAlias.contains("temp_")) {
            return;
        }

        PlayerProfile profile = getProfile(player.getUniqueId());
        if (profile == null) return;

        String group = getGroupName(alias);
        
        // Clone ItemStacks and state synchronously on the main thread to ensure async thread safety
        org.bukkit.inventory.ItemStack[] rawInv = player.getInventory().getContents();
        org.bukkit.inventory.ItemStack[] rawArmor = player.getInventory().getArmorContents();
        org.bukkit.inventory.ItemStack[] rawEnder = player.getEnderChest().getContents();

        org.bukkit.inventory.ItemStack[] clonedInv = new org.bukkit.inventory.ItemStack[rawInv.length];
        for (int i = 0; i < rawInv.length; i++) {
            clonedInv[i] = rawInv[i] != null ? rawInv[i].clone() : null;
        }

        org.bukkit.inventory.ItemStack[] clonedArmor = new org.bukkit.inventory.ItemStack[rawArmor.length];
        for (int i = 0; i < rawArmor.length; i++) {
            clonedArmor[i] = rawArmor[i] != null ? rawArmor[i].clone() : null;
        }

        org.bukkit.inventory.ItemStack[] clonedEnder = new org.bukkit.inventory.ItemStack[rawEnder.length];
        for (int i = 0; i < rawEnder.length; i++) {
            clonedEnder[i] = rawEnder[i] != null ? rawEnder[i].clone() : null;
        }

        double health = Math.max(1.0, player.getHealth());
        int food = player.getFoodLevel();
        int level = player.getLevel();
        float exp = player.getExp();
        String gameModeName = player.getGameMode().name();
        java.util.List<PotionEffect> effects = new java.util.ArrayList<>(player.getActivePotionEffects());

        // Update in-memory profile
        profile.setInventory(group, clonedInv);
        profile.setArmor(group, clonedArmor);
        profile.setEnderChest(group, clonedEnder);
        profile.setStats(group, health, food, level, exp);
        
        // Save to group-specific file
        File playerDir = new File(plugin.getDataFolder(), "players" + File.separator + player.getUniqueId());
        if (!playerDir.exists()) playerDir.mkdirs();
        
        String fileName = group.replace("/", "_") + ".yml";
        File groupFile = new File(playerDir, fileName);
        FileConfiguration config = YamlConfiguration.loadConfiguration(groupFile);
        
        config.set("inventory", clonedInv);
        config.set("armor", clonedArmor);
        config.set("enderchest", clonedEnder);
        config.set("health", health);
        config.set("food", food);
        config.set("level", level);
        config.set("exp", exp);
        config.set("gamemode", gameModeName);
        config.set("potion_effects", effects);
        
        try {
            // Save asynchronously to prevent server thread blocking
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    config.save(groupFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save inventory for " + player.getName() + " in group " + group + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            // Fallback to synchronous if scheduler is not available (e.g. shutdown)
            try {
                config.save(groupFile);
            } catch (IOException ex) {
                plugin.getLogger().severe("Could not save inventory for " + player.getName() + " in group " + group + ": " + ex.getMessage());
            }
        }
    }

    public void loadInventory(Player player, String worldName) {
        // If SpyInventories is enabled, let it handle inventory management completely
        if (Bukkit.getPluginManager().isPluginEnabled("SpyInventories")) {
            return;
        }

        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return; // Don't override spectator inventory

        World world = Bukkit.getWorld(worldName);
        String alias = world != null ? plugin.getWorldManager().getAliasForWorld(world) : worldName;
        String lowerAlias = alias.toLowerCase();

        // EXCLUSION CHECK - Now uses alias
        if (lowerAlias.contains("match_") || lowerAlias.contains("temp_")) {
            return;
        }

        PlayerProfile profile = getProfile(player.getUniqueId());
        if (profile == null) return;

        String group = getGroupName(alias);
        
        // Load from group-specific file
        File playerDir = new File(plugin.getDataFolder(), "players" + File.separator + player.getUniqueId());
        String fileName = group.replace("/", "_") + ".yml";
        File groupFile = new File(playerDir, fileName);
        
        if (!groupFile.exists()) {
            // No saved data for this group. 
            // If SpyInventories is present, we let it handle the fallback/default state
            // to avoid conflicting clears.
            if (Bukkit.getPluginManager().isPluginEnabled("SpyInventories")) {
                plugin.getLogger().info("No SpyCore inventory found for " + player.getName() + " in group " + group + ". Deferring to SpyInventories.");
                return;
            }

            // No saved data for this group, reset to defaults
            player.getInventory().clear();
            player.getEnderChest().clear();
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(maxHealth);
            player.setFoodLevel(20);
            player.setLevel(0);
            player.setExp(0);
            plugin.getLogger().info("No inventory found for " + player.getName() + " in group " + group + ". Using defaults.");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(groupFile);
        
        // Use lists if stored as such, otherwise try casting
        org.bukkit.inventory.ItemStack[] contents = null;
        Object invObj = config.get("inventory");
        if (invObj instanceof java.util.List) {
            contents = ((java.util.List<org.bukkit.inventory.ItemStack>) invObj).toArray(new org.bukkit.inventory.ItemStack[0]);
        } else if (invObj != null) {
            contents = (org.bukkit.inventory.ItemStack[]) invObj;
        }

        org.bukkit.inventory.ItemStack[] armor = null;
        Object armorObj = config.get("armor");
        if (armorObj instanceof java.util.List) {
            armor = ((java.util.List<org.bukkit.inventory.ItemStack>) armorObj).toArray(new org.bukkit.inventory.ItemStack[0]);
        } else if (armorObj != null) {
            armor = (org.bukkit.inventory.ItemStack[]) armorObj;
        }

        org.bukkit.inventory.ItemStack[] ender = null;
        Object enderObj = config.get("enderchest");
        if (enderObj instanceof java.util.List) {
            ender = ((java.util.List<org.bukkit.inventory.ItemStack>) enderObj).toArray(new org.bukkit.inventory.ItemStack[0]);
        } else if (enderObj != null) {
            ender = (org.bukkit.inventory.ItemStack[]) enderObj;
        }

        player.getInventory().clear();
        if (contents != null) player.getInventory().setContents(contents);
        if (armor != null) player.getInventory().setArmorContents(armor);
        
        if (ender != null) {
            player.getEnderChest().setContents(ender);
        } else {
            player.getEnderChest().clear();
        }

        double health = ((Number) config.get("health", 20.0)).doubleValue();
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, Math.max(1.0, health))); // Ensure they don't die immediately on load and don't exceed max health
        player.setFoodLevel(((Number) config.get("food", 20)).intValue());
        player.setLevel(((Number) config.get("level", 0)).intValue());
        player.setExp(((Number) config.get("exp", 0.0f)).floatValue());

        // Load GameMode
        String gmName = config.getString("gamemode");
        if (gmName != null) {
            try {
                player.setGameMode(GameMode.valueOf(gmName));
            } catch (IllegalArgumentException ignored) {}
        }

        // Load Potion Effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        Object effectsObj = config.get("potion_effects");
        if (effectsObj instanceof Collection) {
            player.addPotionEffects((Collection<PotionEffect>) effectsObj);
        }
        
        plugin.getLogger().info("Loaded inventory for " + player.getName() + " in group " + group + " from " + fileName);
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
        
        // Skip tracking if they are in the lobby or a world that isn't part of a group we care about
        if (alias.equalsIgnoreCase("lobby")) return;

        String baseName = getGroupName(alias);

        PlayerProfile profile = getProfile(player.getUniqueId());
        if (profile != null) {
            profile.setLastLocation(baseName, location);
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            trackLocation(player, player.getLocation());
            saveInventory(player, player.getWorld().getName());

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
