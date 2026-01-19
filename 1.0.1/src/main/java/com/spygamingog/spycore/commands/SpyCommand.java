package com.spygamingog.spycore.commands;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.managers.WorldManager;
import com.spygamingog.spycore.managers.MetadataManager;
import com.spygamingog.spycore.managers.TemplateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpyCommand implements CommandExecutor, TabCompleter {
    private final SpyCore plugin;

    public SpyCommand(SpyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String mainArg = args[0].toLowerCase();

        switch (mainArg) {
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
            case "setspawn":
                String[] setSpawnArgs = new String[args.length + 1];
                setSpawnArgs[0] = "world";
                setSpawnArgs[1] = "setspawn";
                System.arraycopy(args, 1, setSpawnArgs, 2, args.length - 1);
                handleWorldCommand(sender, setSpawnArgs);
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
            case "template":
                handleTemplateCommand(sender, args);
                break;
            case "tag":
                handleTagCommand(sender, args);
                break;
            case "find":
                handleFindCommand(sender, args);
                break;
            default:
                sender.sendMessage("§cUnknown command. Use /spy help");
                break;
        }

        return true;
    }

    private void handleCreateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy create <worldname> <type> [generator]");
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
        } else {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /spy create <worldname> <normal|nether|the_end> [generator]");
                return;
            }
            String name = args[1];

            // Check if world already exists in the system
            if (plugin.getWorldManager().getWorldAliases().containsKey(name)) {
                sender.sendMessage("§cError: A world with the name '" + name + "' already exists.");
                return;
            }

            World.Environment env = parseEnv(args[2]);
            String generator = args.length >= 4 ? args[3] : null;
            
            sender.sendMessage("§aCreating root world " + name + " (" + env.name() + ")" + (generator != null ? " with generator " + generator : "") + "...");
            World world = plugin.getWorldManager().createWorld(null, name, env, generator);
            if (world != null) {
                sender.sendMessage("§aWorld created successfully.");
            } else {
                sender.sendMessage("§cFailed to create world. Check console for details.");
            }
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

        sender.sendMessage("§aCloning world '" + source + "' to container '" + targetContainer + "' as '" + targetName + "'...");
        World cloned = plugin.getWorldManager().cloneWorld(source, targetContainer, targetName, generator);
        if (cloned != null) {
            sender.sendMessage("§aSuccessfully cloned world to " + cloned.getName());
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
                    sender.sendMessage("§eFeatures: autoheal, hunger, fly, mobspawn, weathercycle, timecycle, difficulty");
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
                            org.bukkit.Difficulty.valueOf(value.toString());
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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can teleport.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /spy world tp <worldname>");
                    sender.sendMessage("§cUsage: /spy world tp <containerpath> <worldname>");
                    return;
                }

                if (args.length > 2 && args[2].equalsIgnoreCase("list")) {
                    sender.sendMessage("§6Available worlds:");
                    for (String worldAlias : plugin.getWorldManager().getWorldAliases().keySet()) {
                        sender.sendMessage("§e- " + worldAlias);
                    }
                    return;
                }
                
                World target = null;
                // 1. Try direct alias first (e.g., /spy world tp Solo)
                String lastArg = args[args.length - 1];
                target = plugin.getWorldManager().getWorld(lastArg);
                
                // 2. If not found, try interpreting args as a path (e.g., /spy world tp Bedwars Arenas Rooftop)
                if (target == null && args.length >= 4) {
                    StringBuilder pathBuilder = new StringBuilder();
                    for (int i = 2; i < args.length - 1; i++) {
                        if (pathBuilder.length() > 0) pathBuilder.append("/");
                        pathBuilder.append(args[i]);
                    }
                    String path = pathBuilder.toString();
                    String worldName = args[args.length - 1];
                    
                    // Try to find if this path+name combination is registered as an alias
                    for (java.util.Map.Entry<String, String> entry : plugin.getWorldManager().getWorldAliases().entrySet()) {
                        String fullPath = entry.getValue();
                        String expectedPath = "spycore-worlds/" + path + "/" + worldName;
                        if (fullPath.equalsIgnoreCase(expectedPath)) {
                            target = plugin.getWorldManager().getWorld(entry.getKey());
                            break;
                        }
                    }
                    
                    // 3. Last ditch effort: try to load it if it's not registered
                    if (target == null) {
                        target = plugin.getWorldManager().loadWorld(path, worldName);
                    }
                }

                if (target != null) {
                    Location spawn = target.getSpawnLocation();
                    player.teleport(spawn);
                    String tpAlias = plugin.getWorldManager().getAliasForWorld(target);
                    sender.sendMessage("§aTeleported to " + tpAlias);
                } else {
                    sender.sendMessage("§cWorld not found.");
                }
                break;
            case "setspawn":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set spawn.");
                    return;
                }
                World w = player.getWorld();
                w.setSpawnLocation(player.getLocation());
                sender.sendMessage("§aSpawn location set for " + w.getName());
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
                    sender.sendMessage("§aGamerule " + args[3] + " set to " + args[4] + " in " + grWorld.getName());
                } else {
                    sender.sendMessage("§cFailed to set gamerule.");
                }
                break;
        }
    }

    private void handleLoadCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy load <worldname> [to <container>] [generator]");
            sender.sendMessage("§cUsage: /spy load container <container> <world> [generator]");
            return;
        }

        if (args[1].equalsIgnoreCase("container")) {
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /spy load container <container> <world> [generator]");
                return;
            }
            String container = args[2];
            String world = args[3];
            String generator = args.length >= 5 ? args[4] : null;
            sender.sendMessage("§aLoading container world " + world + " from " + container + (generator != null ? " with generator " + generator : "") + "...");
            plugin.getWorldManager().loadWorld(container, world, generator);
            sender.sendMessage("§aLoad operation attempted.");
        } else {
            String world = args[1];
            String container = null;
            String generator = null;

            // Check for /spy load <world> to <container> [generator]
            if (args.length >= 4 && args[2].equalsIgnoreCase("to")) {
                container = args[3];
                generator = args.length >= 5 ? args[4] : null;
                
                sender.sendMessage("§aLoading world " + world + " and moving to container " + container + "...");
                // To move it, we first need to make sure it's known.
                // If it's not in worldAliases, it's likely in the root.
                if (!plugin.getWorldManager().getWorldAliases().containsKey(world)) {
                    // Try to load from root first to register it
                    plugin.getWorldManager().loadWorld(null, world, generator);
                }
                
                if (plugin.getWorldManager().moveWorld(world, container, world)) {
                    sender.sendMessage("§aWorld " + world + " loaded and moved to " + container);
                } else {
                    sender.sendMessage("§cFailed to load and move world.");
                }
            } else {
                generator = args.length >= 3 ? args[2] : null;
                sender.sendMessage("§aLoading root world " + world + (generator != null ? " with generator " + generator : "") + "...");
                plugin.getWorldManager().loadWorld(null, world, generator);
                sender.sendMessage("§aLoad operation attempted.");
            }
        }
    }

    private void handleContainerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spy container [con1] [con2] ... <action> <type> <name> [worldType] [generator]");
            return;
        }

        StringBuilder pathBuilder = new StringBuilder();
        int actionIdx = -1;
        String action = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.equals("create") || arg.equals("delete") || arg.equals("load") || arg.equals("unload")) {
                action = arg;
                actionIdx = i;
                break;
            }
            if (pathBuilder.length() > 0) pathBuilder.append("/");
            pathBuilder.append(args[i]);
        }

        if (action == null) {
            sender.sendMessage("§cNo action specified. Usage: /spy container [path...] <create|delete|load|unload> ...");
            return;
        }

        String containerPath = pathBuilder.toString();
        int remaining = args.length - 1 - actionIdx;

        switch (action) {
            case "create":
                if (remaining < 2) {
                    sender.sendMessage("§cUsage: /spy container [path...] create <container|world> <name> [worldType] [generator]");
                    return;
                }
                String type = args[actionIdx + 1].toLowerCase();
                String name = args[actionIdx + 2];

                if (type.equals("container")) {
                    String newPath = containerPath.isEmpty() ? name : containerPath + "/" + name;
                    if (plugin.getWorldManager().createContainer(newPath)) {
                        sender.sendMessage("§aContainer '" + newPath + "' created.");
                    } else {
                        sender.sendMessage("§cFailed to create container.");
                    }
                } else if (type.equals("world")) {
                    if (remaining < 3) {
                        sender.sendMessage("§cUsage: /spy container [path...] create world <name> <type> [generator]");
                        return;
                    }
                    World.Environment env = parseEnv(args[actionIdx + 3]);
                    String generator = remaining >= 4 ? args[actionIdx + 4] : null;
                    sender.sendMessage("§aCreating world " + name + " in container " + (containerPath.isEmpty() ? "root" : containerPath) + (generator != null ? " with generator " + generator : "") + "...");
                    plugin.getWorldManager().createWorld(containerPath.isEmpty() ? null : containerPath, name, env, generator);
                    sender.sendMessage("§aWorld created successfully.");
                }
                break;
            case "delete":
                if (remaining < 2) {
                    sender.sendMessage("§cUsage: /spy container [path...] delete <container|world> <name>");
                    return;
                }
                String delType = args[actionIdx + 1].toLowerCase();
                String delName = args[actionIdx + 2];

                if (delType.equals("container")) {
                    String fullPath = containerPath.isEmpty() ? delName : containerPath + "/" + delName;
                    if (plugin.getWorldManager().removeContainer(fullPath)) {
                        sender.sendMessage("§aContainer '" + fullPath + "' removed.");
                    } else {
                        sender.sendMessage("§cFailed to remove container.");
                    }
                } else {
                    String alias = containerPath.isEmpty() ? delName : containerPath + "/" + delName;
                    if (plugin.getWorldManager().unloadWorld(alias, true)) {
                        sender.sendMessage("§aWorld " + alias + " unloaded.");
                    } else {
                        sender.sendMessage("§cCould not unload world.");
                    }
                }
                break;
            case "load":
                if (remaining < 1) {
                    sender.sendMessage("§cUsage: /spy container [path...] load <worldname> [generator]");
                    return;
                }
                String loadName = args[actionIdx + 1];
                String loadGen = remaining >= 2 ? args[actionIdx + 2] : null;
                plugin.getWorldManager().loadWorld(containerPath.isEmpty() ? null : containerPath, loadName, loadGen);
                sender.sendMessage("§aLoad attempt for " + loadName + " in " + (containerPath.isEmpty() ? "root" : containerPath) + (loadGen != null ? " with generator " + loadGen : ""));
                break;
            case "unload":
                if (remaining < 1) {
                    sender.sendMessage("§cUsage: /spy container [path...] unload <worldname>");
                    return;
                }
                String unloadName = args[actionIdx + 1];
                String uAlias = containerPath.isEmpty() ? unloadName : containerPath + "/" + unloadName;
                plugin.getWorldManager().unloadWorld(uAlias, true);
                sender.sendMessage("§aUnload attempt for " + uAlias);
                break;
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
        java.util.Map<String, String> tags = new java.util.HashMap<>();
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

        addHelpLine(sender, "/spy create <world> <type> [gen]", "Create world in root.");
        addHelpLine(sender, "/spy clone <src> <con> <name> [gen]", "Clone world to container.");
        addHelpLine(sender, "/spy create container <name>", "Create a new container.");
        addHelpLine(sender, "/spy delete <world>", "Unload a world.");
        addHelpLine(sender, "/spy delete container <name>", "Wipe container from disk.");
        addHelpLine(sender, "/spy load <world> [gen]", "Load from root.");
        addHelpLine(sender, "/spy load container <con> <world> [gen]", "Load from container.");
        addHelpLine(sender, "/spy container <con> create <world> <type> [gen]", "Create in container.");
        addHelpLine(sender, "/spy world tp <name>", "Teleport to a world.");
        addHelpLine(sender, "/spy world info", "Show world diagnostics.");
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
            return filter(Arrays.asList("create", "clone", "delete", "remove", "unload", "move", "world", "setspawn", "load", "container", "template", "tag", "find", "help"), args[0]);
        }

        String sub = args[0].toLowerCase();

        // 1. Recursive Container Command Logic (Special Case)
        if (args.length >= 2 && sub.equals("container")) {
            StringBuilder currentPathBuilder = new StringBuilder();
            int actionIdx = -1;
            for (int i = 1; i < args.length - 1; i++) {
                String arg = args[i].toLowerCase();
                if (arg.equals("create") || arg.equals("delete") || arg.equals("load") || arg.equals("unload")) {
                    actionIdx = i;
                    break;
                }
                if (currentPathBuilder.length() > 0) currentPathBuilder.append("/");
                currentPathBuilder.append(args[i]);
            }

            String currentPath = currentPathBuilder.toString();
            String lastArg = args[args.length - 1];

            if (actionIdx == -1) {
                List<String> suggestions = new ArrayList<>();
                suggestions.addAll(getContainersInPath(currentPath));
                suggestions.addAll(Arrays.asList("create", "delete", "load", "unload"));
                return filter(suggestions, lastArg);
            } else {
                int relativeIdx = args.length - 1 - actionIdx;
                String action = args[actionIdx].toLowerCase();

                if (action.equals("create") || action.equals("delete")) {
                    if (relativeIdx == 1) return filter(Arrays.asList("container", "world"), lastArg);
                    if (relativeIdx == 2) {
                        String type = args[actionIdx + 1].toLowerCase();
                        if (type.equals("container") && action.equals("delete")) return filter(getContainersInPath(currentPath), lastArg);
                        if (type.equals("world") && action.equals("delete")) return filter(getWorldsInPath(currentPath), lastArg);
                        return new ArrayList<>();
                    }
                    if (relativeIdx == 3 && action.equals("create") && args[actionIdx + 1].equalsIgnoreCase("world")) {
                        return filter(Arrays.asList("normal", "nether", "the_end"), lastArg);
                    }
                    if (relativeIdx == 4 && action.equals("create") && args[actionIdx + 1].equalsIgnoreCase("world")) {
                        return filter(Arrays.asList("VoidGen"), lastArg);
                    }
                } else if (action.equals("load") || action.equals("unload")) {
                    if (relativeIdx == 1) return filter(getWorldsInPath(currentPath), lastArg);
                    if (relativeIdx == 2 && action.equals("load")) return filter(Arrays.asList("VoidGen"), lastArg);
                }
            }
            return new ArrayList<>();
        }

        // 2. World Command Tab Completion (Special Case)
        if (args.length >= 2 && sub.equals("world")) {
            if (args.length == 2) {
                return filter(Arrays.asList("tp", "setspawn", "gamerule", "info", "modify"), args[1]);
            }
            
            String worldSub = args[1].toLowerCase();
            if (worldSub.equals("tp")) {
                StringBuilder currentPathBuilder = new StringBuilder();
                for (int i = 2; i < args.length - 1; i++) {
                    if (currentPathBuilder.length() > 0) currentPathBuilder.append("/");
                    currentPathBuilder.append(args[i]);
                }
                String currentPath = currentPathBuilder.toString();
                String lastArg = args[args.length - 1];

                List<String> suggestions = new ArrayList<>();
                // 1. If it's the first argument for tp, show all registered aliases
                if (args.length == 3) {
                    suggestions.addAll(plugin.getWorldManager().getWorldAliases().keySet());
                }

                // 2. Show containers and worlds in the current path
                suggestions.addAll(getContainersInPath(currentPath));
                suggestions.addAll(getWorldsInPath(currentPath));
                
                return filter(suggestions, lastArg);
            }
            
            if (worldSub.equals("modify")) {
                if (args.length == 3) return filter(new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet()), args[2]);
                if (args.length == 4) return filter(Arrays.asList("set"), args[3]);
                if (args.length == 5) return filter(Arrays.asList("autoheal", "hunger", "fly", "mobspawn", "weathercycle", "timecycle", "difficulty"), args[4]);
                if (args.length == 6) {
                    if (args[4].equalsIgnoreCase("difficulty")) {
                        return filter(Arrays.asList("PEACEFUL", "EASY", "NORMAL", "HARD"), args[5].toUpperCase());
                    }
                    return filter(Arrays.asList("true", "false"), args[5]);
                }
            }

            if (worldSub.equals("gamerule")) {
                if (args.length == 3) return filter(new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet()), args[2]);
                if (args.length == 4) return filter(Arrays.stream(GameRule.values()).map(GameRule::getName).collect(Collectors.toList()), args[3]);
                if (args.length == 5) return filter(Arrays.asList("true", "false"), args[4]);
            }
            return new ArrayList<>();
        }

        // 3. Other standard commands
        if (args.length == 2) {
            switch (sub) {
                case "create":
                    return filter(Arrays.asList("container"), args[1]);
                case "delete":
                case "remove":
                case "unload":
                    return filter(new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet()), args[1]);
                case "clone":
                    return filter(new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet()), args[1]);
                case "load":
                    List<String> loadSuggestions = new ArrayList<>(Arrays.asList("container"));
                    // Only show worlds that are NOT loaded/registered
                    List<String> rootWorlds = getWorldsInPath("");
                    rootWorlds.removeAll(plugin.getWorldManager().getWorldAliases().keySet());
                    loadSuggestions.addAll(rootWorlds);
                    return filter(loadSuggestions, args[1]);
                case "move":
                    return filter(Arrays.asList("world", "container"), args[1]);
                case "template":
                    return filter(Arrays.asList("BedwarsSolo", "BedwarsDoubles"), args[1]); // Example templates
                case "tag":
                case "find":
                    return filter(new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet()), args[1]);
            }
        }

        if (args.length == 3) {
            switch (sub) {
                case "delete":
                    if (args[1].equalsIgnoreCase("container")) return suggestContainers("", args[2]);
                    if (args[1].equalsIgnoreCase("world")) return filter(new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet()), args[2]);
                    break;
                case "clone":
                    List<String> cloneSuggestions = getContainersInPath("");
                    cloneSuggestions.add("root");
                    return filter(cloneSuggestions, args[2]);
                case "load":
                    if (args[1].equalsIgnoreCase("container")) {
                        List<String> cons = getContainersInPath("");
                        cons.add("root");
                        return filter(cons, args[2]);
                    }
                    return filter(Arrays.asList("to", "VoidGen"), args[2]);
                case "move":
                    if (args[1].equalsIgnoreCase("world")) return filter(new ArrayList<>(plugin.getWorldManager().getWorldAliases().keySet()), args[2]);
                    if (args[1].equalsIgnoreCase("container")) {
                        // For moving a container, suggest containers in the path built so far
                        StringBuilder pathBuilder = new StringBuilder();
                        for (int i = 2; i < args.length - 1; i++) {
                            if (pathBuilder.length() > 0) pathBuilder.append("/");
                            pathBuilder.append(args[i]);
                        }
                        String path = pathBuilder.toString();
                        return filter(getContainersInPath(path), args[args.length - 1]);
                    }
                    break;
                case "template":
                    List<String> templateCons = getContainersInPath("");
                    templateCons.add("root");
                    return filter(templateCons, args[2]);
            }
        }

        if (args.length == 4) {
            switch (sub) {
                case "load":
                    if (args[1].equalsIgnoreCase("container")) return suggestWorldsInContainer(args[2], args[3]);
                    if (args[2].equalsIgnoreCase("to")) {
                        List<String> cons = getContainersInPath("");
                        cons.add("root");
                        return filter(cons, args[3]);
                    }
                    break;
                case "move":
                    // We are at the target container path argument (args[3])
                    StringBuilder movePathBuilder = new StringBuilder();
                    // If moving a container, the container name might have taken up multiple slots if we supported it, 
                    // but move command currently expects 4 args: /spy move <type> <name> <target>
                    // So args[3] is always the target.
                    List<String> moveTargetSuggestions = new ArrayList<>();
                    moveTargetSuggestions.add("root");
                    
                    // We can also suggest existing container paths
                    // Since it's just one argument for target, we might need a recursive way to suggest paths,
                    // but for now let's just suggest top-level containers and root.
                    moveTargetSuggestions.addAll(getContainersInPath(""));
                    return filter(moveTargetSuggestions, args[3]);
                case "template":
                    return new ArrayList<>(); // World name
            }
        }

        if (args.length == 5) {
            if (sub.equals("load") && args[1].equalsIgnoreCase("container")) return filter(Arrays.asList("VoidGen"), args[4]);
        }

        return null;
    }

    private List<String> getLoadedWorldsInPath(String path) {
        List<String> allWorlds = getWorldsInPath(path);
        return allWorlds.stream().filter(alias -> {
            String fullPath = plugin.getWorldManager().getWorldAliases().get(alias);
            if (fullPath == null) {
                // Check if it's a root world
                return org.bukkit.Bukkit.getWorld(alias) != null;
            }
            return org.bukkit.Bukkit.getWorld(fullPath) != null;
        }).collect(Collectors.toList());
    }

    private List<String> getContainersInPath(String path) {
        java.io.File containersBase = new java.io.File(plugin.getServer().getWorldContainer(), "spycore-worlds");
        java.io.File searchDir = path.isEmpty() ? containersBase : new java.io.File(containersBase, path.replace("/", java.io.File.separator));
        
        List<String> suggestions = new ArrayList<>();
        if (searchDir.exists() && searchDir.isDirectory()) {
            java.io.File[] files = searchDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isDirectory() && !new java.io.File(f, "level.dat").exists()) {
                        suggestions.add(f.getName());
                    }
                }
            }
        }
        return suggestions;
    }

    private List<String> getWorldsInPath(String path) {
        java.io.File containersBase = new java.io.File(plugin.getServer().getWorldContainer(), "spycore-worlds");
        java.io.File searchDir = path.isEmpty() ? plugin.getServer().getWorldContainer() : new java.io.File(containersBase, path.replace("/", java.io.File.separator));
        
        List<String> suggestions = new ArrayList<>();
        if (searchDir.exists() && searchDir.isDirectory()) {
            java.io.File[] files = searchDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isDirectory() && new java.io.File(f, "level.dat").exists()) {
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
