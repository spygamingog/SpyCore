# Changelog

### Added

## [1.0.4] - 2026-02-04

### Added
- **Hibernation System**: Added a robust hibernation system to optimize server resources by unloading unused worlds.
- **Hibernation Whitelist**: Introduced a whitelist configuration to prevent critical worlds from entering sleep mode.
- **World Creation**: Added support for seeds in `/spy create` (supports numeric seed or 'random' keyword).
- **Global Uniqueness**: Enforced strict global name uniqueness for worlds to prevent conflicts across containers.
- **Command Validation**: Hardened `/spy world tp` to strictly validate container paths and prevent loading containers as worlds.


### Changed
- **Default Configuration**: Hibernation is now **disabled by default** to ensure stability and prevent unexpected world unloading on new installations.
- **World Management**: Standardized world alias mapping and improved path resolution for nested world containers.
- **Respawn Logic**: Improved respawn handling to better support plugins with custom game logic (like SpyHunts) by respecting their event priorities.
- **Chat & Tab Isolation**: Enhanced isolation logic to ensure players in different game instances (sets of worlds) cannot see each other's chat or tab list entries.

### Fixed
- **Thread Safety**: Resolved a critical deadlock issue in `runSync` by ensuring strict main-thread execution for synchronous tasks.
