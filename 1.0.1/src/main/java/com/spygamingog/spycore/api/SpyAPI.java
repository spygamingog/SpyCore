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
        return getWorldManager().loadWorld(container, worldName, generator);
    }

    /**
     * Create a new world.
     * @param container The container name. Use null for root.
     * @param worldName The world name.
     * @param environment The world environment (NORMAL, NETHER, END).
     * @return The created World object.
     */
    public static World createWorld(@Nullable String container, String worldName, World.Environment environment) {
        return getWorldManager().createWorld(container, worldName, environment, null);
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
