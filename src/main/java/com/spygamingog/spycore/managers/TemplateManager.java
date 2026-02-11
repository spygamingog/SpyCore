package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class TemplateManager {
    private final SpyCore plugin;
    private final File templatesFolder;

    public TemplateManager(SpyCore plugin) {
        this.plugin = plugin;
        this.templatesFolder = new File(plugin.getDataFolder(), "templates");
        if (!templatesFolder.exists()) {
            templatesFolder.mkdirs();
        }
    }

    public World createFromTemplate(String templateName, String targetContainer, String targetName) {
        return createFromTemplate(templateName, targetContainer, targetName, false);
    }

    public World createFromTemplate(String templateName, String targetContainer, String targetName, boolean temporary) {
        File templateDir = resolveTemplateDir(templateName);
        if (templateDir == null || !templateDir.exists()) {
            plugin.getLogger().warning("Template not found in any source: " + templateName);
            return null;
        }

        // Target path consistency with WorldManager (spycore-worlds/)
        String containerPath = (targetContainer == null || targetContainer.isEmpty()) ? "" : targetContainer + "/";
        File targetDir = new File(plugin.getServer().getWorldContainer(), "spycore-worlds/" + containerPath + targetName);
        
        try {
            // Optimization: If it's a temporary world, we don't necessarily need to copy everything if we use a Read-Only approach,
            // but for standard Bukkit, we still copy the bytes to a temp folder.
            FileUtils.copyDirectory(templateDir, targetDir);
            
            // Remove session.lock and uid.dat to avoid conflicts
            new File(targetDir, "session.lock").delete();
            new File(targetDir, "uid.dat").delete();
            
            World world = plugin.getWorldManager().loadWorld(targetContainer, targetName);
            if (world != null && temporary) {
                // Optimization: No-Save Flag & KeepSpawnInMemory false
                world.setAutoSave(false);
                world.setKeepSpawnInMemory(false);
                plugin.getLogger().info("VFS: Configured " + targetName + " as a TEMPORARY world (AutoSave=false).");
            }
            return world;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to copy template: " + templateName, e);
            return null;
        }
    }

    private File resolveTemplateDir(String templateName) {
        // 1. Check internal templates folder (plugins/SpyCore/templates)
        File internal = new File(templatesFolder, templateName);
        if (internal.exists()) return internal;

        // 2. Check spycore-worlds container
        File containerWorld = new File(plugin.getServer().getWorldContainer(), "spycore-worlds/" + templateName);
        if (containerWorld.exists()) return containerWorld;

        // 3. Check root folder
        File rootWorld = new File(plugin.getServer().getWorldContainer(), templateName);
        if (rootWorld.exists()) return rootWorld;

        return null;
    }

    public void deleteDisposableWorld(String container, String worldName) {
        String containerPath = (container == null || container.isEmpty()) ? "" : container + "/";
        String fullPath = "spycore-worlds/" + containerPath + worldName;
        
        World world = Bukkit.getWorld(fullPath);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        File worldDir = new File(plugin.getServer().getWorldContainer(), fullPath);
        try {
            FileUtils.deleteDirectory(worldDir);
            plugin.getLogger().info("Deleted disposable world: " + fullPath);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete world: " + worldName, e);
        }
    }
}
