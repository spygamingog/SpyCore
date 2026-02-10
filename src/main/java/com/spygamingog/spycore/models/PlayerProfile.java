package com.spygamingog.spycore.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PlayerProfile {
    private final UUID uuid;
    private final String name;
    private final Map<String, Object> data = new HashMap<>();

    public void setData(String key, Object value) {
        data.put(key, value);
    }

    public void setLastLocation(String worldSet, Location location) {
        Map<String, Object> locations = (Map<String, Object>) data.computeIfAbsent("last_locations", k -> new HashMap<String, Object>());
        if (location == null) {
            locations.remove(worldSet);
            return;
        }
        
        // Store as map to avoid "unknown world" errors during YAML loading
        Map<String, Object> locMap = new HashMap<>();
        locMap.put("world", location.getWorld().getName());
        locMap.put("x", location.getX());
        locMap.put("y", location.getY());
        locMap.put("z", location.getZ());
        locMap.put("yaw", (double) location.getYaw());
        locMap.put("pitch", (double) location.getPitch());
        locations.put(worldSet, locMap);
    }

    public Location getLastLocation(String worldSet) {
        Map<String, Object> locations = (Map<String, Object>) data.get("last_locations");
        if (locations == null) return null;
        
        Object val = locations.get(worldSet);
        if (val == null) return null;
        
        if (val instanceof Location) {
            return (Location) val;
        }
        
        if (val instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) val;
            String worldName = (String) map.get("world");
            if (worldName == null) return null;
            
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) return null;
            
            return new Location(
                world,
                (double) map.get("x"),
                (double) map.get("y"),
                (double) map.get("z"),
                ((Double) map.getOrDefault("yaw", 0.0)).floatValue(),
                ((Double) map.getOrDefault("pitch", 0.0)).floatValue()
            );
        }
        
        return null;
    }

    public void setInventory(String worldSet, org.bukkit.inventory.ItemStack[] contents) {
        Map<String, Object> inventories = (Map<String, Object>) data.computeIfAbsent("inventories", k -> new HashMap<String, Object>());
        inventories.put(worldSet, contents);
    }

    public org.bukkit.inventory.ItemStack[] getInventory(String worldSet) {
        Map<String, Object> inventories = (Map<String, Object>) data.get("inventories");
        if (inventories == null) return null;
        Object val = inventories.get(worldSet);
        if (val instanceof java.util.List) {
            return ((java.util.List<org.bukkit.inventory.ItemStack>) val).toArray(new org.bukkit.inventory.ItemStack[0]);
        }
        return (org.bukkit.inventory.ItemStack[]) val;
    }

    public void setArmor(String worldSet, org.bukkit.inventory.ItemStack[] contents) {
        Map<String, Object> armor = (Map<String, Object>) data.computeIfAbsent("armor", k -> new HashMap<String, Object>());
        armor.put(worldSet, contents);
    }

    public org.bukkit.inventory.ItemStack[] getArmor(String worldSet) {
        Map<String, Object> armor = (Map<String, Object>) data.get("armor");
        if (armor == null) return null;
        Object val = armor.get(worldSet);
        if (val instanceof java.util.List) {
            return ((java.util.List<org.bukkit.inventory.ItemStack>) val).toArray(new org.bukkit.inventory.ItemStack[0]);
        }
        return (org.bukkit.inventory.ItemStack[]) val;
    }

    public void setEnderChest(String worldSet, org.bukkit.inventory.ItemStack[] contents) {
        Map<String, Object> enderChests = (Map<String, Object>) data.computeIfAbsent("ender_chests", k -> new HashMap<String, Object>());
        enderChests.put(worldSet, contents);
    }

    public org.bukkit.inventory.ItemStack[] getEnderChest(String worldSet) {
        Map<String, Object> enderChests = (Map<String, Object>) data.get("ender_chests");
        if (enderChests == null) return null;
        Object val = enderChests.get(worldSet);
        if (val instanceof java.util.List) {
            return ((java.util.List<org.bukkit.inventory.ItemStack>) val).toArray(new org.bukkit.inventory.ItemStack[0]);
        }
        return (org.bukkit.inventory.ItemStack[]) val;
    }

    public void setStats(String worldSet, double health, int food, int level, float exp) {
        Map<String, Object> stats = (Map<String, Object>) data.computeIfAbsent("stats", k -> new HashMap<String, Object>());
        Map<String, Object> worldStats = new HashMap<>();
        worldStats.put("health", health);
        worldStats.put("food", food);
        worldStats.put("level", level);
        worldStats.put("exp", exp);
        stats.put(worldSet, worldStats);
    }

    public Map<String, Object> getStats(String worldSet) {
        Map<String, Object> stats = (Map<String, Object>) data.get("stats");
        return stats != null ? (Map<String, Object>) stats.get(worldSet) : null;
    }

    public Object getData(String key) {
        return data.get(key);
    }
    
    public <T> T getData(String key, Class<T> type) {
        Object val = data.get(key);
        if (type.isInstance(val)) {
            return type.cast(val);
        }
        return null;
    }
}
