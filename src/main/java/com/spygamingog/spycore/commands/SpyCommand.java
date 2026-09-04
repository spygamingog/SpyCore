package com.spygamingog.spycore.commands;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.models.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SpyCommand implements CommandExecutor, TabCompleter {
    private final SpyCore plugin;

    public SpyCommand(SpyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("spy.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sendHelp(sender);
                break;
            case "create":
                handleCreateCommand(sender, args);
                break;
            case "clone":
                handleCloneCommand(sender, args);
                break;
            case "delete":
                handleDeleteCommand(sender, args);
                break;
            case "remove":
                handleRemoveCommand(sender, args);
                break;
            case "unload":
                handleUnloadCommand(sender, args);
                break;
            case "move":
                handleMoveCommand(sender, args);
                break;
            case "world":
                handleWorldCommand(sender, args);
                break;
            case "load":
                handleLoadCommand(sender, args);
                break;
            case "container":
                handleContainerCommand(sender, args);
                break;
            case "whitelist":
                handleWhitelistCommand(sender, args);
                break;
            case "wake":
                handleWakeCommand(sender, args);
                break;
            case "template":
                handleTemplateCommand(sender, args);
                break;
            case "tag":
                handleTagCommand(sender, args);
                break;
            case "find":
                handleFindCommand(sender, args);
                break;
            case "tp":
                // Quick alias for /spy world tp [target] <world>
                String[] shiftedArgs = new String[args.length + 1];
                shiftedArgs[0] = "world";
                shiftedArgs[1] = "tp";
                System.arraycopy(args, 1, shiftedArgs, 2, args.length - 1);
                handleWorldCommand(sender, shiftedArgs);
                break;
            case "setspawn":
                // Quick shortcut
                if (sender instanceof Player player) {
                    World w = player.getWorld();
                    w.setSpawnLocation(player.getLocation());
                    sender.sendMessage("§aSpawn location set for " + plugin.getWorldManager().getAliasForWorld(w));
                } else {
                    sender.sendMessage("§cOnly players can use setspawn shortcut.");
                }
                break;
            default:
                sender.sendMessage(Component.text("Unknown subcommand. Use /spy help", NamedTextColor.RED));
                break;
        }

        return true;
    }

    private void handleCreateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy create world <worldname> <type> [--seed <seed>] [--generator <gen>] [--superflat]");
            sender.sendMessage("§cUsage: /spy create container <containername>");
            return;
        }

        if (args[1].equalsIgnoreCase("container")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /spy create container <name>");
                return;
            }
            if (plugin.getWorldManager().createContainer(args[2])) {
                sender.sendMessage("§aContainer '" + args[2] + "' created successfully.");
            } else {
                sender.sendMessage("§cFailed to create container. It might already exist.");
            }
        } else if (args[1].equalsIgnoreCase("world")) {
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /spy create world <worldname> <normal|nether|the_end> [--seed <seed>] [--generator <gen>] [--superflat]");
                return;
            }
            String name = args[2];

            // Check if world already exists in the system (Global Uniqueness)
            if (plugin.getWorldManager().getWorldAliases().containsKey(name)) {
                sender.sendMessage("§cError: A world with the name '" + name + "' already exists.");
                return;
            }

            // Check if folder already exists (Prevent loading existing as new)
            if (new File(plugin.getServer().getWorldContainer(), name).exists()) {
                sender.sendMessage("§cError: A folder named '" + name + "' already exists in root.");
                return;
            }

            World.Environment env = parseEnv(args[3]);
            String generator = null;
            Long seed = null;
            boolean superflat = false;

            // Parse flags starting from index 4
            for (int i = 4; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                switch (arg) {
                    case "--seed":
                        if (i + 1 < args.length) {
                            String seedStr = args[++i];
                            if (!seedStr.equalsIgnoreCase("random")) {
                                try {
                                    seed = Long.parseLong(seedStr);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage("§cInvalid seed. Using random.");
                                }
                            }
                        }
                        break;
                    case "--generator":
                        if (i + 1 < args.length) {
                            generator = args[++i];
                        }
                        break;
                    case "--superflat":
                        superflat = true;
                        break;
                }
            }
            
            sender.sendMessage("§aCreating root world " + name + " (" + env.name() + ")" + 
                    (seed != null ? " with seed " + seed : "") + 
                    (generator != null ? " with generator " + generator : "") + 
                    (superflat ? " (superflat)" : "") + "...");
            
            World world = plugin.getWorldManager().createWorld(null, name, env, generator, seed, superflat);
            if (world != null) {
                sender.sendMessage("§aWorld created successfully.");
            } else {
                sender.sendMessage("§cFailed to create world. Check console for details.");
            }
        } else {
            sender.sendMessage("§cUsage: /spy create world <worldname> <type> [--seed <seed>] [--generator <gen>] [--superflat]");
            sender.sendMessage("§cUsage: /spy create container <containername>");
        }
    }

    private void handleCloneCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /spy clone <source> <targetContainer> <targetName> [generator]");
            sender.sendMessage("§eExample: /spy clone Solo Bedwars SoloClone VoidGen");
            sender.sendMessage("§eUse 'root' as targetContainer for root worlds.");
            return;
        }

        String source = args[1];
        String targetContainer = args[2];
        String targetName = args[3];
        String generator = args.length >= 5 ? args[4] : null;

        // Check if world already exists in the system (Global Uniqueness)
        if (plugin.getWorldManager().getWorldAliases().containsKey(targetName)) {
            sender.sendMessage("§cError: A world with the name '" + targetName + "' already exists.");
            return;
        }

        sender.sendMessage("§aCloning world '" + source + "' to container '" + targetContainer + "' as '" + targetName + "'...");
        World cloned = plugin.getWorldManager().cloneWorld(source, targetContainer, targetName, generator);
        if (cloned != null) {
            sender.sendMessage("§aSuccessfully cloned world to " + plugin.getWorldManager().getAliasForWorld(cloned));
        } else {
            sender.sendMessage("§cFailed to clone world. Check console for details.");
        }
    }

    private void handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy delete <worldname>");
            sender.sendMessage("§cUsage: /spy delete container <name>");
            return;
        }

        if (args[1].equalsIgnoreCase("container")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /spy delete container <name>");
                return;
            }
            if (plugin.getWorldManager().removeContainer(args[2])) {
                sender.sendMessage("§aContainer '" + args[2] + "' and all its contents removed from disk and config.");
            } else {
                sender.sendMessage("§cFailed to remove container. Make sure it exists.");
            }
        } else {
            String alias = args[1];
            if (plugin.getWorldManager().deleteWorld(alias)) {
                sender.sendMessage("§aWorld '" + alias + "' deleted from disk and config.");
            } else {
                sender.sendMessage("§cWorld '" + alias + "' not found or could not be deleted.");
            }
        }
    }

    private void handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy remove <worldname>");
            return;
        }

        String alias = args[1];
        if (plugin.getWorldManager().removeWorld(alias)) {
            sender.sendMessage("§aWorld '" + alias + "' removed from config and list, but files remain.");
        } else {
            sender.sendMessage("§cWorld '" + alias + "' not found in registered list.");
        }
    }

    private void handleUnloadCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy unload <worldname>");
            return;
        }

        String alias = args[1];
        if (plugin.getWorldManager().removeWorld(alias)) {
            sender.sendMessage("§aWorld '" + alias + "' has been unloaded and removed from active lists.");
        } else {
            sender.sendMessage("§cWorld '" + alias + "' is not loaded or not found.");
        }
    }

    private void handleMoveCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /spy move <world|container> <name> <targetContainerPath>");
            sender.sendMessage("§eExample: /spy move world Rooftop Bedwars/Arenas");
            sender.sendMessage("§eExample: /spy move container Arenas Bedwars");
            sender.sendMessage("§eUse 'root' to move to the root directory.");
            return;
        }

        String type = args[1].toLowerCase();
        String name = args[2];
        String target = args[3].replace(".", "/"); // Allow dots as separators too

        if (type.equals("world")) {
            if (plugin.getWorldManager().moveWorld(name, target, name)) {
                sender.sendMessage("§aWorld '" + name + "' moved to " + target);
            } else {
                sender.sendMessage("§cFailed to move world. Make sure it exists and target is valid.");
            }
        } else if (type.equals("container")) {
            if (plugin.getWorldManager().moveContainer(name, target)) {
                sender.sendMessage("§aContainer '" + name + "' moved to " + target);
            } else {
                sender.sendMessage("§cFailed to move container. Make sure it exists and target is valid.");
            }
        } else {
            sender.sendMessage("§cInvalid type. Use 'world' or 'container'.");
        }
    }

    private void handleWorldCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy world <tp|setspawn|gamerule|info|modify>");
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "modify":
                if (args.length < 5 || !args[3].equalsIgnoreCase("set")) {
                    sender.sendMessage("§cUsage: /spy world modify <world> set <feature> <value>");
                    sender.sendMessage("§eFeatures: autoheal, hunger, fly, mobspawn, pvp, bedrespawn, weathercycle, timecycle, difficulty");
                    return;
                }
                String modifyWorld = args[2];
                String feature = args[4].toLowerCase();
                String valueStr = args[5].toLowerCase();
                
                if (plugin.getWorldManager().getWorldAliases().containsKey(modifyWorld)) {
                    Object value;
                    if (valueStr.equals("true") || valueStr.equals("false")) {
                        value = Boolean.parseBoolean(valueStr);
                    } else if (feature.equalsIgnoreCase("difficulty")) {
                        try {
                            value = valueStr.toUpperCase();
                            Difficulty.valueOf(value.toString());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cInvalid difficulty. Use: PEACEFUL, EASY, NORMAL, HARD");
                            return;
                        }
                    } else {
                        value = valueStr;
                    }
                    
                    plugin.getWorldManager().setWorldSetting(modifyWorld, feature, value);
                    sender.sendMessage("§aSetting '" + feature + "' set to '" + value + "' for world " + modifyWorld);
                } else {
                    sender.sendMessage("§cWorld not found.");
                }
                break;
            case "info":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can check world info.");
                    return;
                }
                World current = player.getWorld();
                String alias = plugin.getWorldManager().getAliasForWorld(current);
                String container = plugin.getWorldManager().getContainerForWorld(current);
                
                sender.sendMessage("§8§m---------------------------------------");
                sender.sendMessage("§6§lSpyCore World Information");
                sender.sendMessage("§eName: §f" + alias);
                sender.sendMessage("§eContainer: §b" + container);
                sender.sendMessage("§ePlayers: §f" + current.getPlayers().size());
                sender.sendMessage("§eEnvironment: §f" + current.getEnvironment().name());
                sender.sendMessage("§8§m---------------------------------------");
                break;
            case "tp":
                // /spy world tp [target] <worldname>
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /spy world tp [target] <worldname>");
                    return;
                }

                List<Player> targets = new ArrayList<>();
                String worldName = "";

                // Check if args[2] is a selector or player name
                // Case 1: /spy world tp <worldname> (self teleport)
                if (args.length == 3 && sender instanceof Player) {
                    String input = args[2];
                    boolean isWorld = plugin.getWorldManager().getWorldAliases().containsKey(input) || Bukkit.getWorld(input) != null;
                    boolean isSelector = input.startsWith("@");

                    if (isWorld && !isSelector) {
                        targets.add((Player) sender);
                        worldName = input;
                    } else if (isSelector) {
                        sender.sendMessage("§cUsage: /spy world tp " + input + " <worldname>");
                        return;
                    } else {
                        // Check if it matches an online player
                        try {
                            List<Player> found = plugin.getServer().selectEntities(sender, input).stream()
                                .filter(e -> e instanceof Player)
                                .map(e -> (Player) e)
                                .collect(Collectors.toList());
                            
                            if (!found.isEmpty()) {
                                sender.sendMessage("§cUsage: /spy world tp " + input + " <worldname>");
                                return;
                            }
                        } catch (Exception ignored) {}

                        targets.add((Player) sender);
                        worldName = input;
                    }
                } 
                // Case 2: /spy world tp <target> <worldname>
                else if (args.length >= 4) {
                    try {
                        targets.addAll(plugin.getServer().selectEntities(sender, args[2]).stream()
                                .filter(e -> e instanceof Player)
                                .map(e -> (Player) e)
                                .collect(Collectors.toList()));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§cInvalid target selector: " + args[2]);
                        return;
                    }

                    if (targets.isEmpty()) {
                        sender.sendMessage("§cNo players found matching " + args[2]);
                        return;
                    }
                    worldName = args[3];
                } else {
                    // Args length is 3 but sender is console -> must specify target
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cUsage: /spy world tp <target> <worldname>");
                        return;
                    }
                    targets.add((Player) sender);
                    worldName = args[2];
                }

                World targetWorld = plugin.getWorldManager().getWorld(worldName);
                if (targetWorld == null) {
                    // Try to load it if it's a known alias but unloaded?
                    // Or maybe it's a container path? 
                    // For now, simple lookup.
                    // Check if it's a container path like "container/world"
                    if (worldName.contains("/")) {
                        String[] parts = worldName.split("/");
                        if (parts.length >= 2) {
                            // Try to load/get world from container
                             targetWorld = plugin.getWorldManager().getWorld(parts[parts.length-1]); // Simplified lookup
                        }
                    }
                }

                if (targetWorld != null) {
                    for (Player targetPlayer : targets) {
                        // Logic to find best location (last location or spawn)
                        Location targetLoc = targetWorld.getSpawnLocation();
                        
                        // Try to get last location from profile if available
                        // (Assuming PlayerProfile and getLastLocation logic exists and works)
                        try {
                             PlayerProfile profile = plugin.getPlayerManager().getProfile(targetPlayer.getUniqueId());
                             if (profile != null) {
                                 String wAlias = plugin.getWorldManager().getAliasForWorld(targetWorld);
                                 Location last = profile.getLastLocation(wAlias);
                                 if (last != null && last.getWorld() != null) {
                                     targetLoc = last;
                                 }
                             }
                        } catch (Exception ignored) {}

                        targetPlayer.teleport(targetLoc);
                        targetPlayer.setGameMode(org.bukkit.GameMode.SURVIVAL);
                        targetPlayer.sendMessage("§aTeleported to world " + targetWorld.getName());
                    }
                    sender.sendMessage("§aTeleported " + targets.size() + " player(s) to " + targetWorld.getName());
                } else {
                    sender.sendMessage("§cWorld '" + worldName + "' not found.");
                }
                break;
            case "setspawn":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set spawn.");
                    return;
                }
                World w = player.getWorld();
                w.setSpawnLocation(player.getLocation());
                sender.sendMessage("§aSpawn location set for " + plugin.getWorldManager().getAliasForWorld(w));
                break;
            case "gamerule":
                if (args.length < 5) {
                    sender.sendMessage("§cUsage: /spy world gamerule <world> <rule> <value>");
                    return;
                }
                World grWorld = plugin.getWorldManager().getWorld(args[2]);
                if (grWorld == null) {
                    sender.sendMessage("§cWorld not found.");
                    return;
                }
                GameRule rule = GameRule.getByName(args[3]);
                if (rule == null) {
                    sender.sendMessage("§cInvalid gamerule.");
                    return;
                }
                if (grWorld.setGameRuleValue(args[3], args[4])) {
                    sender.sendMessage("§aGamerule " + args[3] + " set to " + args[4] + " in " + plugin.getWorldManager().getAliasForWorld(grWorld));
                } else {
                    sender.sendMessage("§cFailed to set gamerule.");
                }
                break;
        }
    }

    private void handleLoadCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy load <worldname> [to <container>] [--generator <gen>] [--superflat]");
            sender.sendMessage("§cUsage: /spy load container <container> <world> [--generator <gen>] [--superflat]");
            return;
        }

        String generator = null;
        boolean superflat = false;

        // Parse flags
        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--generator") && i + 1 < args.length) {
                generator = args[++i];
            } else if (args[i].equalsIgnoreCase("--superflat")) {
                superflat = true;
            }
        }

        if (args[1].equalsIgnoreCase("container")) {
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /spy load container <container> <world> [--generator <gen>] [--superflat]");
                return;
            }
            String container = args[2];
            String world = args[3];
            
            // Re-parse flags for container subcommand which starts flags at index 4
            generator = null;
            superflat = false;
            for (int i = 4; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("--generator") && i + 1 < args.length) {
                    generator = args[++i];
                } else if (args[i].equalsIgnoreCase("--superflat")) {
                    superflat = true;
                }
            }

            if (plugin.getWorldManager().getWorldAliases().containsKey(world)) {
                sender.sendMessage("§aLoading world " + world + " from container " + container + "...");
                plugin.getWorldManager().loadWorld(container, world, generator);
            } else {
                sender.sendMessage("§eWorld " + world + " not in config. Discovering with default environment...");
                // Note: superflat is passed here but loadWorldInternal only uses it if no config exists
                plugin.getWorldManager().loadWorld(container, world, generator, World.Environment.NORMAL);
            }
            sender.sendMessage("§aLoad operation attempted.");
        } else {
            String world = args[1];
            String container = null;

            if (args.length >= 4 && args[2].equalsIgnoreCase("to")) {
                container = args[3];
                // Re-parse flags starting after 'to <container>'
                generator = null;
                superflat = false;
                for (int i = 4; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("--generator") && i + 1 < args.length) {
                        generator = args[++i];
                    } else if (args[i].equalsIgnoreCase("--superflat")) {
                        superflat = true;
                    }
                }
                
                sender.sendMessage("§aLoading world " + world + " and moving to container " + container + "...");
                if (!plugin.getWorldManager().getWorldAliases().containsKey(world)) {
                    plugin.getWorldManager().loadWorld(null, world, generator);
                }
                
                if (plugin.getWorldManager().moveWorld(world, container, world)) {
                    sender.sendMessage("§aWorld " + world + " loaded and moved to " + container);
                } else {
                    sender.sendMessage("§cFailed to load and move world.");
                }
            } else {
                sender.sendMessage("§aLoading root world " + world + (generator != null ? " with generator " + generator : "") + "...");
                plugin.getWorldManager().loadWorld(null, world, generator);
                sender.sendMessage("§aLoad operation attempted.");
            }
        }
    }

    private void handleContainerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy container <create|delete|move|list>");
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "create":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /spy container create <containername>");
                    sender.sendMessage("§cUsage: /spy container create world <container> <worldname> <type> [seed] [generator]");
                    return;
                }
                if (args[2].equalsIgnoreCase("world")) {
                    if (args.length < 6) {
                        sender.sendMessage("§cUsage: /spy container create world <container> <worldname> <type> [--seed <seed>] [--generator <gen>] [--superflat]");
                        return;
                    }
                    String container = args[3];
                    String name = args[4];
                    World.Environment env = parseEnv(args[5]);
                    String generator = null;
                    Long seed = null;
                    boolean superflat = false;

                    for (int i = 6; i < args.length; i++) {
                        String arg = args[i].toLowerCase();
                        switch (arg) {
                            case "--seed":
                                if (i + 1 < args.length) {
                                    String seedStr = args[++i];
                                    if (!seedStr.equalsIgnoreCase("random")) {
                                        try {
                                            seed = Long.parseLong(seedStr);
                                        } catch (NumberFormatException e) {
                                            sender.sendMessage("§cInvalid seed. Using random.");
                                        }
                                    }
                                }
                                break;
                            case "--generator":
                                if (i + 1 < args.length) {
                                    generator = args[++i];
                                }
                                break;
                            case "--superflat":
                                superflat = true;
                                break;
                        }
                    }

                    sender.sendMessage("§aCreating world " + name + " in container " + container + " (" + env.name() + ")" + 
                            (seed != null ? " with seed " + seed : "") + 
                            (generator != null ? " with generator " + generator : "") + 
                            (superflat ? " (superflat)" : "") + "...");
                    
                    World world = plugin.getWorldManager().createWorld(container, name, env, generator, seed, superflat);
                    if (world != null) {
                        sender.sendMessage("§aWorld created successfully in container.");
                    } else {
                        sender.sendMessage("§cFailed to create world. Check console.");
                    }
                } else {
                    if (plugin.getWorldManager().createContainer(args[2])) {
                        sender.sendMessage("§aContainer '" + args[2] + "' created successfully.");
                    } else {
                        sender.sendMessage("§cFailed to create container.");
                    }
                }
                break;
            case "delete":
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /spy container delete <container|world> <name>");
                    return;
                }
                String delType = args[2].toLowerCase();
                String delName = args[3];

                if (delType.equals("container")) {
                    if (plugin.getWorldManager().removeContainer(delName)) {
                        sender.sendMessage("§aContainer '" + delName + "' removed.");
                    } else {
                        sender.sendMessage("§cFailed to remove container.");
                    }
                } else if (delType.equals("world")) {
                    if (plugin.getWorldManager().deleteWorld(delName)) {
                        sender.sendMessage("§aWorld " + delName + " deleted.");
                    } else {
                        sender.sendMessage("§cCould not delete world.");
                    }
                }
                break;
            case "list":
                sender.sendMessage("§6Registered World Aliases:");
                for (String alias : plugin.getWorldManager().getWorldAliases().keySet()) {
                    sender.sendMessage("§e- " + alias);
                }
                break;
            case "move":
                handleMoveCommand(sender, args);
                break;
        }
    }

    private void handleWhitelistCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy whitelist <add|remove|list> [world]");
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "add":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /spy whitelist add <world>");
                    return;
                }
                String addAlias = args[2];
                plugin.getWorldManager().addWorldToWhitelist(addAlias);
                sender.sendMessage("§aWorld '" + addAlias + "' added to hibernation whitelist.");
                break;
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /spy whitelist remove <world>");
                    return;
                }
                String removeAlias = args[2];
                plugin.getWorldManager().removeWorldFromWhitelist(removeAlias);
                sender.sendMessage("§aWorld '" + removeAlias + "' removed from hibernation whitelist.");
                break;
            case "list":
                sender.sendMessage("§6Hibernation Whitelist:");
                for (String alias : plugin.getWorldManager().getHibernationWhitelist()) {
                    sender.sendMessage("§e- " + alias);
                }
                break;
            default:
                sender.sendMessage("§cUnknown subcommand. Use /spy whitelist <add|remove|list>");
                break;
        }
    }

    private void handleWakeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy wake <world>");
            return;
        }
        String alias = args[1];
        World w = plugin.getWorldManager().wakeWorld(alias);
        if (w != null) {
            sender.sendMessage("§aWorld '" + alias + "' has been woken up from hibernation.");
        } else {
            sender.sendMessage("§cCould not wake up world '" + alias + "'. Make sure it exists.");
        }
    }

    private void handleTemplateCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /spy template <template> <container> <world>");
            return;
        }
        plugin.getTemplateManager().createFromTemplate(args[1], args[2], args[3]);
        sender.sendMessage("§aCreated world from template.");
    }

    private void handleTagCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /spy tag <world> <key> <val>");
            return;
        }
        World w = plugin.getWorldManager().getWorld(args[1]);
        if (w != null) {
            plugin.getMetadataManager().setTag(w, args[2], args[3]);
            sender.sendMessage("§aTagged " + args[1]);
        }
    }

    private void handleFindCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /spy find <container> <key> <val>");
            return;
        }
        Map<String, String> tags = new HashMap<>();
        tags.put(args[2], args[3]);
        List<World> found = plugin.getMetadataManager().findWorlds(args[1], tags);
        sender.sendMessage("§aFound: " + found.stream().map(World::getName).collect(Collectors.joining(", ")));
    }

    private World.Environment parseEnv(String s) {
        try {
            return World.Environment.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return World.Environment.NORMAL;
        }
    }

    private void sendHelp(CommandSender sender) {
        Component header = Component.text("--- SpyCore Ultimate Help ---", NamedTextColor.GOLD, TextDecoration.BOLD);
        sender.sendMessage(header);

        addHelpLine(sender, "/spy create <world> <type> [--seed <seed>] [--generator <gen>] [--superflat]", "Create world in root.");
        addHelpLine(sender, "/spy clone <src> <con> <name> [gen]", "Clone world to container.");
        addHelpLine(sender, "/spy create container <name>", "Create a new container.");
        addHelpLine(sender, "/spy delete <world>", "Unload a world.");
        addHelpLine(sender, "/spy delete container <name>", "Wipe container from disk.");
        addHelpLine(sender, "/spy load <world> [gen]", "Load from root.");
        addHelpLine(sender, "/spy load container <con> <world> [gen]", "Load from container.");
        addHelpLine(sender, "/spy container <con> create world <world> <type> [--seed <seed>] [--generator <gen>] [--superflat]", "Create in container.");
        addHelpLine(sender, "/spy world tp <name>", "Teleport to a world.");
        addHelpLine(sender, "/spy world info", "Show world diagnostics.");
        addHelpLine(sender, "/spy whitelist <add|remove|list> <world>", "Manage hibernation whitelist.");
        addHelpLine(sender, "/spy wake <world>", "Wake world from hibernation.");
        addHelpLine(sender, "/spy template <tpl> <con> <world>", "Clone from template.");
        
        sender.sendMessage(Component.text("-----------------------------", NamedTextColor.GOLD));
    }

    private void addHelpLine(CommandSender sender, String usage, String description) {
        Component line = Component.text("» ", NamedTextColor.GRAY)
                .append(Component.text(usage, NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(Component.text(description, NamedTextColor.AQUA)))
                        .clickEvent(ClickEvent.suggestCommand(usage.split(" ").length > 2 ? usage.substring(0, usage.indexOf(" ", usage.indexOf(" ") + 1)) + " " : usage + " ")))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Info", NamedTextColor.GRAY, TextDecoration.ITALIC));
        sender.sendMessage(line);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "clone", "delete", "remove", "unload", "move", "setspawn", "world", "tp", "load", "container", "template", "tag", "find", "whitelist", "wake", "help")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "tp":
                    List<String> tpOptions = new ArrayList<>();
                    tpOptions.add("@s");
                    tpOptions.add("@p");
                    tpOptions.add("@a");
                    tpOptions.add("@r");
                    tpOptions.add("@e");
                    tpOptions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                    tpOptions.addAll(plugin.getWorldManager().getWorldAliases().keySet());
                    return tpOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "create":
                    return Arrays.asList("world", "container").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "delete":
                case "unload":
                case "remove":
                case "setspawn":
                case "load":
                case "wake":
                    List<String> loadOptions = new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet());
                    loadOptions.add("container");
                    return loadOptions.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "world":
                    return Arrays.asList("tp", "setspawn", "gamerule", "info", "modify").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "container":
                    return Arrays.asList("create", "delete", "move", "list").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "tp":
                    return plugin.getWorldManager().getWorldAliases().keySet().stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                case "load":
                    if (args[1].equalsIgnoreCase("container")) {
                        return getContainersInPath("").stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                    }
                    break;
                case "create":
                    if (args[1].equalsIgnoreCase("world")) return null; // <worldname>
                    break;
                case "world":
                    if (args[1].equalsIgnoreCase("tp")) {
                        List<String> options = new ArrayList<>();
                        options.add("@s");
                        options.add("@p");
                        options.add("@a");
                        options.add("@r");
                        options.add("@e");
                        options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        options.addAll(plugin.getWorldManager().getWorldAliases().keySet());
                        return options.stream().filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                    }
                    if (args[1].equalsIgnoreCase("setspawn") || args[1].equalsIgnoreCase("modify") || args[1].equalsIgnoreCase("gamerule")) {
                        return plugin.getWorldManager().getWorldAliases().keySet().stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                    }
                    break;
                case "container":
                    if (args[1].equalsIgnoreCase("create")) {
                        return Arrays.asList("world").stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                    }
                    break;
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("tp")) {
                return plugin.getWorldManager().getWorldAliases().keySet().stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("load") && args[1].equalsIgnoreCase("container")) {
                return getWorldsInPath(args[2]).stream().filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
            }
        }

        if (args.length >= 4) {
            String mainArg = args[0].toLowerCase();
            String subArg = args[1].toLowerCase();

            if ((mainArg.equals("create") && subArg.equals("world")) || 
                (mainArg.equals("load") && !args[1].equalsIgnoreCase("container")) ||
                (mainArg.equals("clone"))) {
                
                // Position of flag parsing depends on command
                int flagStart = 4; // Default for create world and clone
                if (mainArg.equals("load")) {
                    flagStart = args.length >= 3 && args[2].equalsIgnoreCase("to") ? 4 : 2;
                }

                // If the previous argument was --generator, suggest generators
                if (args.length > flagStart) {
                    String prev = args[args.length - 2].toLowerCase();
                    if (prev.equals("--generator")) {
                        return Arrays.asList("voidgen", "lazy").stream()
                                .filter(s -> s.startsWith(args[args.length - 1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }

                // Positional arguments
                if (mainArg.equals("create") && args.length == 4) {
                    return Arrays.asList("normal", "nether", "the_end").stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }

                // Suggest flags
                List<String> flags = new ArrayList<>();
                flags.add("--seed");
                flags.add("--generator");
                flags.add("--superflat");

                String lastArg = args[args.length - 1].toLowerCase();
                return flags.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
            }

            if (mainArg.equals("container") && subArg.equals("create") && args.length >= 5) {
                 if (args[2].equalsIgnoreCase("world")) {
                     if (args.length == 5) return null; // <worldname>
                     if (args.length == 6) {
                         return Arrays.asList("normal", "nether", "the_end").stream()
                                 .filter(s -> s.startsWith(args[5].toLowerCase())).collect(Collectors.toList());
                     }

                     List<String> flags = new ArrayList<>();
                     flags.add("--seed");
                     flags.add("--generator");
                     flags.add("--superflat");

                     String lastArg = args[args.length - 1].toLowerCase();
                     String prevArg = args[args.length - 2].toLowerCase();

                     if (prevArg.equals("--seed")) return Arrays.asList("random");
                     if (prevArg.equals("--generator")) return Arrays.asList("voidgen", "lazy");

                     return flags.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
                 }
            }
        }
        
        // Add more deep completion as needed...
        return null;
    }

    private List<String> getLoadedWorldsInPath(String path) {
        List<String> allWorlds = getWorldsInPath(path);
        return allWorlds.stream().filter(alias -> {
            String fullPath = plugin.getWorldManager().getWorldAliases().get(alias);
            if (fullPath == null) {
                // Check if it's a root world
                return Bukkit.getWorld(alias) != null;
            }
            return Bukkit.getWorld(fullPath) != null;
        }).collect(Collectors.toList());
    }

    private List<String> getContainersInPath(String path) {
        File containersBase = new File(plugin.getServer().getWorldContainer(), "spycore-worlds");
        File searchDir = path.isEmpty() ? containersBase : new File(containersBase, path.replace("/", File.separator));
        
        List<String> suggestions = new ArrayList<>();
        if (searchDir.exists() && searchDir.isDirectory()) {
            File[] files = searchDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && !new File(f, "level.dat").exists()) {
                        suggestions.add(f.getName());
                    }
                }
            }
        }
        return suggestions;
    }

    private List<String> getWorldsInPath(String path) {
        File containersBase = new File(plugin.getServer().getWorldContainer(), "spycore-worlds");
        File searchDir = path.isEmpty() ? plugin.getServer().getWorldContainer() : new File(containersBase, path.replace("/", File.separator));
        
        List<String> suggestions = new ArrayList<>();
        if (searchDir.exists() && searchDir.isDirectory()) {
            File[] files = searchDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && new File(f, "level.dat").exists()) {
                        suggestions.add(f.getName());
                    }
                }
            }
        }
        return suggestions;
    }

    private List<String> suggestContainers(String parentPath, String input) {
        return filter(getContainersInPath(parentPath), input);
    }

    private List<String> suggestWorldsInContainer(String containerPath, String input) {
        return filter(getWorldsInPath(containerPath), input);
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
