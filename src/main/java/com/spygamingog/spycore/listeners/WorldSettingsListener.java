package com.spygamingog.spycore.listeners;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.managers.WorldManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WorldSettingsListener implements Listener {
    private final SpyCore plugin;
    private final WorldManager worldManager;
    private final Set<UUID> pendingSpawnSetting = new HashSet<>();

    public WorldSettingsListener(SpyCore plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        
        // Check if player is in a world that needs spawn setting
        if (pendingSpawnSetting.contains(player.getUniqueId())) {
            Block block = player.getLocation().getBlock();
            
            // If player is in a safe spot (ground is solid/water and head/foot are air)
            if (worldManager.isSafe(block)) {
                world.setSpawnLocation(player.getLocation());
                pendingSpawnSetting.remove(player.getUniqueId());
                player.sendMessage("§a[SpyCore] World spawn location has been set to your current position.");
            }
        }
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
        Player player = event.getPlayer();
        applyFlySetting(player);
        
        // Check if we need to track this player for spawn setting in the new world
        String alias = worldManager.getAliasForWorld(player.getWorld());
        if (alias != null && player.getLocation().getY() >= 249) {
            pendingSpawnSetting.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World world = event.getRespawnLocation().getWorld();
        if (world == null) return;

        String alias = worldManager.getAliasForWorld(world);
        if (alias == null) return;

        boolean bedRespawn = (boolean) worldManager.getWorldSetting(alias, "bedrespawn", true);
        if (!bedRespawn && event.isBedSpawn()) {
            event.setRespawnLocation(world.getSpawnLocation());
            player.sendMessage("§c[SpyCore] Bed respawn is disabled in this world. Teleporting to spawn.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPVP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;

        World world = victim.getWorld();
        String alias = worldManager.getAliasForWorld(world);
        if (alias == null) return;

        boolean pvp = (boolean) worldManager.getWorldSetting(alias, "pvp", true);
        if (!pvp) {
            event.setCancelled(true);
        }
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
