package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MetadataManager {
    private final SpyCore plugin;
    private final File metadataFile;
    private FileConfiguration metadataConfig;
    private final Map<String, Map<String, String>> worldMetadata = new HashMap<>();

    public MetadataManager(SpyCore plugin) {
        this.plugin = plugin;
        this.metadataFile = new File(plugin.getDataFolder(), "metadata.yml");
        loadMetadata();
    }

    private void loadMetadata() {
        if (!metadataFile.exists()) {
            try {
                metadataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create metadata.yml", e);
            }
        }
        metadataConfig = YamlConfiguration.loadConfiguration(metadataFile);
        
        if (metadataConfig.getConfigurationSection("worlds") != null) {
            for (String worldPath : metadataConfig.getConfigurationSection("worlds").getKeys(false)) {
                Map<String, String> tags = new HashMap<>();
                for (String tag : metadataConfig.getConfigurationSection("worlds." + worldPath).getKeys(false)) {
                    tags.put(tag, metadataConfig.getString("worlds." + worldPath + "." + tag));
                }
                worldMetadata.put(worldPath.replace("__", "/"), tags); // Convert back from safe YAML keys
            }
        }
    }

    public void setTag(World world, String key, String value) {
        String worldPath = world.getName(); 
        worldMetadata.computeIfAbsent(worldPath, k -> new HashMap<>()).put(key, value);
        saveMetadata(worldPath, key, value);
    }

    public String getTag(World world, String key) {
        Map<String, String> tags = worldMetadata.get(world.getName());
        return tags != null ? tags.get(key) : null;
    }

    public Map<String, String> getTags(World world) {
        return worldMetadata.getOrDefault(world.getName(), new HashMap<>());
    }

    /**
     * Query Engine: Find all worlds in a specific container that match all provided tags.
     */
    public List<World> findWorlds(String container, Map<String, String> requiredTags) {
        return worldMetadata.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("containers/" + container + "/"))
                .filter(entry -> {
                    Map<String, String> worldTags = entry.getValue();
                    return requiredTags.entrySet().stream()
                            .allMatch(tag -> worldTags.containsKey(tag.getKey()) && worldTags.get(tag.getKey()).equalsIgnoreCase(tag.getValue()));
                })
                .map(entry -> plugin.getWorldManager().getWorld(entry.getKey().substring(entry.getKey().lastIndexOf("/") + 1)))
                .collect(Collectors.toList());
    }

    private void saveMetadata(String worldPath, String key, String value) {
        String safePath = worldPath.replace("/", "__").replace(".", "_");
        metadataConfig.set("worlds." + safePath + "." + key, value);
        try {
            metadataConfig.save(metadataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save metadata.yml", e);
        }
    }
}
