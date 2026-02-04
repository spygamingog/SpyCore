package com.spygamingog.spycore.generators;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * A "Lazy" generator that bypasses Bukkit's expensive spawn point search.
 * By providing a fixed spawn location, we prevent the server from loading
 * chunks to find a safe spot during world creation.
 */
public class LazyTemplateGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return true; // Keep true so we actually get land
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return true;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return true;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return true;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }

    @Override
    public @Nullable org.bukkit.Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        // Return (0, 100, 0) immediately. 
        // Bukkit won't load chunks to find "solid ground" if this is returned.
        return new org.bukkit.Location(world, 0, 100, 0);
    }
}
