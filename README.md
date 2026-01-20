# SpyCore

[![Platform](https://img.shields.io/badge/platform-PaperMC%20%7C%20Spigot-blue)](https://papermc.io)
[![Version](https://img.shields.io/badge/minecraft-1.21.11-green)](https://www.minecraft.net)

**SpyCore** is a modern, lightweight world and server management framework for Minecraft networks. It provides an organized way to manage worlds using a container-based folder structure, while offering high-performance world lifecycle handling.

## 🚀 Features

- **📂 Containerized Management**: Group worlds into subdirectories (e.g., `spycore-worlds/Bedwars/Map1`).
- **🏷️ World Aliasing**: Use friendly names for complex file paths.
- **❄️ Hibernation System**: Automatically unloads idle worlds to save resources.
- **🔄 VFS (Virtual File System)**: Abstracted world loading, unloading, and management.
- **🏗️ Template System**: Deploy new worlds instantly from pre-defined templates.
- **🔍 Metadata System**: Tag and search worlds using flexible metadata.
- **🛡️ Safe Arrival**: Advanced algorithm to find the nearest safe spot for player teleports.

![SpyCore World directory structure](https://cdn.modrinth.com/data/cached_images/c2f907c8751f09a256634c616c31d7c016adaa60.gif)

## 🛠️ Installation

1. Download the latest `SpyCore.jar`.
2. Place it in your server's `plugins/` folder.
3. Restart the server.

## 📖 API for Developers

SpyCore is built with developers in mind. Use the `SpyAPI` to interact with the core:

```java
// Teleport a player to a safe location in a world
SpyAPI.teleportToSafeLocation(player, world);

// Load a world from a container
World world = SpyAPI.loadWorld("minigames", "desert_arena");
```

---
*Developed by SpyGamingOG*
