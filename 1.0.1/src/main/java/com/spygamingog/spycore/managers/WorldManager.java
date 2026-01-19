package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.api.events.SpyWorldCreateEvent;
import com.spygamingog.spycore.api.events.SpyWorldLoadEvent;
import com.spygamingog.spycore.api.events.SpyWorldUnloadEvent;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WorldManager {
    private final SpyCore plugin;
    private final File containersFolder;
    private final File worldsConfigFile;
    private FileConfiguration worldsConfig;
    
    // Key: Alias (e.g., "DesertMap"), Value: Full Path (e.g., "containers/Bedwars/Solo/DesertMap")
    @Getter
    private final Map<String, String> worldAliases = new ConcurrentHashMap<>();
    
    // Key: Full Path, Value: Last access time
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    public WorldManager(SpyCore plugin) {
        this.plugin = plugin;
        this.containersFolder = new File(plugin.getServer().getWorldContainer(), "spycore-worlds");
        if (!containersFolder.exists()) {
            containersFolder.mkdirs();
        }
        this.worldsConfigFile = new File(plugin.getDataFolder(), "worlds.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!worldsConfigFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                worldsConfigFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create worlds.yml", e);
            }
        }
        worldsConfig = YamlConfiguration.loadConfiguration(worldsConfigFile);
    }

    public void loadWorlds() {
        if (worldsConfig.getConfigurationSection("worlds") == null) return;

        for (String key : worldsConfig.getConfigurationSection("worlds").getKeys(false)) {
            String container = worldsConfig.getString("worlds." + key + ".container");
            String worldName = worldsConfig.getString("worlds." + key + ".name");
            boolean hibernate = worldsConfig.getBoolean("worlds." + key + ".hibernate", false);
            String generator = worldsConfig.getString("worlds." + key + ".generator");
            
            String fullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                    ? worldName 
                    : "spycore-worlds/" + container + "/" + worldName;
            worldAliases.put(worldName, fullPath);
            
            if (!hibernate) {
                World world = loadWorldInternal(container, worldName, generator);
                if (world != null) {
                    applyWorldSettings(world, worldName);
                }
            }
        }
    }

    public void applyWorldSettings(World world, String alias) {
        String container = getContainerFromPath(world.getName());
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");
        
        if (worldsConfig.contains("worlds." + key + ".settings")) {
            String settingsPath = "worlds." + key + ".settings.";
            
            if (worldsConfig.contains(settingsPath + "timecycle")) {
                boolean timeCycle = worldsConfig.getBoolean(settingsPath + "timecycle");
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, timeCycle);
            }
            
            if (worldsConfig.contains(settingsPath + "weathercycle")) {
                boolean weatherCycle = worldsConfig.getBoolean(settingsPath + "weathercycle");
                world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, weatherCycle);
            }

            if (worldsConfig.contains(settingsPath + "difficulty")) {
                try {
                    org.bukkit.Difficulty difficulty = org.bukkit.Difficulty.valueOf(worldsConfig.getString(settingsPath + "difficulty").toUpperCase());
                    world.setDifficulty(difficulty);
                } catch (IllegalArgumentException | NullPointerException ignored) {}
            }
        }
    }

    public void setWorldSetting(String alias, String setting, boolean value) {
        setWorldSetting(alias, setting, (Object) value);
    }

    public void setWorldSetting(String alias, String setting, Object value) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) return;

        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");
        
        worldsConfig.set("worlds." + key + ".settings." + setting.toLowerCase(), value);
        try {
            worldsConfig.save(worldsConfigFile);
            
            World world = Bukkit.getWorld(fullPath);
            if (world != null) {
                if (setting.equalsIgnoreCase("timecycle")) {
                    world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, (Boolean) value);
                } else if (setting.equalsIgnoreCase("weathercycle")) {
                    world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, (Boolean) value);
                } else if (setting.equalsIgnoreCase("difficulty")) {
                    try {
                        org.bukkit.Difficulty difficulty = org.bukkit.Difficulty.valueOf(value.toString().toUpperCase());
                        world.setDifficulty(difficulty);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save world setting: " + setting, e);
        }
    }

    public Object getWorldSetting(String alias, String setting, Object defaultValue) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) return defaultValue;

        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");
        
        return worldsConfig.get("worlds." + key + ".settings." + setting.toLowerCase(), defaultValue);
    }

    public World loadWorld(String container, String worldName) {
        return loadWorld(container, worldName, null);
    }

    public World loadWorld(String container, String worldName, String generator) {
        // Check if name is already used as an alias for another path
        String existingPath = worldAliases.get(worldName);
        String newFullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? worldName 
                : "spycore-worlds/" + container + "/" + worldName;
        
        if (existingPath != null && !existingPath.equals(newFullPath)) {
            plugin.getLogger().warning("VFS: Cannot load world '" + worldName + "' from " + newFullPath + " - alias already used for " + existingPath);
            return null;
        }

        worldAliases.put(worldName, newFullPath);
        World world = loadWorldInternal(container, worldName, generator);
        if (world != null) {
            saveWorldToConfig(container, worldName, false, generator);
        }
        return world;
    }

    public boolean removeWorld(String alias) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) return false;

        unloadWorld(alias, true);
        worldAliases.remove(alias);
        lastAccessTime.remove(fullPath);

        // Remove from config
        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");
        worldsConfig.set("worlds." + key, null);
        try {
            worldsConfig.save(worldsConfigFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save worlds.yml after world removal", e);
            return false;
        }
    }

    public boolean deleteWorld(String alias) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) {
            // Try to find if it's a root world not in aliases
            fullPath = alias;
        }

        unloadWorld(alias, true);
        worldAliases.remove(alias);
        lastAccessTime.remove(fullPath);

        // Remove from config
        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");
        worldsConfig.set("worlds." + key, null);
        try {
            worldsConfig.save(worldsConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save worlds.yml after world deletion", e);
        }

        // Physically delete folder
        File worldFolder = fullPath.startsWith("spycore-worlds/")
                ? new File(containersFolder, fullPath.replace("spycore-worlds/", "").replace("/", File.separator))
                : new File(plugin.getServer().getWorldContainer(), fullPath);

        if (worldFolder.exists()) {
            try {
                FileUtils.deleteDirectory(worldFolder);
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not delete world folder: " + fullPath, e);
                return false;
            }
        }
        return true;
    }

    private String getContainerFromPath(String fullPath) {
        if (!fullPath.startsWith("spycore-worlds/")) return "root";
        String relative = fullPath.replace("spycore-worlds/", "");
        int lastSlash = relative.lastIndexOf("/");
        if (lastSlash == -1) return "root";
        return relative.substring(0, lastSlash);
    }

    private World loadWorldInternal(String container, String worldName, String generator) {
        String fullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? worldName 
                : "spycore-worlds/" + container + "/" + worldName;
        World world = Bukkit.getWorld(fullPath);
        if (world != null) return world;

        File worldFolder = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? new File(plugin.getServer().getWorldContainer(), worldName) 
                : new File(containersFolder, container.replace("/", File.separator) + File.separator + worldName);
        
        if (!worldFolder.exists()) {
            return null;
        }

        WorldCreator creator = new WorldCreator(fullPath);
        if (generator != null && !generator.isEmpty()) {
            creator.generator(generator);
        }
        
        world = Bukkit.createWorld(creator);
        
        if (world != null) {
            lastAccessTime.put(fullPath, System.currentTimeMillis());
            plugin.getLogger().info("VFS: Loaded " + fullPath + " (Alias: " + worldName + ")" + (generator != null ? " with generator: " + generator : ""));
            Bukkit.getPluginManager().callEvent(new SpyWorldLoadEvent(world, worldName));
        }
        
        return world;
    }

    public World getWorld(String alias) {
        String fullPath = worldAliases.get(alias);
        
        // If not found by alias, check if the alias itself is a full path
        if (fullPath == null) {
             if (alias.startsWith("spycore-worlds/")) {
                 fullPath = alias;
             } else if (alias.contains("/")) {
                 // Might be a container path without the prefix
                 fullPath = "spycore-worlds/" + alias;
             } else {
                 // Check if it's a root world (might be hibernating)
                 String key = "root_" + alias.replace(".", "_");
                 if (worldsConfig.contains("worlds." + key)) {
                     fullPath = alias;
                 } else {
                     return Bukkit.getWorld(alias);
                 }
             }
         }

        World world = Bukkit.getWorld(fullPath);
        if (world == null) {
            // World is hibernating, wake it up
            if (fullPath.startsWith("spycore-worlds/")) {
                String relativePath = fullPath.replace("spycore-worlds/", "");
                int lastSlash = relativePath.lastIndexOf("/");
                String container = null;
                String name = relativePath;
                
                if (lastSlash != -1) {
                    container = relativePath.substring(0, lastSlash);
                    name = relativePath.substring(lastSlash + 1);
                }
                
                String safeContainer = container == null ? "root" : container;
                String key = safeContainer.replace(".", "_") + "_" + name.replace(".", "_");
                String generator = worldsConfig.getString("worlds." + key + ".generator");
                
                return loadWorldInternal(container, name, generator);
            } else {
                String key = "root_" + fullPath.replace(".", "_");
                String generator = worldsConfig.getString("worlds." + key + ".generator");
                return loadWorldInternal(null, fullPath, generator);
            }
        }
        
        if (world != null) {
            lastAccessTime.put(fullPath, System.currentTimeMillis());
        }
        return world;
    }

    public World createWorld(String container, String worldName, World.Environment environment) {
        return createWorld(container, worldName, environment, null);
    }

    public World createWorld(String container, String worldName, World.Environment environment, String generator) {
        // Check for world name uniqueness across ALL containers and root
        if (worldAliases.containsKey(worldName)) {
            plugin.getLogger().warning("VFS: Cannot create world '" + worldName + "' - name already exists in another container.");
            return null;
        }

        String fullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? worldName 
                : "spycore-worlds/" + container + "/" + worldName;
        
        // Ensure nested container directories exist
        if (container != null && !container.isEmpty() && !container.equalsIgnoreCase("root")) {
            File folder = new File(containersFolder, container.replace("/", File.separator));
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }

        WorldCreator creator = new WorldCreator(fullPath);
        creator.environment(environment);
        if (generator != null && !generator.isEmpty()) {
            creator.generator(generator);
        }
        
        World world = Bukkit.createWorld(creator);

        if (world != null) {
            // Set safe spawn location
            Location safe = findSafeLocation(world);
            world.setSpawnLocation(safe);
            
            worldAliases.put(worldName, fullPath);
            lastAccessTime.put(fullPath, System.currentTimeMillis());
            saveWorldToConfig(container, worldName, false, generator);
            plugin.getLogger().info("VFS: Created " + fullPath + " (Alias: " + worldName + ") with generator: " + (generator != null ? generator : "default"));
            Bukkit.getPluginManager().callEvent(new SpyWorldCreateEvent(world, worldName));
        }

        return world;
    }

    public World cloneWorld(String sourceAlias, String targetContainer, String targetName) {
        return cloneWorld(sourceAlias, targetContainer, targetName, null);
    }

    public World cloneWorld(String sourceAlias, String targetContainer, String targetName, String generator) {
        // Find source world path
        String sourceFullPath = worldAliases.get(sourceAlias);
        File sourceFolder;
        
        if (sourceFullPath != null) {
            sourceFolder = new File(plugin.getServer().getWorldContainer(), sourceFullPath.replace("/", File.separator));
        } else {
            // Check if it's a root world not yet in our system
            sourceFolder = new File(plugin.getServer().getWorldContainer(), sourceAlias);
            if (!sourceFolder.exists()) {
                // Check if it's in our containers folder directly
                sourceFolder = new File(containersFolder, sourceAlias.replace("/", File.separator));
            }
        }

        if (!sourceFolder.exists()) {
            plugin.getLogger().warning("VFS: Source world '" + sourceAlias + "' not found for cloning.");
            return null;
        }

        // Target path
        String targetFullPath = (targetContainer == null || targetContainer.isEmpty() || targetContainer.equalsIgnoreCase("root")) 
                ? targetName 
                : "spycore-worlds/" + targetContainer + "/" + targetName;

        File targetFolder = (targetContainer == null || targetContainer.isEmpty() || targetContainer.equalsIgnoreCase("root"))
                ? new File(plugin.getServer().getWorldContainer(), targetName)
                : new File(containersFolder, targetContainer.replace("/", File.separator) + File.separator + targetName);

        if (targetFolder.exists()) {
            plugin.getLogger().warning("VFS: Target world '" + targetName + "' already exists.");
            return null;
        }

        try {
            // Copy files
            FileUtils.copyDirectory(sourceFolder, targetFolder);
            
            // Remove session.lock and uid.dat
            new File(targetFolder, "session.lock").delete();
            new File(targetFolder, "uid.dat").delete();

            // Load the cloned world
            WorldCreator creator = new WorldCreator(targetFullPath);
            if (generator != null && !generator.isEmpty()) {
                creator.generator(generator);
            }
            
            World world = Bukkit.createWorld(creator);
            if (world != null) {
                worldAliases.put(targetName, targetFullPath);
                lastAccessTime.put(targetFullPath, System.currentTimeMillis());
                saveWorldToConfig(targetContainer, targetName, false, generator);
                plugin.getLogger().info("VFS: Cloned '" + sourceAlias + "' to '" + targetFullPath + "' (Alias: " + targetName + ")");
                Bukkit.getPluginManager().callEvent(new SpyWorldCreateEvent(world, targetName));
                return world;
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "VFS: Failed to clone world '" + sourceAlias + "' to '" + targetName + "'", e);
        }

        return null;
    }

    public boolean unloadWorld(String alias, boolean save) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) fullPath = alias;
        
        World world = Bukkit.getWorld(fullPath);
        if (world == null) return false;

        Bukkit.getPluginManager().callEvent(new SpyWorldUnloadEvent(world, alias));
        return Bukkit.unloadWorld(world, save);
    }

    public boolean createContainer(String name) {
        File folder = new File(containersFolder, name.replace("/", File.separator));
        if (folder.exists()) return false;
        return folder.mkdirs();
    }

    public boolean removeContainer(String containerName) {
        // First, unregister and unload everything in it
        deleteContainer(containerName);
        
        // Then, physically delete the folder
        File folder = new File(containersFolder, containerName.replace("/", File.separator));
        if (folder.exists()) {
            try {
                FileUtils.deleteDirectory(folder);
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not delete container folder: " + containerName, e);
                return false;
            }
        }
        return false;
    }

    public boolean moveWorld(String alias, String targetContainer, String targetName) {
        String sourceFullPath = worldAliases.get(alias);
        if (sourceFullPath == null) return false;

        File sourceFolder = sourceFullPath.startsWith("spycore-worlds/")
                ? new File(containersFolder, sourceFullPath.replace("spycore-worlds/", "").replace("/", File.separator))
                : new File(plugin.getServer().getWorldContainer(), sourceFullPath);

        if (!sourceFolder.exists()) return false;

        String targetFullPath = (targetContainer == null || targetContainer.isEmpty() || targetContainer.equalsIgnoreCase("root"))
                ? targetName
                : "spycore-worlds/" + targetContainer + "/" + targetName;

        File targetFolder = (targetContainer == null || targetContainer.isEmpty() || targetContainer.equalsIgnoreCase("root"))
                ? new File(plugin.getServer().getWorldContainer(), targetName)
                : new File(containersFolder, targetContainer.replace("/", File.separator) + File.separator + targetName);

        if (targetFolder.exists()) return false;

        // Unload source world
        unloadWorld(alias, true);

        try {
            // Move directory
            FileUtils.moveDirectory(sourceFolder, targetFolder);
            
            // Remove from old config location
            String oldContainer = getContainerFromPath(sourceFullPath);
            String oldKey = oldContainer.replace(".", "_") + "_" + alias.replace(".", "_");
            String generator = worldsConfig.getString("worlds." + oldKey + ".generator");
            worldsConfig.set("worlds." + oldKey, null);

            // Update aliases and config
            worldAliases.remove(alias);
            worldAliases.put(targetName, targetFullPath);
            lastAccessTime.remove(sourceFullPath);
            
            saveWorldToConfig(targetContainer, targetName, false, generator);
            
            // Load in new location
            loadWorldInternal(targetContainer, targetName, generator);
            
            plugin.getLogger().info("VFS: Moved world '" + alias + "' to '" + targetFullPath + "'");
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "VFS: Failed to move world '" + alias + "'", e);
            return false;
        }
    }

    public boolean moveContainer(String sourcePath, String targetParentPath) {
        File sourceFolder = new File(containersFolder, sourcePath.replace("/", File.separator));
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) return false;

        String containerName = sourceFolder.getName();
        String targetPath = (targetParentPath == null || targetParentPath.isEmpty() || targetParentPath.equalsIgnoreCase("root"))
                ? containerName
                : targetParentPath + "/" + containerName;

        File targetFolder = (targetParentPath == null || targetParentPath.isEmpty() || targetParentPath.equalsIgnoreCase("root"))
                ? new File(containersFolder, containerName)
                : new File(containersFolder, targetPath.replace("/", File.separator));

        if (targetFolder.exists()) return false;

        // Find all worlds in this container and its sub-containers
        String prefix = "spycore-worlds/" + sourcePath + "/";
        Map<String, String> affectedWorlds = worldAliases.entrySet().stream()
                .filter(entry -> entry.getValue().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Unload all affected worlds
        for (String alias : affectedWorlds.keySet()) {
            unloadWorld(alias, true);
        }

        try {
            // Move the whole container directory
            FileUtils.moveDirectory(sourceFolder, targetFolder);

            // Update config and aliases for each world
            for (Map.Entry<String, String> entry : affectedWorlds.entrySet()) {
                String alias = entry.getKey();
                String oldFullPath = entry.getValue();
                
                String oldContainer = getContainerFromPath(oldFullPath);
                String oldKey = oldContainer.replace(".", "_") + "_" + alias.replace(".", "_");
                String generator = worldsConfig.getString("worlds." + oldKey + ".generator");
                worldsConfig.set("worlds." + oldKey, null);

                String relativeToSource = oldFullPath.replace(prefix, "");
                String newFullPath = "spycore-worlds/" + targetPath + "/" + relativeToSource;
                String newContainer = getContainerFromPath(newFullPath);
                
                worldAliases.put(alias, newFullPath);
                lastAccessTime.remove(oldFullPath);
                saveWorldToConfig(newContainer, alias, false, generator);
                
                // Reload world in new location
                loadWorldInternal(newContainer, alias, generator);
            }
            
            worldsConfig.save(worldsConfigFile);
            plugin.getLogger().info("VFS: Moved container '" + sourcePath + "' to '" + targetPath + "'");
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "VFS: Failed to move container '" + sourcePath + "'", e);
            return false;
        }
    }

    public void deleteContainer(String containerName) {
        Set<String> worldsInContainer = worldAliases.entrySet().stream()
                .filter(entry -> entry.getValue().startsWith("spycore-worlds/" + containerName + "/"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (String alias : worldsInContainer) {
            String fullPath = worldAliases.get(alias);
            unloadWorld(alias, true);
            worldAliases.remove(alias);
            lastAccessTime.remove(fullPath);
            // Remove from config
            String key = containerName.replace(".", "_") + "_" + alias.replace(".", "_");
            worldsConfig.set("worlds." + key, null);
        }
        
        try {
            worldsConfig.save(worldsConfigFile);
            plugin.getLogger().info("VFS: Container '" + containerName + "' unregistered and all worlds unloaded.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save worlds.yml after container deletion", e);
        }
    }

    public String getContainerForWorld(World world) {
        String worldName = world.getName();
        if (worldName.startsWith("spycore-worlds/")) {
            String path = worldName.replace("spycore-worlds/", "");
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash != -1) {
                return path.substring(0, lastSlash);
            }
        }
        return "root";
    }

    public String getAliasForWorld(World world) {
        String worldName = world.getName();
        if (worldName.startsWith("spycore-worlds/")) {
            String path = worldName.replace("spycore-worlds/", "");
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
        }
        return worldName;
    }

    private void saveWorldToConfig(String container, String worldName, boolean hibernate, String generator) {
        String safeContainer = container == null ? "root" : container;
        String key = safeContainer.replace(".", "_") + "_" + worldName.replace(".", "_");
        worldsConfig.set("worlds." + key + ".container", container);
        worldsConfig.set("worlds." + key + ".name", worldName);
        worldsConfig.set("worlds." + key + ".hibernate", hibernate);
        if (generator != null && !generator.isEmpty()) {
            worldsConfig.set("worlds." + key + ".generator", generator);
        }
        try {
            worldsConfig.save(worldsConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save worlds.yml", e);
        }
    }

    public void checkHibernation() {
        long now = System.currentTimeMillis();
        long threshold = 10 * 60 * 1000; // 10 minutes

        for (Map.Entry<String, String> entry : worldAliases.entrySet()) {
            String fullPath = entry.getValue();
            World world = Bukkit.getWorld(fullPath);
            
            if (world != null && world.getPlayers().isEmpty()) {
                Long lastAccess = lastAccessTime.get(fullPath);
                if (lastAccess != null && (now - lastAccess) > threshold) {
                    unloadWorld(entry.getKey(), true);
                    plugin.getLogger().info("VFS: Hibernating world " + fullPath + " due to inactivity.");
                }
            }
        }
    }

    public void shutdown() {
        // Unload all managed worlds
        for (String alias : worldAliases.keySet()) {
            unloadWorld(alias, true);
        }
    }

    public Location findSafeLocation(World world) {
        // The End spawn is already fine by default
        if (world.getEnvironment() == World.Environment.THE_END) {
            return world.getSpawnLocation().add(0.5, 0, 0.5);
        }

        Location spawn = world.getSpawnLocation();
        
        // Determine search start height
        int startY = world.getMaxHeight() - 2;
        if (world.getEnvironment() == World.Environment.NETHER) {
            startY = 125; // Stay below the bedrock roof (usually at 127)
        }

        // Search in a 16x16 area around spawn (slightly wider for better variety)
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                // Scan downwards from the determined top
                for (int y = startY; y > world.getMinHeight(); y--) {
                    Block b = world.getBlockAt(spawn.getBlockX() + x, y, spawn.getBlockZ() + z);
                    if (isSafe(b)) {
                        return b.getLocation().add(0.5, 0, 0.5);
                    }
                }
            }
        }
        
        // Fallback for Overworld: highest block
        if (world.getEnvironment() == World.Environment.NORMAL) {
            int highestY = world.getHighestBlockYAt(spawn);
            return new Location(world, spawn.getX(), highestY + 1, spawn.getZ()).add(0.5, 0, 0.5);
        }
        
        return spawn.add(0.5, 0, 0.5);
    }

    private boolean isSafe(Block block) {
        Material foot = block.getType();
        Material head = block.getRelative(0, 1, 0).getType();
        Material ground = block.getRelative(0, -1, 0).getType();

        return (foot == Material.AIR || foot == Material.CAVE_AIR) && 
               (head == Material.AIR || head == Material.CAVE_AIR) && 
               ground.isSolid();
    }
}
