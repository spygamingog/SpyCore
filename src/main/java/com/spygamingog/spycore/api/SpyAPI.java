package com.spygamingog.spycore.api;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.managers.*;
import com.spygamingog.spycore.services.HologramService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The primary API entry point for SpyCore.
 * Developers should use this class to interact with world management,
 * metadata, and services instead of accessing managers directly.
 */
public class SpyAPI {

    /**
     * Teleports a player to a safe location in the target world.
     * @param player The player to teleport.
     * @param world The target world.
     */
    public static void teleportToSafeLocation(Player player, World world) {
        Location safe = getWorldManager().findSafeLocation(world);
        player.teleport(safe);
    }

    /**
     * Get a world by its alias or name.
     * @param alias The world alias or technical name.
     * @return The World object, or null if not found/loaded.
     */
    public static @Nullable World getWorld(String alias) {
        return getWorldManager().getWorld(alias);
    }

    /**
     * Get a world by its alias or name, with control over automatic loading.
     * @param alias The world alias or technical name.
     * @param loadIfAbsent Whether to load the world if it's currently hibernating.
     * @return The World object, or null if not found/loaded.
     */
    public static @Nullable World getWorld(String alias, boolean loadIfAbsent) {
        return getWorldManager().getWorld(alias, loadIfAbsent);
    }

    /**
     * Manually wake a world from hibernation.
     * @param alias The world alias or technical name.
     * @return The World object, or null if it couldn't be loaded.
     */
    public static @Nullable World wakeWorld(String alias) {
        return getWorldManager().wakeWorld(alias);
    }

    /**
     * Add a world to the hibernation whitelist.
     * @param alias The world alias.
     */
    public static void addWorldToHibernationWhitelist(String alias) {
        getWorldManager().addWorldToWhitelist(alias);
    }

    /**
     * Remove a world from the hibernation whitelist.
     * @param alias The world alias.
     */
    public static void removeWorldFromHibernationWhitelist(String alias) {
        getWorldManager().removeWorldFromWhitelist(alias);
    }

    /**
     * Check if a world is whitelisted from hibernation.
     * @param alias The world alias.
     * @return True if whitelisted.
     */
    public static boolean isWorldHibernationWhitelisted(String alias) {
        return getWorldManager().isWorldWhitelisted(alias);
    }

    /**
     * Get the alias for a given world.
     * @param world The world object.
     * @return The alias name.
     */
    public static String getAliasForWorld(World world) {
        return getWorldManager().getAliasForWorld(world);
    }

    /**
     * Get the container name for a given world.
     * @param world The world object.
     * @return The container name, or "root".
     */
    public static String getContainerForWorld(World world) {
        return getWorldManager().getContainerForWorld(world);
    }

    /**
     * Check if a world is currently loaded.
     * @param alias The world alias.
     * @return True if loaded.
     */
    public static boolean isWorldLoaded(String alias) {
        return getWorldManager().isWorldLoaded(alias);
    }

    /**
     * Load a world from a specific container.
     * @param container The container name (e.g., "lobby"). Use null for root.
     * @param worldName The world folder name.
     * @return The loaded World object.
     */
    public static World loadWorld(@Nullable String container, String worldName) {
        return getWorldManager().loadWorld(container, worldName, null);
    }

    /**
     * Load a world from a specific container with a custom generator.
     * @param container The container name. Use null for root.
     * @param worldName The world folder name.
     * @param generator The generator name.
     * @return The loaded World object.
     */
    public static World loadWorld(@Nullable String container, String worldName, @Nullable String generator) {
        return getWorldManager().loadWorld(container, worldName, generator, null);
    }

    /**
     * Load a world from a specific container with a custom generator and environment.
     * @param container The container name. Use null for root.
     * @param worldName The world folder name.
     * @param generator The generator name.
     * @param environment The world environment.
     * @return The loaded World object.
     */
    public static World loadWorld(@Nullable String container, String worldName, @Nullable String generator, World.Environment environment) {
        return getWorldManager().loadWorld(container, worldName, generator, environment, null);
    }

    /**
     * Load a world from a specific container with a custom generator, environment and seed.
     * @param container The container name. Use null for root.
     * @param worldName The world folder name.
     * @param generator The generator name.
     * @param environment The world environment.
     * @param seed The seed to ensure consistency (optional).
     * @return The loaded World object.
     */
    public static World loadWorld(@Nullable String container, String worldName, @Nullable String generator, World.Environment environment, @Nullable Long seed) {
        return getWorldManager().loadWorld(container, worldName, generator, environment, seed);
    }

    /**
     * Create a new world.
     * @param container The container name. Use null for root.
     * @param worldName The world name.
     * @param environment The world environment (NORMAL, NETHER, END).
     * @return The created World object.
     */
    public static World createWorld(@Nullable String container, String worldName, World.Environment environment) {
        return getWorldManager().createWorld(container, worldName, environment, (String) null);
    }

    /**
     * Create a new world with a custom generator.
     * @param container The container name. Use null for root.
     * @param worldName The world name.
     * @param environment The world environment (NORMAL, NETHER, END).
     * @param generator The generator name (e.g., "VoidGen").
     * @return The created World object.
     */
    public static World createWorld(@Nullable String container, String worldName, World.Environment environment, @Nullable String generator) {
        return getWorldManager().createWorld(container, worldName, environment, generator);
    }

    /**
     * Create a new world with a seed.
     * @param container The container name. Use null for root.
     * @param worldName The world name.
     * @param environment The world environment (NORMAL, NETHER, END).
     * @param seed The seed.
     * @return The created World object.
     */
    public static World createWorld(@Nullable String container, String worldName, World.Environment environment, long seed) {
        return getWorldManager().createWorld(container, worldName, environment, seed);
    }

    /**
     * Create a new world with a custom generator and seed.
     * @param container The container name. Use null for root.
     * @param worldName The world name.
     * @param environment The world environment (NORMAL, NETHER, END).
     * @param generator The generator name.
     * @param seed The seed.
     * @return The created World object.
     */
    public static World createWorld(@Nullable String container, String worldName, World.Environment environment, @Nullable String generator, long seed) {
        return getWorldManager().createWorld(container, worldName, environment, generator, seed);
    }

    /**
     * Clone an existing world.
     * @param sourceAlias The source world alias or name.
     * @param targetContainer The target container name.
     * @param targetName The target world name.
     * @return The cloned World object.
     */
    public static World cloneWorld(String sourceAlias, @Nullable String targetContainer, String targetName) {
        return getWorldManager().cloneWorld(sourceAlias, targetContainer, targetName, null);
    }

    /**
     * Clone an existing world with a custom generator.
     * @param sourceAlias The source world alias or name.
     * @param targetContainer The target container name.
     * @param targetName The target world name.
     * @param generator The generator name for the cloned world.
     * @return The cloned World object.
     */
    public static World cloneWorld(String sourceAlias, @Nullable String targetContainer, String targetName, @Nullable String generator) {
        return getWorldManager().cloneWorld(sourceAlias, targetContainer, targetName, generator);
    }

    /**
     * Unload a world.
     * @param alias The world alias.
     * @param save Whether to save the world before unloading.
     * @return True if successful.
     */
    public static boolean unloadWorld(String alias, boolean save) {
        return getWorldManager().unloadWorld(alias, save);
    }

    /**
     * Delete a world and its files.
     * @param alias The world alias.
     * @return True if successful.
     */
    public static boolean deleteWorld(String alias) {
        return getWorldManager().deleteWorld(alias);
    }

    /**
     * Delete only the files and configuration for a world.
     * MUST be called after the world is unloaded.
     * This method is safe to call from an asynchronous thread.
     */
    public static boolean deleteWorldFiles(String alias) {
        return getWorldManager().deleteWorldFiles(alias);
    }

    /**
     * Copy world files from source to target.
     * This method is safe to call from an asynchronous thread.
     * @return True if successful.
     */
    public static boolean copyWorldFiles(String sourceAlias, @Nullable String targetContainer, String targetName) {
        return getWorldManager().copyWorldFiles(sourceAlias, targetContainer, targetName);
    }

    /**
     * Link two worlds together to share chat and tablist.
     * @param world1 First world.
     * @param world2 Second world.
     */
    public static void linkWorlds(World world1, World world2) {
        getWorldManager().linkWorlds(world1, world2);
    }

    /**
     * Check if two worlds are linked.
     * @param world1 First world.
     * @param world2 Second world.
     * @return True if linked.
     */
    public static boolean areLinked(World world1, World world2) {
        return getWorldManager().areLinked(world1, world2);
    }

    /**
     * Unlink a world from all others.
     * @param world The world to unlink.
     */
    public static void unlinkWorlds(World world) {
        getWorldManager().unlinkWorlds(world);
    }

    /**
     * Attach metadata to a world.
     * @param world The world object.
     * @param key The metadata key.
     * @param value The metadata value.
     */
    public static void setWorldTag(World world, String key, String value) {
        getMetadataManager().setTag(world, key, value);
    }

    /**
     * Get metadata from a world.
     * @param world The world object.
     * @param key The metadata key.
     * @return The metadata value, or null.
     */
    public static String getWorldTag(World world, String key) {
        return getMetadataManager().getTag(world, key);
    }

    /**
     * Find worlds matching specific metadata in a container.
     * @param container The container to search.
     * @param tags Map of key-value pairs to match.
     * @return List of matching worlds.
     */
    public static List<World> findWorlds(String container, Map<String, String> tags) {
        return getMetadataManager().findWorlds(container, tags);
    }

    public static WorldManager getWorldManager() {
        return SpyCore.getInstance().getWorldManager();
    }

    public static PlayerManager getPlayerManager() {
        return SpyCore.getInstance().getPlayerManager();
    }

    public static MetadataManager getMetadataManager() {
        return SpyCore.getInstance().getMetadataManager();
    }

    public static ServiceManager getServiceManager() {
        return SpyCore.getInstance().getServiceManager();
    }

    public static DataService getDataService() {
        return getServiceManager().getService(DataService.class);
    }

    public static HologramService getHologramService() {
        return getServiceManager().getService(HologramService.class);
    }
}
