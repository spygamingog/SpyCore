package com.spygamingog.spycore;

import com.spygamingog.spycore.api.DataService;
import com.spygamingog.spycore.commands.SpyCommand;
import com.spygamingog.spycore.managers.*;
import com.spygamingog.spycore.services.HologramService;
import com.spygamingog.spycore.services.YamlDataService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpyCore extends JavaPlugin {

    @Getter
    private static SpyCore instance;

    @Getter
    private WorldManager worldManager;
    @Getter
    private PlayerManager playerManager;
    @Getter
    private ServiceManager serviceManager;
    @Getter
    private TemplateManager templateManager;
    @Getter
    private MetadataManager metadataManager;
    @Getter
    private PacketManager packetManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Managers
        this.worldManager = new WorldManager(this);
        this.playerManager = new PlayerManager(this);
        this.serviceManager = new ServiceManager(this);
        this.templateManager = new TemplateManager(this);
        this.metadataManager = new MetadataManager(this);
        this.packetManager = new PacketManager(this);

        // Register Services
        this.serviceManager.registerService(HologramService.class, new HologramService());
        this.serviceManager.registerService(DataService.class, new YamlDataService(this));

        // Start Hibernation Task (Check every 1 minute)
        Bukkit.getScheduler().runTaskTimer(this, () -> worldManager.checkHibernation(), 20 * 60, 20 * 60);

        // Register Commands
        getCommand("spy").setExecutor(new SpyCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new com.spygamingog.spycore.listeners.WorldSettingsListener(this), this);
        getServer().getPluginManager().registerEvents(new com.spygamingog.spycore.listeners.WorldChatTabListener(this), this);

        // Load data
        this.worldManager.loadWorlds();
        this.playerManager.initialize();

        getLogger().info("SpyCore has been enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown sequence
        if (worldManager != null) {
            worldManager.shutdown();
        }
        if (playerManager != null) {
            playerManager.shutdown();
        }
        
        getLogger().info("SpyCore has been disabled!");
    }
}
