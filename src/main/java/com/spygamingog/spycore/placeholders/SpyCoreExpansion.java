package com.spygamingog.spycore.placeholders;

import com.spygamingog.spycore.SpyCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SpyCoreExpansion extends PlaceholderExpansion {

    private final SpyCore plugin;

    public SpyCoreExpansion(SpyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "SpyGaming";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "spycore";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "";

        World world = player.getPlayer().getWorld();
        String worldName = world.getName();

        switch (params.toLowerCase()) {
            case "world_name":
                return worldName;
            case "world_alias":
                // Try to find the alias for the current world name (full path)
                for (Map.Entry<String, String> entry : plugin.getWorldManager().getWorldAliases().entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(worldName)) {
                        return entry.getKey();
                    }
                }
                return worldName; // Fallback to full path if no alias found
        }

        return null;
    }
}
