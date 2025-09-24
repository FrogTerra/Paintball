package me.FrogTerra.paintball.command;

import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.arena.Arena;
import me.FrogTerra.paintball.utility.MessageUtils;
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

/**
 * Command handler for arena management
 */
public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final Paintball plugin;

    public ArenaCommand(Paintball plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("paintball.admin")) {
            player.sendMessage(MessageUtils.parseMessage("<red>You don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            this.sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena create <name> [schematic-file]"));
                    return true;
                }
                this.openArenaCreationGUI(player, args[1]);
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena delete <name>"));
                    return true;
                }
                this.deleteArena(player, args[1]);
            }
            case "list" -> this.listArenas(player);
            case "edit" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena edit <name>"));
                    return true;
                }
                this.openArenaManagementGUI(player, args[1]);
            }
            case "info" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena info <name>"));
                    return true;
                }
                this.showArenaInfo(player, args[1]);
            }
            case "preload" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena preload <name>"));
                    return true;
                }
                this.preloadArena(player, args[1]);
            }
            case "unload" -> this.unloadArena(player);
            case "reload" -> this.reloadArenas(player);
            default -> this.sendHelpMessage(player);
        }

        return true;
    }

    private void openArenaCreationGUI(final Player player, final String name) {
        new me.FrogTerra.paintball.gui.ArenaCreationGUI(name).open(player);
    }

    private void openArenaManagementGUI(final Player player, final String name) {
        if (!this.plugin.getArenaManager().getArenas().containsKey(name.toLowerCase())) {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + name + "' not found!"));
            return;
        }
        new me.FrogTerra.paintball.gui.ArenaCreationGUI(name).open(player);
    }

    private void deleteArena(final Player player, final String name) {
        if (this.plugin.getArenaManager().deleteArena(name)) {
            player.sendMessage(MessageUtils.parseMessage("<green>Arena '" + name + "' deleted successfully!"));
        } else {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + name + "' not found!"));
        }
    }

    private void listArenas(final Player player) {
        final var arenas = this.plugin.getArenaManager().getArenas();
        
        if (arenas.isEmpty()) {
            player.sendMessage(MessageUtils.parseMessage("<yellow>No arenas found."));
            return;
        }

        player.sendMessage(MessageUtils.parseMessage("<green><bold>Available Arenas:"));
        for (final Arena arena : arenas.values()) {
            final String status = arena.isEnabled() ? "<green>✓" : "<red>✗";
            final String spawns = arena.getTotalSpawns() + " spawns";
            final String flags = arena.getTotalFlagSpawns() > 0 ? ", " + arena.getTotalFlagSpawns() + " flags" : "";
            
            player.sendMessage(MessageUtils.parseMessage(
                status + " <yellow>" + arena.getName() + " <gray>(" + spawns + flags + ")"
            ));
        }
    }

    private void showArenaInfo(final Player player, final String name) {
        final Arena arena = this.plugin.getArenaManager().getArenas().get(name.toLowerCase());
        
        if (arena == null) {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + name + "' not found!"));
            return;
        }

        player.sendMessage(MessageUtils.parseMessage("<green><bold>Arena Information: " + arena.getName()));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Schematic: <white>" + arena.getSchematicFile()));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Status: " + (arena.isEnabled() ? "<green>Enabled" : "<red>Disabled")));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Compatible Gamemodes: <white>" + 
            (arena.getCompatibleGameModes() != null ? 
                arena.getCompatibleGameModes().stream()
                    .map(gm -> gm.getDisplayName())
                    .collect(Collectors.joining(", ")) : "None")));
        
        player.sendMessage(MessageUtils.parseMessage("<yellow>Spawn Points:"));
        player.sendMessage(MessageUtils.parseMessage("  <red>Red Team: <white>" + arena.getRedSpawns().size()));
        player.sendMessage(MessageUtils.parseMessage("  <blue>Blue Team: <white>" + arena.getBlueSpawns().size()));
        player.sendMessage(MessageUtils.parseMessage("  <white>Free For All: <white>" + arena.getFreeForAllSpawns().size()));
        
        if (arena.getTotalFlagSpawns() > 0) {
            player.sendMessage(MessageUtils.parseMessage("<yellow>Flag Spawns:"));
            player.sendMessage(MessageUtils.parseMessage("  <red>Red Flags: <white>" + arena.getRedFlagSpawns().size()));
            player.sendMessage(MessageUtils.parseMessage("  <blue>Blue Flags: <white>" + arena.getBlueFlagSpawns().size()));
        }
        
        player.sendMessage(MessageUtils.parseMessage("<yellow>Valid: " + (arena.isValid() ? "<green>Yes" : "<red>No")));
    }

    private void preloadArena(final Player player, final String name) {
        final Arena arena = this.plugin.getArenaManager().getArenas().get(name.toLowerCase());
        
        if (arena == null) {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + name + "' not found!"));
            return;
        }

        player.sendMessage(MessageUtils.parseMessage("<yellow>Preloading arena: " + name + "..."));
        
        this.plugin.getArenaManager().preloadArena(name).thenAccept(success -> {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (success) {
                    player.sendMessage(MessageUtils.parseMessage("<green>Successfully preloaded arena: " + name));
                } else {
                    player.sendMessage(MessageUtils.parseMessage("<red>Failed to preload arena: " + name));
                }
            });
        });
    }

    private void unloadArena(final Player player) {
        this.plugin.getArenaManager().clearPreloadedArena().thenAccept(success -> {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (success) {
                    player.sendMessage(MessageUtils.parseMessage("<green>Successfully unloaded preloaded arena"));
                } else {
                    player.sendMessage(MessageUtils.parseMessage("<yellow>No arena was preloaded"));
                }
            });
        });
    }

    private void reloadArenas(final Player player) {
        // Save current arenas and reload from file
        this.plugin.getArenaManager().saveArenas();
        player.sendMessage(MessageUtils.parseMessage("<green>Arena configuration reloaded!"));
    }

    private void sendHelpMessage(final Player player) {
        player.sendMessage(MessageUtils.parseMessage("<green><bold>Arena Commands:"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena create <name> [schematic] <gray>- Create/manage an arena"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena delete <name> <gray>- Delete an arena"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena list <gray>- List all arenas"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena edit <name> <gray>- Manage arena settings"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena info <name> <gray>- Show arena information"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena preload <name> <gray>- Preload arena to reduce game start lag"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena unload <gray>- Unload currently preloaded arena"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena reload <gray>- Reload arena configuration"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "list", "edit", "info", "preload", "unload", "reload")
                    .stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("edit") || 
                                 args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("preload"))) {
            return this.plugin.getArenaManager().getArenas().keySet()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}