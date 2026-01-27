package com.spygamingog.spycore.services;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class HologramService {
    private final List<ArmorStand> holograms = new ArrayList<>();

    public void spawn(Location location, String text) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setMarker(true);
        holograms.add(armorStand);
    }

    public void clearAll() {
        for (ArmorStand stand : holograms) {
            if (stand.isValid()) {
                stand.remove();
            }
        }
        holograms.clear();
    }
}
