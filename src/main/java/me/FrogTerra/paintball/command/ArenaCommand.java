package me.FrogTerra.paintball.command;

import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.arena.Arena;
import me.FrogTerra.paintball.game.Gamemode;
import me.FrogTerra.paintball.gui.ArenaManagementGUI;
import me.FrogTerra.paintball.utility.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for arena management
 */
public class ArenaCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(MessageUtils.parseMessage("<red>You must be an operator to use arena commands."));
            return true;
        }

        if (args.length == 0) {
            this.sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena create <name> <schematic-file>"));
                    return true;
                }
                this.createArena(player, args[1], args[2]);
            }
            case "edit" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena edit <name>"));
                    return true;
                }
                this.editArena(player, args[1]);
            }
            case "force" -> {
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena force <arena> <gamemode>"));
                    return true;
                }
                this.forceArena(player, args[1], args[2]);
            }
            case "random" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena random <gamemode>"));
                    return true;
                }
                this.randomArena(player, args[1]);
            }
            case "list" -> this.listArenas(player);
            case "info" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Usage: /arena info <name>"));
                    return true;
                }
                this.showArenaInfo(player, args[1]);
            }
            case "reload" -> this.reloadArenas(player);
            default -> this.sendHelpMessage(player);
        }

        return true;
    }

    private void createArena(final Player player, final String name, final String schematicFile) {
        // Check if schematic exists
        final File schematicsFolder = new File(Paintball.getPlugin().getDataFolder(), "schematics");
        final File schematicFileObj = new File(schematicsFolder, schematicFile);
        
        if (!schematicFileObj.exists()) {
            player.sendMessage(MessageUtils.parseMessage("<red>Schematic file not found: " + schematicFile));
            player.sendMessage(MessageUtils.parseMessage("<yellow>Available schematics: " + 
                this.getAvailableSchematics().stream().collect(Collectors.joining(", "))));
            return;
        }

        if (Paintball.getPlugin().getArenaManager().createArena(name, schematicFile)) {
            player.sendMessage(MessageUtils.parseMessage("<green>Arena '" + name + "' created successfully!"));
            player.sendMessage(MessageUtils.parseMessage("<yellow>Opening editing GUI to configure the arena..."));
            
            // Open editing GUI immediately after creation
            final ArenaManagementGUI gui = new ArenaManagementGUI(name);
            gui.open(player);
        } else {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + name + "' already exists!"));
        }
    }

    private void editArena(final Player player, final String name) {
        final Arena arena = Paintball.getPlugin().getArenaManager().getArenas().get(name.toLowerCase());
        
        if (arena == null) {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + name + "' not found!"));
            return;
        }

        final ArenaManagementGUI gui = new ArenaManagementGUI(name);
        gui.open(player);
    }

    private void forceArena(final Player player, final String arenaName, final String gamemodeName) {
        final Arena arena = Paintball.getPlugin().getArenaManager().getArenas().get(arenaName.toLowerCase());
        
        if (arena == null) {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + arenaName + "' not found!"));
            return;
        }

        final Gamemode gamemode = Gamemode.fromString(gamemodeName);
        
        if (gamemode == null) {
            player.sendMessage(MessageUtils.parseMessage("<red>Invalid gamemode: " + gamemodeName));
            player.sendMessage(MessageUtils.parseMessage("<yellow>Available gamemodes: " + 
                Arrays.stream(Gamemode.values()).map(Gamemode::getDisplayName).collect(Collectors.joining(", "))));
            return;
        }

        if (!arena.isCompatible(gamemode)) {
            player.sendMessage(MessageUtils.parseMessage("<red>Arena '" + arenaName + "' is not compatible with " + gamemode.getDisplayName()));
            return;
        }

        // Set forced arena and gamemode for next game
        Paintball.getPlugin().getLobbyManager().setNextGame(gamemode, arena);
        player.sendMessage(MessageUtils.parseMessage("<green>Forced next game: <yellow>" + gamemode.getDisplayName() + 
                                                    " <gray>on <white>" + arena.getName()));
    }

    private void randomArena(final Player player, final String gamemodeName) {
        final Gamemode gamemode = Gamemode.fromString(gamemodeName);
        
        if (gamemode == null) {
            player.sendMessage(MessageUtils.parseMessage("<red>Invalid gamemode: " + gamemodeName));
            player.sendMessage(MessageUtils.parseMessage("<yellow>Available gamemodes: " + 
                Arrays.stream(Gamemode.values()).map(Gamemode::getDisplayName).collect(Collectors.joining(", "))));
            return;
        }

        final List<Arena> compatibleArenas = Paintball.getPlugin().getArenaManager().getArenas().values().stream()
            .filter(arena -> arena.isCompatible(gamemode))
            .collect(Collectors.toList());

        if (compatibleArenas.isEmpty()) {
            player.sendMessage(MessageUtils.parseMessage("<red>No compatible arenas found for " + gamemode.getDisplayName()));
            return;
        }

        final Arena randomArena = compatibleArenas.get((int) (Math.random() * compatibleArenas.size()));
        
        // Set random arena for next game
        Paintball.getPlugin().getLobbyManager().setNextGame(gamemode, randomArena);
        player.sendMessage(MessageUtils.parseMessage("<green>Set random arena: <yellow>" + gamemode.getDisplayName() + 
                                                    " <gray>on <white>" + randomArena.getName()));
    }

    private void listArenas(final Player player) {
        final var arenas = Paintball.getPlugin().getArenaManager().getArenas();
        
        if (arenas.isEmpty()) {
            player.sendMessage(MessageUtils.parseMessage("<yellow>No arenas found."));
            return;
        }

        player.sendMessage(MessageUtils.parseMessage("<green><bold>Available Arenas:"));
        for (final Arena arena : arenas.values()) {
            final String status = arena.isEnabled() ? "<green>✓" : "<red>✗";
            final String spawns = arena.getTotalSpawns() + " spawns";
            final String flags = arena.getTotalFlagSpawns() > 0 ? ", " + arena.getTotalFlagSpawns() + " flags" : "";
            final String gamemodes = arena.getCompatibleGameModes().size() + " gamemodes";
            
            player.sendMessage(MessageUtils.parseMessage(
                status + " <yellow>" + arena.getName() + " <gray>(" + spawns + flags + ", " + gamemodes + ")"
            ));
        }
    }

    private void showArenaInfo(final Player player, final String name) {
        final Arena arena = Paintball.getPlugin().getArenaManager().getArenas().get(name.toLowerCase());
        
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
                    .map(Gamemode::getDisplayName)
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

    private void reloadArenas(final Player player) {
        Paintball.getPlugin().getArenaManager().saveArenas();
        player.sendMessage(MessageUtils.parseMessage("<green>Arena configuration reloaded!"));
    }

    private void sendHelpMessage(final Player player) {
        player.sendMessage(MessageUtils.parseMessage("<green><bold>Arena Commands:"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena create <name> <schematic> <gray>- Create a new arena"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena edit <name> <gray>- Edit arena configuration"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena force <arena> <gamemode> <gray>- Force specific arena and gamemode"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena random <gamemode> <gray>- Set random arena for gamemode"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena list <gray>- List all arenas"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena info <name> <gray>- Show arena information"));
        player.sendMessage(MessageUtils.parseMessage("<yellow>/arena reload <gray>- Reload arena configuration"));
    }

    private List<String> getAvailableSchematics() {
        final File schematicsFolder = new File(Paintball.getPlugin().getDataFolder(), "schematics");
        final List<String> schematics = new ArrayList<>();
        
        if (schematicsFolder.exists() && schematicsFolder.isDirectory()) {
            final File[] files = schematicsFolder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".schem") || name.toLowerCase().endsWith(".schematic"));
            
            if (files != null) {
                for (final File file : files) {
                    schematics.add(file.getName());
                }
            }
        }
        
        return schematics;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String alias, @NotNull final String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "edit", "force", "random", "list", "info", "reload")
                    .stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length == 3) {
                    return this.getAvailableSchematics().stream()
                            .filter(schematic -> schematic.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "edit", "info" -> {
                if (args.length == 2) {
                    return Paintball.getPlugin().getArenaManager().getArenas().keySet()
                            .stream()
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "force" -> {
                if (args.length == 2) {
                    return Paintball.getPlugin().getArenaManager().getArenas().keySet()
                            .stream()
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (args.length == 3) {
                    return Arrays.stream(Gamemode.values())
                            .map(Gamemode::name)
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "random" -> {
                if (args.length == 2) {
                    return Arrays.stream(Gamemode.values())
                            .map(Gamemode::name)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return new ArrayList<>();
    }
}