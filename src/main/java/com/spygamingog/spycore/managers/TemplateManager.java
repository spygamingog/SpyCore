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
        File templateDir = new File(templatesFolder, templateName);
        if (!templateDir.exists()) {
            plugin.getLogger().warning("Template not found: " + templateName);
            return null;
        }

        File targetDir = new File(plugin.getServer().getWorldContainer(), "containers/" + targetContainer + "/" + targetName);
        
        try {
            FileUtils.copyDirectory(templateDir, targetDir);
            // Remove uid.dat if it exists to avoid UUID conflicts
            File uidFile = new File(targetDir, "uid.dat");
            if (uidFile.exists()) {
                uidFile.delete();
            }
            
            return plugin.getWorldManager().loadWorld(targetContainer, targetName);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to copy template: " + templateName, e);
            return null;
        }
    }

    public void deleteDisposableWorld(String container, String worldName) {
        World world = Bukkit.getWorld("containers/" + container + "/" + worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        File worldDir = new File(plugin.getServer().getWorldContainer(), "containers/" + container + "/" + worldName);
        try {
            FileUtils.deleteDirectory(worldDir);
            plugin.getLogger().info("Deleted disposable world: " + container + "/" + worldName);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete world: " + worldName, e);
        }
    }
}
