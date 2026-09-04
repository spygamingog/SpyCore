# Changelog - SpyCore

All notable changes to the SpyCore project will be documented in this file.

## [1.1.1] - 2026-09-05

### Fixed
- **Teleport Dimension Bounce / Infinite Toggle**: Fixed bug where repeatedly executing `/spy world tp <world>` ping-ponged the player between Overworld and Nether. Teleportation now strictly validates that destination coordinates match the requested target world.
- **Per-World Location Memory**: Updated `PlayerManager#trackLocation` to store last player positions keyed by specific world alias rather than world group base name, preventing linked Nether/End coordinates from overwriting Overworld locations.
- **Hardcoded Gamemode Removal**: Removed hardcoded `setGameMode(SURVIVAL)` in `/spy world tp` and `/spy tp`, preserving the player's active gamemode (Creative, Spectator, Adventure) across teleports.
- **Cross-Group Inventory Load Guard**: Restricted `onWorldChange` inventory reloading to actual cross-group transitions, preventing in-memory player state from being overwritten when traveling between linked dimensions in the same set.

## [1.1.0] - 2026-09-04

### Added
- **Native Target Operators**: Expanded tab completion to suggest `@s` and `@e` for `/spy world tp`.
- **Root Command Alias**: Added `/spy tp [target] <world>` directly matching `plugin.yml`'s `spy.tp` permission node.
- **Automated Hibernation Scheduler**: Activated a recurring 60-second world hibernation checker in the plugin lifecycle so inactive worlds automatically unload.

### Fixed
- **Player Death Inventory Loss**: Fixed critical bug in `onPlayerRespawn` where a dead player's empty inventory was saved to disk, permanently wiping saved world items.
- **Target Operator Name Collision**: Resolved command ambiguity where `/spy world tp <world>` failed if an online player's name matched a world alias.
- **PVP Projectile Bypass**: Fixed projectile damage (arrows, tridents, splash potions) ignoring `pvp: false` in `WorldSettingsListener`.
- **Spawn Point Hijacking**: Removed erroneous check that reset world spawns whenever a player landed from altitudes Y >= 249.
- **Coordinate ClassCastException**: Fixed `ClassCastException` in `PlayerProfile` when reading whole-number coordinates from YAML files.
- **Metadata Path Corruption**: Replaced fragile dot replacement with structured tokens to prevent dotted world names from being mangled.
- **Event Handler Registration**: Added explicit `HandlerList` declarations to `SpyWorldCreateEvent`, `SpyWorldLoadEvent`, and `SpyWorldUnloadEvent`.
- **Thread-Safe Async Inventory Serialization**: Cloned `ItemStack[]` arrays synchronously on the main thread before dispatching to async file I/O.

### Changed & Performance
- **Java 21/25 Build Stability**: Eliminated fragile annotation processing dependencies in favor of standard native Java getters and setters.
- **Clean Packaging**: Configured `maven-shade-plugin` with clean packaging to prevent `dependency-reduced-pom.xml` clutter, and pruned unused dependencies.

## [1.0.9] - 2026-02-20

### Changed
- **Inventory Conflict Resolution**: Disabled SpyCore's internal inventory management automatically when `SpyInventories` is detected. This prevents race conditions and data corruption during world transitions.

## [1.0.8] - 2026-02-14

### Added
- **PlaceholderAPI Expansion**: Integrated internal PAPI expansion for core features.
- **World Alias Placeholders**: Added `%spycore_world_alias%` to show clean world names (aliases) instead of nested paths.
- **World Ignoring System**: Added `ignored-worlds` and `ignored-suffixes` to `config.yml`. This allows the VFS and hibernation systems to bypass specific worlds (e.g., lobby, hub) or worlds matching certain patterns.
- **Improved Hibernation Logic**: Enhanced the auto-sleep cycle to strictly respect the new ignore lists, preventing accidental hibernation of critical infrastructure worlds.
- **Enhanced VFS Cleaning**: Automated removal of `uid.dat` during world cloning/importing to resolve UUID conflict errors permanently.

### Fixed
- **Redundant Expansion Cleanup**: Removed redundant expansion code that caused circular dependencies and build failures.

### Changed
- Refactored internal world loading sequence for better stability under high concurrency.
- Optimized metadata storage to reduce memory footprint on long-running instances.

## [1.0.7] - 2026-02-12

### Added
- **Advanced World Management**: Implemented "Read-Only" style template cloning for temporary match worlds.
- **No-Save Flag**: Temporary worlds now default to `AutoSave=false` and `KeepSpawnInMemory=false` to reduce Disk I/O and memory usage.
- **Auto-Cleanup**: Enhanced shutdown sequence to automatically unload and delete temporary match worlds (`temp_match_` container).
- **VFS Error Resilience**: Added disk-based fallback for worlds missing from configuration, resolving "VFS: World load failed" errors.
- **World Aliasing**: Added support for world aliases to maintain consistent references across different match instances.

## [1.0.6] - 2026-02-11

### Fixed
- Resolved syntax errors in `SpyCommand.java` regarding duplicate `case "load"` statements.
- Fixed a type mismatch where `List<String>` was incorrectly being converted to `boolean` in the command handler.
- Verified and added missing imports in `SpyCommand.java` for improved stability.
- Optimized tab completion logic for `/spy load` and `/spy world tp` commands.

### Changed
- Improved argument length validation in `onTabComplete` to prevent potential NullPointerExceptions.
- Refactored `SpyCommand` to strictly follow Bukkit `CommandExecutor` and `TabCompleter` contracts.

## [1.0.5] - Earlier
- Initial stable release of the container-based world management system.
- Implementation of the hibernation and metadata systems.
