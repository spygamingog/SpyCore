package com.spygamingog.spycore.models;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProfile {
    private final UUID uuid;
    private final String name;
    private final Map<String, Object> data = new HashMap<>();

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getData() {
        return data;
    }

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
            
            double x = ((Number) map.getOrDefault("x", 0.0)).doubleValue();
            double y = ((Number) map.getOrDefault("y", 64.0)).doubleValue();
            double z = ((Number) map.getOrDefault("z", 0.0)).doubleValue();
            float yaw = ((Number) map.getOrDefault("yaw", 0.0)).floatValue();
            float pitch = ((Number) map.getOrDefault("pitch", 0.0)).floatValue();

            return new Location(world, x, y, z, yaw, pitch);
        }
        
        return null;
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
