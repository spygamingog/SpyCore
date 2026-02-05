package com.spygamingog.spycore.listeners;

import com.spygamingog.spycore.SpyCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class WorldIsolationListener implements Listener {

    private final SpyCore plugin;

    public WorldIsolationListener(SpyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        World world = sender.getWorld();

        // STRICT ISOLATION: Only show chat to players in the EXACT SAME world.
        // SpyNetherPortals (or others) can override this at a higher priority 
        // to add linked worlds back (Nether/End).
        event.getRecipients().removeIf(recipient -> !recipient.getWorld().equals(world));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinMonitor(PlayerJoinEvent event) {
        updateVisibility(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinMessage(PlayerJoinEvent event) {
        handleJoinMessage(event);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuitMessage(PlayerQuitEvent event) {
        handleQuitMessage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        updateVisibility(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        handleDeathMessage(event);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        handleAdvancementMessage(event);
    }

    private void updateVisibility(Player player) {
        World world = player.getWorld();

        // Use a small delay to ensure world change is fully processed by Bukkit
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;

                World oWorld = online.getWorld();
                // STRICT ISOLATION: Only see players in the EXACT SAME world.
                boolean shouldSee = world.equals(oWorld);

                if (shouldSee) {
                    player.showPlayer(plugin, online);
                    online.showPlayer(plugin, player);
                } else {
                    player.hidePlayer(plugin, online);
                    online.hidePlayer(plugin, player);
                }
            }
        }, 2L);
    }
    
    private void handleJoinMessage(PlayerJoinEvent event) {
        Component message = event.joinMessage();
        if (message == null) return;
        
        event.joinMessage(null); // Cancel global broadcast
        broadcastToWorld(event.getPlayer().getWorld(), message);
    }

    private void handleQuitMessage(PlayerQuitEvent event) {
        Component message = event.quitMessage();
        if (message == null) return;
        
        event.quitMessage(null); // Cancel global broadcast
        broadcastToWorld(event.getPlayer().getWorld(), message);
    }
    
    private void handleDeathMessage(PlayerDeathEvent event) {
        Component message = event.deathMessage();
        if (message == null) return;
        
        event.deathMessage(null); // Cancel global broadcast
        broadcastToWorld(event.getEntity().getWorld(), message);
    }
    
    private void handleAdvancementMessage(PlayerAdvancementDoneEvent event) {
        Component message = event.message();
        if (message == null) return;
        
        event.message(null); // Cancel global broadcast
        broadcastToWorld(event.getPlayer().getWorld(), message);
    }
    
    private void broadcastToWorld(World world, Component message) {
        for (Player online : world.getPlayers()) {
            online.sendMessage(message);
        }
        // Also log to console
        Bukkit.getConsoleSender().sendMessage(message);
    }
}
