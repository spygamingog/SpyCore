package com.spygamingog.spycore.listeners;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.managers.WorldManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class WorldSettingsListener implements Listener {
    private final SpyCore plugin;
    private final WorldManager worldManager;

    public WorldSettingsListener(SpyCore plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMobSpawn(EntitySpawnEvent event) {
        World world = event.getLocation().getWorld();
        if (world == null) return;

        String alias = worldManager.getAliasForWorld(world);
        if (alias == null) return;

        boolean mobSpawn = (boolean) worldManager.getWorldSetting(alias, "mobspawn", true);
        if (!mobSpawn) {
            org.bukkit.entity.EntityType type = event.getEntityType();
            // Allow ArmorStands (Holograms), Items (Generators), Golems, and Fish
            if (type == org.bukkit.entity.EntityType.ARMOR_STAND || 
                type == org.bukkit.entity.EntityType.ITEM ||
                type.name().contains("GOLEM") || 
                type.name().contains("FISH")) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        World world = player.getWorld();
        String alias = worldManager.getAliasForWorld(world);
        if (alias == null) return;

        boolean hunger = (boolean) worldManager.getWorldSetting(alias, "hunger", true);
        if (!hunger) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAutoHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        World world = player.getWorld();
        String alias = worldManager.getAliasForWorld(world);
        if (alias == null) return;

        boolean autoHeal = (boolean) worldManager.getWorldSetting(alias, "autoheal", true);
        if (!autoHeal && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        applyFlySetting(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyFlySetting(event.getPlayer());
    }

    private void applyFlySetting(Player player) {
        World world = player.getWorld();
        String alias = worldManager.getAliasForWorld(world);
        if (alias == null) return;

        boolean fly = (boolean) worldManager.getWorldSetting(alias, "fly", false);
        if (fly || player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }
}
