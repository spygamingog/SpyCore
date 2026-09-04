package com.spygamingog.spycore;

import com.spygamingog.spycore.api.DataService;
import com.spygamingog.spycore.commands.SpyCommand;
import com.spygamingog.spycore.managers.*;
import com.spygamingog.spycore.placeholders.SpyCoreExpansion;
import com.spygamingog.spycore.services.HologramService;
import com.spygamingog.spycore.services.YamlDataService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpyCore extends JavaPlugin {

    private static SpyCore instance;

    public static SpyCore getInstance() {
        return instance;
    }

    private WorldManager worldManager;
    private PlayerManager playerManager;
    private ServiceManager serviceManager;
    private TemplateManager templateManager;
    private MetadataManager metadataManager;
    private PacketManager packetManager;

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    public PacketManager getPacketManager() {
        return packetManager;
    }

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

        // Load worlds FIRST so they are available for data deserialization
        this.worldManager.loadWorlds();

        // Register Services
        this.serviceManager.registerService(HologramService.class, new HologramService());
        this.serviceManager.registerService(DataService.class, new YamlDataService(this));

        // Register Commands
        getCommand("spy").setExecutor(new SpyCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new com.spygamingog.spycore.listeners.WorldSettingsListener(this), this);
        getServer().getPluginManager().registerEvents(new com.spygamingog.spycore.listeners.WorldIsolationListener(this), this);

        // Schedule automated world hibernation checker every 60 seconds (1200 ticks)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (this.worldManager != null) {
                this.worldManager.checkHibernation();
            }
        }, 1200L, 1200L);

        // Load data
        this.playerManager.initialize();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.spygamingog.spycore.placeholders.SpyCoreExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered!");
        }

        getLogger().info("SpyCore v" + getDescription().getVersion() + " has been enabled!");
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
