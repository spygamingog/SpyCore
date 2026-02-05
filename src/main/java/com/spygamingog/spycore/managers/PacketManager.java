package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import org.bukkit.entity.Player;

public class PacketManager {
    private final SpyCore plugin;

    public PacketManager(SpyCore plugin) {
        this.plugin = plugin;
    }

    public void sendActionbar(Player player, String message) {
        player.sendActionBar(message); // Paper API has this
    }

    public void swingArm(Player player) {
        // Example of something that might need NMS or internal API
        // In 1.21.1, we can use player.swingMainHand()
        player.swingMainHand();
    }

    // Future-proofing: When 1.22 comes, we only update this manager
    public void hideNameplate(Player player, Player observer) {
        // Implementation for hiding nameplate using packets
        // This is where NMS would go
    }
}
