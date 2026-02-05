package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.api.events.SpyWorldCreateEvent;
import com.spygamingog.spycore.api.events.SpyWorldLoadEvent;
import com.spygamingog.spycore.api.events.SpyWorldUnloadEvent;
import com.spygamingog.spycore.generators.LazyTemplateGenerator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    
    private final Object configLock = new Object();
    
    // Key: Alias (e.g., "DesertMap"), Value: Full Path (e.g., "containers/Bedwars/Solo/DesertMap")
    @Getter
    private final Map<String, String> worldAliases = new ConcurrentHashMap<>();
    
    // Key: Full Path, Value: Last access time
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    // Linked worlds share chat and tablist (e.g., Overworld + Nether + End)
    private final Map<String, Set<String>> linkedWorlds = new ConcurrentHashMap<>();

    // Worlds that should never hibernate
    @Getter
    private final Set<String> hibernationWhitelist = ConcurrentHashMap.newKeySet();

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
        synchronized (configLock) {
            if (!worldsConfigFile.exists()) {
                try {
                    plugin.getDataFolder().mkdirs();
                    worldsConfigFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create worlds.yml", e);
                }
            }
            worldsConfig = YamlConfiguration.loadConfiguration(worldsConfigFile);
            
            // Load hibernation whitelist
            List<String> whitelist = worldsConfig.getStringList("hibernation-whitelist");
            hibernationWhitelist.addAll(whitelist);
        }
    }

    private void saveWhitelist() {
        synchronized (configLock) {
            worldsConfig.set("hibernation-whitelist", new ArrayList<>(hibernationWhitelist));
            try {
                worldsConfig.save(worldsConfigFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save hibernation whitelist", e);
            }
        }
    }

    public void addWorldToWhitelist(String alias) {
        hibernationWhitelist.add(alias);
        saveWhitelist();
    }

    public void removeWorldFromWhitelist(String alias) {
        hibernationWhitelist.remove(alias);
        saveWhitelist();
    }

    public boolean isWorldWhitelisted(String alias) {
        return hibernationWhitelist.contains(alias);
    }

    public World wakeWorld(String alias) {
        return getWorld(alias, true);
    }

    public void loadWorlds() {
        synchronized (configLock) {
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
                    World world = loadWorldInternal(container, worldName, generator, environment, null);
                    if (world != null) {
                        applyWorldSettings(world, worldName);
                    }
                }
            }
        }
    }

    public boolean isWorldLoaded(String alias) {
        String fullPath = resolveFullPath(alias);
        return Bukkit.getWorld(fullPath) != null;
    }

    public void applyWorldSettings(World world, String alias) {
        String container = getContainerFromPath(world.getName());
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");

        // Keep Bedwars worlds in memory to ensure NPCs are always visible
        if (container.toLowerCase().contains("bedwars")) {
            world.setKeepSpawnInMemory(true);
        }
        
        synchronized (configLock) {
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
    }

    public void setWorldSetting(String alias, String setting, boolean value) {
        setWorldSetting(alias, setting, (Object) value);
    }

    public void setWorldSetting(String alias, String setting, Object value) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) return;

        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");
        
        synchronized (configLock) {
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
    }

    public Object getWorldSetting(String alias, String setting, Object defaultValue) {
        String fullPath = worldAliases.get(alias);
        if (fullPath == null) return defaultValue;

        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + alias.replace(".", "_");
        
        synchronized (configLock) {
            return worldsConfig.get("worlds." + key + ".settings." + setting.toLowerCase(), defaultValue);
        }
    }

    public World loadWorld(String container, String worldName) {
        return loadWorld(container, worldName, null);
    }

    public World loadWorld(String container, String worldName, String generator) {
        return loadWorld(container, worldName, generator, null);
    }

    public World loadWorld(String container, String worldName, String generator, World.Environment environment) {
        return loadWorld(container, worldName, generator, environment, null);
    }

    public World loadWorld(String container, String worldName, String generator, World.Environment environment, Long seed) {
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
            synchronized (configLock) {
                String envStr = worldsConfig.getString("worlds." + key + ".environment");
                finalEnvironment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
            }
        }

        World world = loadWorldInternal(container, worldName, generator, finalEnvironment, seed);
        if (world != null) {
            saveWorldToConfig(container, worldName, false, generator, world.getEnvironment());
        }
        return world;
    }

    public boolean removeWorld(String alias) {
        String fullPath = resolveFullPath(alias);
        String baseName = getAliasFromPath(fullPath);

        unloadWorld(fullPath, true);
        worldAliases.remove(baseName);
        lastAccessTime.remove(fullPath);
        hibernationWhitelist.remove(baseName);
        saveWhitelist();

        // Remove from config
        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + baseName.replace(".", "_");
        
        synchronized (configLock) {
            worldsConfig.set("worlds." + key, null);
            try {
                worldsConfig.save(worldsConfigFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save worlds.yml after world removal", e);
                return false;
            }
        }
    }

    public boolean deleteWorld(String alias) {
        String fullPath = resolveFullPath(alias);
        boolean unloaded = unloadWorld(fullPath, false);
        if (!unloaded && Bukkit.getWorld(fullPath) != null) {
            plugin.getLogger().warning("VFS: Failed to unload world '" + fullPath + "' before deletion. Deletion might fail if files are locked.");
        }
        return deleteWorldFiles(fullPath);
    }

    /**
     * Resolves an alias or partial path to the full technical name (e.g., "factory/slot0" -> "spycore-worlds/factory/slot0").
     */
    public String resolveFullPath(String alias) {
        if (alias == null) return null;
        
        // 1. Check if it's already a full technical path
        if (alias.startsWith("spycore-worlds/")) {
            return alias;
        }

        // 2. Check if it's in our alias map
        String mapped = worldAliases.get(alias);
        if (mapped != null) {
            return mapped;
        }

        // 3. Check if it's a container-style path (e.g., "factory/slot0")
        if (alias.contains("/")) {
            return "spycore-worlds/" + alias;
        }

        // 4. Fallback to just the alias (might be a root world)
        return alias;
    }

    /**
     * Delete only the files and configuration for a world.
     * MUST be called after the world is unloaded.
     * This method is safe to call from an asynchronous thread.
     */
    public boolean deleteWorldFiles(String alias) {
        String fullPath = resolveFullPath(alias);
        String baseName = getAliasFromPath(fullPath);

        plugin.getLogger().info("VFS: Attempting to delete world files for: " + fullPath + " (Alias: " + baseName + ")");

        worldAliases.remove(baseName);
        lastAccessTime.remove(fullPath);
        hibernationWhitelist.remove(baseName);
        saveWhitelist();

        // Remove from config
        String container = getContainerFromPath(fullPath);
        String key = container.replace(".", "_") + "_" + baseName.replace(".", "_");
        
        synchronized (configLock) {
            if (worldsConfig.contains("worlds." + key)) {
                worldsConfig.set("worlds." + key, null);
                try {
                    worldsConfig.save(worldsConfigFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save worlds.yml after world deletion", e);
                }
            }
        }

        // Physically delete folder
        File worldFolder = fullPath.startsWith("spycore-worlds/")
                ? new File(containersFolder, fullPath.replace("spycore-worlds/", "").replace("/", File.separator))
                : new File(plugin.getServer().getWorldContainer(), fullPath);

        if (worldFolder.exists()) {
            try {
                FileUtils.deleteDirectory(worldFolder);
                
                // Verify deletion
                if (worldFolder.exists()) {
                    plugin.getLogger().severe("VFS: FAILED to delete folder (still exists): " + worldFolder.getAbsolutePath());
                    return false;
                }
                
                plugin.getLogger().info("VFS: Successfully deleted folder: " + worldFolder.getAbsolutePath());
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "VFS: Could not delete world folder: " + worldFolder.getAbsolutePath(), e);
                return false;
            }
        } else {
            plugin.getLogger().info("VFS: Delete requested but folder does not exist: " + worldFolder.getAbsolutePath());
        }
        return true;
    }

    private String getAliasFromPath(String fullPath) {
        if (fullPath.startsWith("spycore-worlds/")) {
            String path = fullPath.replace("spycore-worlds/", "");
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
            return path;
        }
        return fullPath;
    }

    private String getContainerFromPath(String fullPath) {
        if (!fullPath.startsWith("spycore-worlds/")) return "root";
        String relative = fullPath.replace("spycore-worlds/", "");
        int lastSlash = relative.lastIndexOf("/");
        if (lastSlash == -1) return "root";
        return relative.substring(0, lastSlash);
    }

    private World loadWorldInternal(String container, String worldName, String generator, World.Environment environment, Long seed) {
        String fullPath = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? worldName 
                : "spycore-worlds/" + container + "/" + worldName;
        World world = Bukkit.getWorld(fullPath);
        if (world != null) return world;

        File worldFolder = (container == null || container.isEmpty() || container.equalsIgnoreCase("root")) 
                ? new File(plugin.getServer().getWorldContainer(), worldName) 
                : new File(containersFolder, container.replace("/", File.separator) + File.separator + worldName);
        
        if (!worldFolder.exists()) {
            plugin.getLogger().warning("VFS: World load failed - Folder does not exist: " + worldFolder.getAbsolutePath());
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
            if (generator.equalsIgnoreCase("lazy")) {
                creator.generator(new LazyTemplateGenerator());
            } else {
                creator.generator(generator);
            }
        }
        if (seed != null) {
            creator.seed(seed);
        }
        
        world = Bukkit.createWorld(creator);
        
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            lastAccessTime.put(fullPath, System.currentTimeMillis());
            plugin.getLogger().info("VFS: Loaded " + fullPath + " (Alias: " + worldName + ") [Env: " + world.getEnvironment() + "]" + (seed != null ? " with seed: " + seed : "") + (generator != null ? " with generator: " + generator : ""));
            Bukkit.getPluginManager().callEvent(new SpyWorldLoadEvent(world, worldName));
        }
        
        return world;
    }

    public World getWorld(String alias) {
        return getWorld(alias, true);
    }

    public World getWorld(String alias, boolean loadIfAbsent) {
        String fullPath = resolveFullPath(alias);
        
        World world = Bukkit.getWorld(fullPath);
        if (world == null && loadIfAbsent) {
            // Check if it's a known managed world even if not currently loaded
            String baseName = getAliasFromPath(fullPath);
            String container = getContainerFromPath(fullPath);
            String key = container.replace(".", "_") + "_" + baseName.replace(".", "_");
            
            String generator;
            String envStr;
            synchronized (configLock) {
                if (!worldsConfig.contains("worlds." + key)) {
                    // Not a managed world we know about in config
                    plugin.getLogger().warning("VFS: World load failed - Not in config: " + key + " (Alias: " + alias + ")");
                    return null;
                }
                generator = worldsConfig.getString("worlds." + key + ".generator");
                envStr = worldsConfig.getString("worlds." + key + ".environment");
            }
            World.Environment environment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;
            
            return loadWorldInternal(container.equals("root") ? null : container, baseName, generator, environment, null);
        }
        
        if (world != null) {
            lastAccessTime.put(fullPath, System.currentTimeMillis());
        }
        return world;
    }

    public World createWorld(String container, String worldName, World.Environment environment) {
        return createWorld(container, worldName, environment, (String) null);
    }

    public World createWorld(String container, String worldName, World.Environment environment, String generator) {
        return createWorld(container, worldName, environment, generator, null);
    }

    public World createWorld(String container, String worldName, World.Environment environment, Long seed) {
        return createWorld(container, worldName, environment, null, seed);
    }

    public World createWorld(String container, String worldName, World.Environment environment, String generator, Long seed) {
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
        creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE); // Optimization for Paper
        if (generator != null && !generator.isEmpty()) {
            if (generator.equalsIgnoreCase("lazy")) {
                creator.generator(new LazyTemplateGenerator());
            } else {
                creator.generator(generator);
            }
        }
        if (seed != null) {
            creator.seed(seed);
        }
        
        plugin.getLogger().info("VFS: Calling Bukkit.createWorld for " + fullPath + "...");
        long start = System.currentTimeMillis();
        World world = Bukkit.createWorld(creator);
        long end = System.currentTimeMillis();
        plugin.getLogger().info("VFS: Bukkit.createWorld for " + fullPath + " took " + (end - start) + "ms");

        if (world != null) {
            world.setKeepSpawnInMemory(false);
            
            // Only find safe location if NOT a factory world to save time/resources during batch creation
            boolean isFactory = (container != null && container.equalsIgnoreCase("factory")) || (generator != null && generator.equalsIgnoreCase("lazy"));
            if (!isFactory) {
                Location safe = findSafeLocation(world);
                world.setSpawnLocation(safe);
            }
            
            worldAliases.put(worldName, fullPath);
            lastAccessTime.put(fullPath, System.currentTimeMillis());
            saveWorldToConfig(container, worldName, false, generator, world.getEnvironment());
            plugin.getLogger().info("VFS: Created " + fullPath + " (Alias: " + worldName + ") " + (seed != null ? "with seed: " + seed : "") + " and generator: " + (generator != null ? generator : "default"));
            Bukkit.getPluginManager().callEvent(new SpyWorldCreateEvent(world, worldName));
        }

        return world;
    }

    public World cloneWorld(String sourceAlias, String targetContainer, String targetName) {
        return cloneWorld(sourceAlias, targetContainer, targetName, null);
    }

    public World cloneWorld(String sourceAlias, String targetContainer, String targetName, String generator) {
        World sourceWorld = getWorld(sourceAlias, true);
        Long seed = sourceWorld != null ? sourceWorld.getSeed() : null;
        World.Environment env = sourceWorld != null ? sourceWorld.getEnvironment() : World.Environment.NORMAL;

        if (copyWorldFiles(sourceAlias, targetContainer, targetName)) {
            return loadWorld(targetContainer, targetName, generator, env, seed);
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
        String fullPath = resolveFullPath(alias);
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
            String generator;
            String envStr;
            synchronized (configLock) {
                generator = worldsConfig.getString("worlds." + oldKey + ".generator");
                envStr = worldsConfig.getString("worlds." + oldKey + ".environment");
                worldsConfig.set("worlds." + oldKey, null);
            }
            World.Environment environment = envStr != null ? World.Environment.valueOf(envStr) : World.Environment.NORMAL;

            // Update aliases and config
            worldAliases.remove(alias);
            worldAliases.put(targetName, targetFullPath);
            lastAccessTime.remove(sourceFullPath);
            
            saveWorldToConfig(targetContainer, targetName, false, generator, environment);
            
            // Load in new location
            loadWorldInternal(targetContainer, targetName, generator, environment, null);
            
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
            synchronized (configLock) {
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
                    loadWorldInternal(newContainer, alias, generator, environment, null);
                }
                
                try {
                    worldsConfig.save(worldsConfigFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save worlds.yml after container move", e);
                }
            }
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

        synchronized (configLock) {
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
        
        synchronized (configLock) {
            worldsConfig.set("worlds." + key + ".container", container);
            worldsConfig.set("worlds." + key + ".name", worldName);
            worldsConfig.set("worlds." + key + ".hibernate", false); // Force hibernate false by default
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
    }

    public void checkHibernation() {
        long now = System.currentTimeMillis();
        long threshold = 10 * 60 * 1000; // 10 minutes

        for (Map.Entry<String, String> entry : worldAliases.entrySet()) {
            String alias = entry.getKey();
            String fullPath = entry.getValue();
            
            // Skip whitelisted worlds
            if (hibernationWhitelist.contains(alias)) {
                continue;
            }

            World world = Bukkit.getWorld(fullPath);
            
            if (world != null) {
                // Check if this world or any of its linked worlds have players
                boolean hasPlayers = !world.getPlayers().isEmpty();
                
                if (!hasPlayers) {
                    Set<String> linked = linkedWorlds.get(fullPath);
                    if (linked != null) {
                        for (String otherPath : linked) {
                            World other = Bukkit.getWorld(otherPath);
                            if (other != null && !other.getPlayers().isEmpty()) {
                                hasPlayers = true;
                                break;
                            }
                        }
                    }
                }

                if (!hasPlayers) {
                    Long lastAccess = lastAccessTime.get(fullPath);
                    if (lastAccess != null && (now - lastAccess) > threshold) {
                        unloadWorld(entry.getKey(), true);
                        plugin.getLogger().info("VFS: Hibernating world " + fullPath + " due to inactivity.");
                    }
                } else {
                    // Update last access time if players are present in the set
                    lastAccessTime.put(fullPath, now);
                }
            }
        }
    }

    public void linkWorlds(World world1, World world2) {
        if (world1 == null || world2 == null) return;
        String name1 = world1.getName();
        String name2 = world2.getName();
        
        plugin.getLogger().info("[WorldManager] Linking worlds: " + name1 + " and " + name2);
        
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
        plugin.getLogger().info("[WorldManager] Transitive link established for " + allLinked.size() + " worlds: " + allLinked);
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
