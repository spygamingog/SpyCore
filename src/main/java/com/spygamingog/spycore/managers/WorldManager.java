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

    // Linked worlds share chat and tablist (e.g., Overworld + Nether + End)
    private final Map<String, Set<String>> linkedWorlds = new ConcurrentHashMap<>();

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
            String envStr = worldsConfig.getString("worlds." + key + ".environment");
            World.Environment environment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
            
            String fullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                    ? worldName 
                    : "spycore-worlds/" + container + "/" + worldName;
            worldAliases.put(worldName, fullPath);
            
            if (!hibernate) {
                World world = loadWorldInternal(container, worldName, generator, environment);
                if (world != null) {
                    applyWorldSettings(world, worldName);
                }
            }
        }
    }

    public boolean isWorldLoaded(String alias) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) return Bukkit.getWorld(alias) != null;
        return Bukkit.getWorld(fullPath) != null;
    }

    public void applyWorldSettings(World world, String alias) {
        String container = getContainerFromPath(world.getName());
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");

        // Keep Bedwars worlds in memory to ensure NPCs are always visible
        if (container.toLowerCase().contains("bedwars")) {
            world.setKeepSpawnInMemory(true);
        }
        
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
        return loadWorld(container, worldName, generator, null);
    }

    public World loadWorld(String container, String worldName, String generator, World.Environment environment) {
        String fullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? worldName 
                : "spycore-worlds/" + container + "/" + worldName;

        // Check if this alias is already taken by a DIFFERENT world
        String existingPath = worldAliases.get(worldName);
        if (existingPath != null && !existingPath.equalsIgnoreCase(fullPath)) {
            plugin.getLogger().warning("VFS: World '" + fullPath + "' loaded, but alias '" + worldName + "' is already taken by '" + existingPath + "'. Use full path to access this world.");
        }

        worldAliases.put(worldName, fullPath);
        
        // Use provided environment or try to get from config
        World.Environment finalEnvironment = environment;
        if (finalEnvironment == null) {
            String safeContainer = container == null ? "root" : container;
            String key = safeContainer.replace(".", "_") + "_" + worldName.replace(".", "_");
            String envStr = worldsConfig.getString("worlds." + key + ".environment");
            finalEnvironment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
        }

        World world = loadWorldInternal(container, worldName, generator, finalEnvironment);
        if (world != null) {
            saveWorldToConfig(container, worldName, false, generator, world.getEnvironment());
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
            fullPath = alias;
        }

        unloadWorld(alias, false);
        return deleteWorldFiles(alias);
    }

    /**
     * Delete only the files and configuration for a world.
     * MUST be called after the world is unloaded.
     * This method is safe to call from an asynchronous thread.
     */
    public boolean deleteWorldFiles(String alias) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) {
            fullPath = alias;
        }

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

    private World loadWorldInternal(String container, String worldName, String generator, World.Environment environment) {
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
        if (environment != null) {
            creator.environment(environment);
            plugin.getLogger().info("VFS: Loading world '" + fullPath + "' with environment: " + environment);
        } else {
            plugin.getLogger().info("VFS: Loading world '" + fullPath + "' with default environment (NORMAL)");
        }
        if (generator != null && !generator.isEmpty()) {
            creator.generator(generator);
        }
        
        world = Bukkit.createWorld(creator);
        
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            lastAccessTime.put(fullPath, System.currentTimeMillis());
            plugin.getLogger().info("VFS: Loaded " + fullPath + " (Alias: " + worldName + ") [Env: " + world.getEnvironment() + "]" + (generator != null ? " with generator: " + generator : ""));
            Bukkit.getPluginManager().callEvent(new SpyWorldLoadEvent(world, worldName));
        }
        
        return world;
    }

    public World getWorld(String alias) {
        return getWorld(alias, true);
    }

    public World getWorld(String alias, boolean loadIfAbsent) {
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
        if (world == null && loadIfAbsent) {
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
                String envStr = worldsConfig.getString("worlds." + key + ".environment");
                World.Environment environment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
                
                return loadWorldInternal(container, name, generator, environment);
            } else {
                String key = "root_" + fullPath.replace(".", "_");
                String generator = worldsConfig.getString("worlds." + key + ".generator");
                String envStr = worldsConfig.getString("worlds." + key + ".environment");
                World.Environment environment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
                return loadWorldInternal(null, fullPath, generator, environment);
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
        String fullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? worldName 
                : "spycore-worlds/" + container + "/" + worldName;

        // Check if this alias is already taken by a DIFFERENT world
        String existingPath = worldAliases.get(worldName);
        if (existingPath != null && !existingPath.equalsIgnoreCase(fullPath)) {
            plugin.getLogger().warning("VFS: World '" + fullPath + "' created, but alias '" + worldName + "' is already taken by '" + existingPath + "'. Use full path to access this world.");
        }
        
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
            world.setKeepSpawnInMemory(false);
            // Set safe spawn location
            Location safe = findSafeLocation(world);
            world.setSpawnLocation(safe);
            
            worldAliases.put(worldName, fullPath);
            lastAccessTime.put(fullPath, System.currentTimeMillis());
            saveWorldToConfig(container, worldName, false, generator, world.getEnvironment());
            plugin.getLogger().info("VFS: Created " + fullPath + " (Alias: " + worldName + ") with generator: " + (generator != null ? generator : "default"));
            Bukkit.getPluginManager().callEvent(new SpyWorldCreateEvent(world, worldName));
        }

        return world;
    }

    public World cloneWorld(String sourceAlias, String targetContainer, String targetName) {
        return cloneWorld(sourceAlias, targetContainer, targetName, null);
    }

    public World cloneWorld(String sourceAlias, String targetContainer, String targetName, String generator) {
        if (copyWorldFiles(sourceAlias, targetContainer, targetName)) {
            return loadWorld(targetContainer, targetName, generator);
        }
        return null;
    }

    /**
     * Copy world files from source to target.
     * This method is safe to call from an asynchronous thread.
     * @return True if successful.
     */
    public boolean copyWorldFiles(String sourceAlias, String targetContainer, String targetName) {
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
            return false;
        }

        File targetFolder = (targetContainer == null || targetContainer.isEmpty() || targetContainer.equalsIgnoreCase("root"))
                ? new File(plugin.getServer().getWorldContainer(), targetName)
                : new File(containersFolder, targetContainer.replace("/", File.separator) + File.separator + targetName);

        if (targetFolder.exists()) {
            plugin.getLogger().warning("VFS: Target world '" + targetName + "' already exists.");
            return false;
        }

        try {
            // Copy files
            FileUtils.copyDirectory(sourceFolder, targetFolder);
            
            // Remove session.lock and uid.dat
            new File(targetFolder, "session.lock").delete();
            new File(targetFolder, "uid.dat").delete();
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "VFS: Failed to copy world files from '" + sourceAlias + "' to '" + targetName + "'", e);
            return false;
        }
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
            String envStr = worldsConfig.getString("worlds." + oldKey + ".environment");
            World.Environment environment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
            worldsConfig.set("worlds." + oldKey, null);

            // Update aliases and config
            worldAliases.remove(alias);
            worldAliases.put(targetName, targetFullPath);
            lastAccessTime.remove(sourceFullPath);
            
            saveWorldToConfig(targetContainer, targetName, false, generator, environment);
            
            // Load in new location
            loadWorldInternal(targetContainer, targetName, generator, environment);
            
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
                String envStr = worldsConfig.getString("worlds." + oldKey + ".environment");
                World.Environment environment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
                worldsConfig.set("worlds." + oldKey, null);

                String relativeToSource = oldFullPath.replace(prefix, "");
                String newFullPath = "spycore-worlds/" + targetPath + "/" + relativeToSource;
                String newContainer = getContainerFromPath(newFullPath);
                
                worldAliases.put(alias, newFullPath);
                lastAccessTime.remove(oldFullPath);
                saveWorldToConfig(newContainer, alias, false, generator, environment);
                
                // Reload world in new location
                loadWorldInternal(newContainer, alias, generator, environment);
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
        
        // Try to find the alias from the map first
        for (Map.Entry<String, String> entry : worldAliases.entrySet()) {
            if (entry.getValue().equals(worldName)) {
                return entry.getKey();
            }
        }
        
        // Fallback to extraction if not in map
        if (worldName.startsWith("spycore-worlds/")) {
            String path = worldName.replace("spycore-worlds/", "");
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
            return path; // Return the name without spycore-worlds/ prefix
        }
        return worldName;
    }

    private void saveWorldToConfig(String container, String worldName, boolean hibernate, String generator, World.Environment environment) {
        String safeContainer = container == null ? "root" : container;
        String key = safeContainer.replace(".", "_") + "_" + worldName.replace(".", "_");
        worldsConfig.set("worlds." + key + ".container", container);
        worldsConfig.set("worlds." + key + ".name", worldName);
        worldsConfig.set("worlds." + key + ".hibernate", hibernate);
        if (generator != null && !generator.isEmpty()) {
            worldsConfig.set("worlds." + key + ".generator", generator);
        }
        if (environment != null) {
            worldsConfig.set("worlds." + key + ".environment", environment.name());
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

    public void linkWorlds(World world1, World world2) {
        if (world1 == null || world2 == null) return;
        String name1 = world1.getName();
        String name2 = world2.getName();
        
        linkedWorlds.computeIfAbsent(name1, k -> ConcurrentHashMap.newKeySet()).add(name2);
        linkedWorlds.computeIfAbsent(name2, k -> ConcurrentHashMap.newKeySet()).add(name1);
        
        // Ensure transitive linking (if A links to B and B links to C, then A links to C)
        Set<String> allLinked = new java.util.HashSet<>();
        allLinked.add(name1);
        allLinked.add(name2);
        allLinked.addAll(linkedWorlds.get(name1));
        allLinked.addAll(linkedWorlds.get(name2));
        
        for (String name : allLinked) {
            linkedWorlds.put(name, ConcurrentHashMap.newKeySet());
            linkedWorlds.get(name).addAll(allLinked);
            linkedWorlds.get(name).remove(name);
        }
    }

    public boolean areLinked(World world1, World world2) {
        if (world1 == null || world2 == null) return false;
        if (world1.equals(world2)) return true;
        
        Set<String> linked = linkedWorlds.get(world1.getName());
        return linked != null && linked.contains(world2.getName());
    }

    public void unlinkWorlds(World world) {
        if (world == null) return;
        String name = world.getName();
        Set<String> linked = linkedWorlds.remove(name);
        if (linked == null) return;

        for (String other : linked) {
            Set<String> otherLinked = linkedWorlds.get(other);
            if (otherLinked != null) {
                otherLinked.remove(name);
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
        int spawnX = spawn.getBlockX();
        int spawnZ = spawn.getBlockZ();
        
        // 1. For Overworld, try the highest block first (very fast)
        if (world.getEnvironment() == World.Environment.NORMAL) {
            int highestY = world.getHighestBlockYAt(spawnX, spawnZ);
            Block b = world.getBlockAt(spawnX, highestY + 1, spawnZ);
            if (isSafe(b)) return b.getLocation().add(0.5, 0, 0.5);
        }

        // 2. Search a smaller area (9x9 instead of 17x17) and optimize Y scan
        int radius = 4;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int currX = spawnX + x;
                int currZ = spawnZ + z;

                if (world.getEnvironment() == World.Environment.NETHER) {
                    // For Nether, scan downwards from just below the ceiling
                    for (int y = 120; y > 32; y--) {
                        Block b = world.getBlockAt(currX, y, currZ);
                        if (isSafe(b)) return b.getLocation().add(0.5, 0, 0.5);
                    }
                } else {
                    // For Overworld/others, check around the highest block
                    int surfaceY = world.getHighestBlockYAt(currX, currZ);
                    // Scan a small range around the surface
                    for (int y = surfaceY + 5; y > surfaceY - 10; y--) {
                        if (y < world.getMinHeight() + 1 || y > world.getMaxHeight() - 2) continue;
                        Block b = world.getBlockAt(currX, y, currZ);
                        if (isSafe(b)) return b.getLocation().add(0.5, 0, 0.5);
                    }
                }
            }
        }
        
        // Fallback for Overworld: highest block
        if (world.getEnvironment() == World.Environment.NORMAL) {
            int highestY = world.getHighestBlockYAt(spawnX, spawnZ);
            return new Location(world, spawnX + 0.5, highestY + 1, spawnZ + 0.5);
        }
        
        return spawn.clone().add(0.5, 0, 0.5);
    }

    private boolean isSafe(Block block) {
        Material foot = block.getType();
        Material head = block.getRelative(0, 1, 0).getType();
        Material ground = block.getRelative(0, -1, 0).getType();

        // Check if the space is clear (not suffocating)
        if (foot != Material.AIR && foot != Material.CAVE_AIR) return false;
        if (head != Material.AIR && head != Material.CAVE_AIR) return false;

        // Check if the ground is solid and safe to stand on
        if (!ground.isSolid()) return false;
        
        // Exclude harmful blocks
        switch (ground) {
            case LAVA:
            case WATER:
            case POWDER_SNOW:
            case MAGMA_BLOCK:
            case POINTED_DRIPSTONE:
            case CACTUS:
            case SWEET_BERRY_BUSH:
            case FIRE:
            case SOUL_FIRE:
            case WITHER_ROSE:
                return false;
            default:
                return true;
        }
    }
}
