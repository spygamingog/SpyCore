package com.spygamingog.spycore.listeners;

import com.spygamingog.spycore.SpyCore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class WorldChatTabListener implements Listener {

    private final SpyCore plugin;

    public WorldChatTabListener(SpyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        World world = sender.getWorld();

        // Only show chat to players in the same world or linked worlds
        event.getRecipients().removeIf(recipient -> {
            World rWorld = recipient.getWorld();
            return !plugin.getWorldManager().areLinked(world, rWorld);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        updateVisibility(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        updateVisibility(event.getPlayer());
    }

    private void updateVisibility(Player player) {
        World world = player.getWorld();

        // Use a small delay to ensure world change is fully processed by Bukkit
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;

                World oWorld = online.getWorld();
                boolean shouldSee = plugin.getWorldManager().areLinked(world, oWorld);

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
}
