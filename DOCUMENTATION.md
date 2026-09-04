# SpyCore — Complete Documentation

Comprehensive technical manual and administration guide for the **SpyCore** multi-world management and foundation framework for Minecraft servers.

---

## 1. Overview & Core Architecture

SpyCore replaces standard monolithic world systems with a **Virtual File System (VFS)**. Worlds can be kept in the traditional root server directory or grouped inside hierarchical folder containers under `spycore-worlds/`.

### Virtual File System (VFS) Concept
- **Root Worlds**: Standard worlds stored at `<server-root>/<worldname>` (e.g., `world`, `world_nether`, `lobby`).
- **Container Worlds**: Worlds grouped by category or minigame stored at `<server-root>/spycore-worlds/<container>/<worldname>` (e.g., `spycore-worlds/Bedwars/Solo1`).
- **World Aliases**: Commands, plugins, and APIs refer to worlds via their clean alias (e.g., `Solo1`). SpyCore maps this alias to the physical path `spycore-worlds/Bedwars/Solo1`.

---

## 2. Requirements & Installation

- **Server Software**: Paper, Purpur, or Folia (API version `1.21+`).
- **Java Version**: Java 21 or newer.
- **Optional Dependencies**:
  - **PlaceholderAPI**: Enables `%spycore_world_name%` and `%spycore_world_alias%`.
  - **SpyInventories**: Automatically handles multi-world player inventory routing.
  - **SpyNetherPortals**: Enables dimension portal routing across custom VFS containers.

### Installation
1. Place `spycore-1.1.0.jar` into your server's `plugins/` directory.
2. Start or restart your server.
3. SpyCore will automatically scan for existing root worlds and create the `plugins/SpyCore/` directory.

---

## 3. Commands Reference

All SpyCore commands are accessed via the primary command `/spy`.

### Primary Subcommands

| Command | Permission | Description |
|---|---|---|
| `/spy help` | `spy.admin` | Displays the interactive command help menu with click-to-fill suggestions. |
| `/spy create world <name> <type> [flags]` | `spy.admin` / `spy.create` | Creates a new world in the server root folder. |
| `/spy create container <name>` | `spy.admin` | Creates a new directory container under `spycore-worlds/`. |
| `/spy container create <name>` | `spy.admin` | Alias for creating a new container. |
| `/spy container <con> create world <name> <type> [flags]` | `spy.admin` / `spy.create` | Creates a new world inside a specific container. |
| `/spy clone <src> <con> <target> [gen]` | `spy.admin` | Deep-clones a source world into a target container. Use `root` as container for root worlds. |
| `/spy load <world> [to <con>] [--generator <gen>] [--superflat]` | `spy.admin` | Loads a root world into memory, optionally moving it into a container. |
| `/spy load container <con> <world> [flags]` | `spy.admin` | Loads an existing world from a container into memory. |
| `/spy unload <world>` | `spy.admin` | Unloads a world from memory and saves chunk data. |
| `/spy remove <world>` | `spy.admin` | Unloads world and removes it from `worlds.yml`, preserving folder on disk. |
| `/spy delete <world>` | `spy.admin` / `spy.delete` | Unloads world and **permanently purges** its folder from disk. |
| `/spy delete container <name>` | `spy.admin` | Unloads all worlds in a container and permanently deletes the container folder. |
| `/spy move world <world> <targetContainer>` | `spy.admin` | Relocates a world folder into another container and re-maps all configs. |
| `/spy move container <con> <targetParent>` | `spy.admin` | Moves an entire container folder into another parent directory. |
| `/spy container list` | `spy.admin` | Lists all registered world aliases. |
| `/spy world tp [target] <world>` | `spy.admin` / `spy.tp` | Teleports yourself or targeted players/entities to a world. Supports target selectors. |
| `/spy world info` | `spy.admin` | Displays detailed technical diagnostics for the world you are standing in. |
| `/spy world setspawn` | `spy.admin` | Sets the spawn point of your current world to your exact position. |
| `/spy setspawn` | `spy.admin` | Quick shortcut for `/spy world setspawn`. |
| `/spy world gamerule <world> <rule> <val>` | `spy.admin` | Sets a vanilla game rule for a specified world. |
| `/spy world modify <world> set <feature> <val>` | `spy.admin` / `spy.world.modify` | Configures per-world gameplay rules (PVP, hunger, autoheal, etc.). |
| `/spy whitelist <add\|remove\|list> [world]` | `spy.admin` | Manages the list of worlds immune from hibernation. |
| `/spy wake <world>` | `spy.admin` | Manages manual wakeup of a hibernating or unloaded world. |
| `/spy template <tpl> <con> <world>` | `spy.admin` | Spawns a new world cloned from a predefined template. |
| `/spy tag <world> <key> <value>` | `spy.admin` | Attaches a metadata tag to a world. |
| `/spy find <con> <key> <value>` | `spy.admin` | Searches for worlds within a container matching a metadata key-value pair. |

---

### Command Flags & Parameters

When creating or loading worlds, the following flags are supported:
- **`<type>`**: Environment type (`normal`, `nether`, `the_end`).
- **`--seed <seed>`**: Custom world seed (e.g. `--seed 123456789` or `--seed random`).
- **`--generator <gen>`**: Custom chunk generator:
  - `voidgen`: Generates an empty void world with a platform-free spawn at `(0.5, 64, 0.5)`.
  - `lazy`: Bypasses Bukkit spawn-search chunk generation, setting spawn at `(0.5, 250, 0.5)`.
  - Any third-party plugin generator ID (e.g., `CityWorld`).
- **`--superflat`**: Sets world type to Minecraft flat-world generation.

#### Examples:
```bash
# Create a normal world in root
/spy create world survival normal

# Create a void world inside the Bedwars container
/spy container Bedwars create world Arena1 normal --generator voidgen

# Clone an arena into a disposable match
/spy clone Arena1 Matches Match_101 voidgen

# Modify world settings
/spy world modify Arena1 set pvp true
/spy world modify Arena1 set hunger false
/spy world modify Arena1 set difficulty HARD

# Teleport with target selectors
/spy world tp @a Arena1
/spy world tp @p lobby
```

---

## 4. Target Selector Support (`@s`, `@p`, `@a`, `@r`, `@e`)

SpyCore integrates Minecraft target selectors in player transportation commands:

| Selector | Meaning | Behavior in SpyCore |
|---|---|---|
| `@s` | Self (executing entity) | Teleports the commanding player to the specified world. |
| `@p` | Nearest player | Resolves the closest player to sender and teleports them. |
| `@r` | Random player | Selects a random player on the server and teleports them. |
| `@a` | All online players | Teleports all online players on the server into the world. |
| `@e` | All entities | Filters for players matching entity selectors (e.g., `@e[type=player]`). |

### Command Syntax Matrix
- `/spy world tp <world>`: Self-teleport (player only). Teleports caller to the world's safe location or last saved location.
- `/spy world tp <target> <world>`: Teleports the specified target(s) to the world. Usable by both players and console.

---

## 5. Permissions Reference

| Node | Description | Default |
|---|---|---|
| `spy.admin` | Grants access to all `/spy` commands and administrative features. | `op` |
| `spy.tp` | Allows teleportation to worlds via `/spy world tp`. | `op` |
| `spy.world.modify` | Allows modifying per-world rules via `/spy world modify`. | `op` |
| `spy.create` | Allows creating worlds and containers. | `op` |
| `spy.delete` | Allows deleting worlds and containers. | `op` |

---

## 6. World Settings & Gameplay Rules

Settings can be toggled per world using `/spy world modify <world> set <feature> <value>` or edited directly in `plugins/SpyCore/worlds.yml`.

| Feature | Values | Description |
|---|---|---|
| `pvp` | `true` / `false` | Enables or disables player-versus-player combat. |
| `mobspawn` | `true` / `false` | Enables or blocks natural mob spawning (preserves armor stands, items, and golems). |
| `hunger` | `true` / `false` | If `false`, player hunger level is permanently locked at 20. |
| `autoheal` | `true` / `false` | If `false`, disables health regeneration from saturated food levels. |
| `fly` | `true` / `false` | Enables survival flight for players inside the world. |
| `bedrespawn` | `true` / `false` | If `false`, overrides bed spawns and forces players to spawn at world spawn. |
| `timecycle` | `true` / `false` | Toggles daylight cycle (`doDaylightCycle`). |
| `weathercycle` | `true` / `false` | Toggles weather changes (`doWeatherCycle`). |
| `difficulty` | `PEACEFUL`, `EASY`, `NORMAL`, `HARD` | Sets the world difficulty level. |

---

## 7. Storage Files & Schemas

### `plugins/SpyCore/worlds.yml`
Main database for world registration, container mappings, and gameplay flags.
```yaml
hibernation-whitelist:
  - lobby
  - hub

worlds:
  root_lobby:
    container: root
    name: lobby
    hibernate: false
    environment: NORMAL
    superflat: false
    generator: null
    settings:
      pvp: false
      mobspawn: false
      hunger: false
      autoheal: true
      fly: true
      bedrespawn: false
      timecycle: false
      weathercycle: false
      difficulty: PEACEFUL
```

### `plugins/SpyCore/metadata.yml`
Persistent key-value tag registry for game engines and matchmakers.
```yaml
worlds:
  spycore-worlds__Bedwars__Solo1:
    mode: solo
    state: waiting
    max_players: 8
```

### `plugins/SpyCore/players/<UUID>/`
- **`profile.yml`**: Contains player last locations per world group (`last_locations.<group>`).
- **`<group>.yml`**: Stores inventory contents, armor, ender chest, health, level, exp, active potion effects, and gamemode for that world dimension group.

---

## 8. Developer API Guide

### Dependency Configuration
Add SpyCore to your plugin's `pom.xml`:
```xml
<dependency>
    <groupId>com.spygamingog</groupId>
    <artifactId>spycore</artifactId>
    <version>1.1.0</version>
    <scope>provided</scope>
</dependency>
```
Declare the dependency in your `plugin.yml`:
```yaml
depend: [SpyCore]
# or softdepend: [SpyCore]
```

### Accessing the API
All core features are accessible through `com.spygamingog.spycore.api.SpyAPI`:

```java
import com.spygamingog.spycore.api.SpyAPI;
import org.bukkit.World;
import org.bukkit.entity.Player;

// Get or load a world by alias
World world = SpyAPI.getWorld("Solo1", true);

// Safe teleportation
SpyAPI.teleportToSafeLocation(player, world);

// Create a world dynamically
World arena = SpyAPI.createWorld("Bedwars", "Arena5", World.Environment.NORMAL, "voidgen");

// Clone a template world for a match
World matchWorld = SpyAPI.cloneWorld("ArenaTemplate", "Matches", "Match_101", "voidgen");

// Link dimensions (Overworld + Nether share chat & tablist)
SpyAPI.linkWorlds(overworld, netherWorld);

// Metadata tags
SpyAPI.setWorldTag(arena, "gamemode", "4v4v4v4");
String mode = SpyAPI.getWorldTag(arena, "gamemode");
```

### Custom Events
Listen to SpyCore lifecycle events using standard Bukkit `@EventHandler`:
- **`SpyWorldCreateEvent`**: Fired when a world is created through SpyCore.
- **`SpyWorldLoadEvent`**: Fired when an existing world is loaded into memory.
- **`SpyWorldUnloadEvent`**: Fired before a world is unloaded.
- **`SpyPlayerRespawnEvent`**: Fired when SpyCore calculates a player's group respawn location. Allows overriding the target location.

---

## 9. PlaceholderAPI Expansions

| Placeholder | Output Example | Description |
|---|---|---|
| `%spycore_world_name%` | `spycore-worlds/Bedwars/Solo1` | Full technical world name / path. |
| `%spycore_world_alias%` | `Solo1` | Clean user-facing alias of the player's current world. |

---

## 10. License

SpyCore and its official companion plugins ([SpyInventories](https://github.com/spygamingog/SpyInventories), [SpyNetherPortals](https://github.com/spygamingog/SpyNetherPortals)) are licensed under the **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (CC BY-NC-SA 4.0)**.
- **Attribution**: You must give appropriate credit to SpyGamingOG.
- **NonCommercial**: You may not use the material for commercial purposes or monetization.
- **ShareAlike**: If you remix, transform, or build upon the material, you must distribute your contributions under the same license.
