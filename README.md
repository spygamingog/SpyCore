# SpyCore

[![Platform](https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur-blue)](https://papermc.io)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-green)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-CC_BY--NC--SA_4.0-lightgrey)](LICENSE)

SpyCore is a multi-world management plugin for Paper and Purpur (1.21+).

Traditional multi-world plugins dump every single world folder directly into your server root, which gets messy quickly on larger servers or minigame networks. SpyCore organizes worlds into categorized subfolders (called **containers**) using a virtual file system (VFS), while still letting you reference them with clean, simple world names in commands and APIs.

It also comes with built-in world hibernation to save RAM, fast template cloning for minigame arenas, per-world rule toggles (PVP, hunger, flight, autoheal), and target selector support (`@a`, `@p`, `@s`, `@r`, `@e`).

---

## Features

- **Folder-based World Organization (VFS)**: Keep your worlds organized. Group minigames, arenas, or seasonal worlds into subdirectories like `spycore-worlds/Bedwars/Solo1` while players and scripts simply refer to `Solo1`.
- **World Hibernation**: Inactive worlds with zero players can automatically unload from memory to conserve RAM and CPU. Hubs and main survival worlds can be whitelisted to stay permanently loaded.
- **Fast Arena & Minigame Templating**: Clone template worlds on the fly with lazy chunk generators that skip Bukkit's slow spawn chunk loading. Ideal for disposable match worlds.
- **Per-World Rule Control**: Configure PVP, hunger depletion, natural regeneration, survival flight, time/weather cycles, and bed respawn behavior independently per world without needing extra rule plugins.
- **Target Selector Support**: Teleport commands natively accept standard Minecraft selectors (`/spy world tp @a Arena1`, `/spy tp @p lobby`, etc.).
- **Chat & Tablist Isolation**: Optionally isolate local chat, death messages, advancements, and tablist visibility to the world a player is currently in.
- **PlaceholderAPI Hook**: Includes placeholders like `%spycore_world_alias%` and `%spycore_world_name%`.
- **Ecosystem Integration**: Pairs directly with [SpyInventories](https://github.com/spygamingog/SpyInventories) for per-world inventory grouping and [SpyNetherPortals](https://github.com/spygamingog/SpyNetherPortals) for custom portal links.

---

## Requirements & Setup

- **Server**: Paper or Purpur 1.21+ (Java 21 or newer required).
- **Setup**:
  1. Download the latest `spycore-1.1.1.jar` from Releases.
  2. Put the `.jar` into your server's `plugins/` directory.
  3. Restart your server. Existing worlds in the server root will be detected and registered automatically.

---

## Quick Command Guide

All commands run under the `/spy` base command (requires `spy.admin` or specific sub-permissions).

### Creating & Managing Worlds
```text
# Create a standard survival world in the server root
/spy create world survival normal

# Create a void world inside a "Bedwars" container folder
/spy container Bedwars create world Arena1 normal --generator voidgen

# Clone an arena template into a match instance
/spy clone Arena1 Matches Match_01 voidgen

# Load an existing world from disk / unload an inactive world
/spy load survival
/spy unload Arena1

# Permanently delete a world folder from disk
/spy delete Match_01
```

### Teleportation & Player Movement
```text
# Teleport yourself to a world
/spy world tp survival

# Teleport another player or a target selector
/spy world tp Steve survival
/spy world tp @a Arena1

# Set the world spawn to your current coordinates
/spy world setspawn
```

### World Rules & Hibernation
```text
# Disable PVP and hunger in a lobby or arena
/spy world modify lobby set pvp false
/spy world modify lobby set hunger false

# Allow survival flight in a creative world
/spy world modify creative set fly true

# Prevent lobby from being unloaded by the hibernation task
/spy whitelist add lobby
```

For the full list of subcommands, permission nodes, generator flags, and storage file schemas, check out [DOCUMENTATION.md](DOCUMENTATION.md).

---

## Developer API

If you are developing plugins on top of SpyCore, add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.spygamingog</groupId>
    <artifactId>spycore</artifactId>
    <version>1.1.1</version>
    <scope>provided</scope>
</dependency>
```

Make sure to add `depend: [SpyCore]` (or `softdepend: [SpyCore]`) in your `plugin.yml`.

### Example Usage
```java
import com.spygamingog.spycore.api.SpyAPI;
import org.bukkit.World;
import org.bukkit.entity.Player;

// Fetch or wake a world by alias
World world = SpyAPI.getWorld("Arena1", true);

// Create a new void world inside a container
World arena = SpyAPI.createWorld("Bedwars", "Arena_02", World.Environment.NORMAL, "voidgen");

// Safely teleport a player
SpyAPI.teleportToSafeLocation(player, world);

// Attach custom metadata tags to a world
SpyAPI.setWorldTag(arena, "game_state", "WAITING");
String state = SpyAPI.getWorldTag(arena, "game_state");
```

---

## Related Plugins

- **[SpyInventories](https://github.com/spygamingog/SpyInventories)**: Multi-world inventory and player state separation (inventories, ender chests, health, gamemodes).
- **[SpyNetherPortals](https://github.com/spygamingog/SpyNetherPortals)**: Custom Nether & End portal routing for container worlds and custom dimensions.

---

## License

This project is licensed under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (CC BY-NC-SA 4.0)](LICENSE).
