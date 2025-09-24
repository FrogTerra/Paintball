package me.FrogTerra.paintball.gui;

import me.FrogTerra.paintball.arena.Arena;
import me.FrogTerra.paintball.game.Gamemode;
import me.FrogTerra.paintball.item.ItemCreator;
import me.FrogTerra.paintball.utility.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI for creating and managing arenas
 */
public class ArenaCreationGUI extends GUI {

    private final String arenaName;
    private final boolean isEditing;
    private Arena arena;
    private String selectedSchematic;
    private final List<String> availableSchematics;
    private int schematicPage = 0;
    private static final int SCHEMATICS_PER_PAGE = 7;

    public ArenaCreationGUI(String arenaName) {
        super(Rows.FIVE, MessageUtils.parseMessage("<dark_gray>Arena: <yellow>" + arenaName));
        this.arenaName = arenaName;
        this.isEditing = getPlugin().getArenaManager().getArenas().containsKey(arenaName.toLowerCase());
        this.arena = isEditing ? getPlugin().getArenaManager().getArenas().get(arenaName.toLowerCase()) : null;
        this.selectedSchematic = arena != null ? arena.getSchematicFile() : null;
        this.availableSchematics = discoverSchematics();
    }

    @Override
    public void onSetItems() {
        // Clear inventory
        getInventory().clear();

        // Setup different sections
        setupArenaInfo();
        setupSchematicSelection();
        setupGamemodeManagement();
        setupControlButtons();

        // Set placeholders
        if (usePlaceholders()) {
            setPlaceholders();
        }
    }

    /**
     * Setup arena information display
     */
    private void setupArenaInfo() {
        List<String> infoLore = new ArrayList<>();
        infoLore.add("<gray>Arena Information:");
        infoLore.add("");
        infoLore.add("<yellow>Name: <white>" + arenaName);
        infoLore.add("<yellow>Status: " + (isEditing ? "<blue>Editing Existing" : "<green>Creating New"));
        
        if (arena != null) {
            infoLore.add("<yellow>Enabled: " + (arena.isEnabled() ? "<green>Yes" : "<red>No"));
            infoLore.add("<yellow>Valid: " + (arena.isValid() ? "<green>Yes" : "<red>No"));
            infoLore.add("");
            infoLore.add("<yellow>Spawn Points:");
            infoLore.add("  <red>Red Team: <white>" + arena.getRedSpawns().size());
            infoLore.add("  <blue>Blue Team: <white>" + arena.getBlueSpawns().size());
            infoLore.add("  <white>Free For All: <white>" + arena.getFreeForAllSpawns().size());
            infoLore.add("  <red>Red Flags: <white>" + arena.getRedFlagSpawns().size());
            infoLore.add("  <blue>Blue Flags: <white>" + arena.getBlueFlagSpawns().size());
        }

        if (selectedSchematic != null) {
            infoLore.add("");
            infoLore.add("<yellow>Selected Schematic:");
            infoLore.add("  <white>" + selectedSchematic);
        }

        setItem(4, new ItemCreator(Material.NAME_TAG)
                .setDisplayName("<gold><bold>" + arenaName)
                .setLore(infoLore.toArray(new String[0]))
                .setRarity(ItemRarity.EPIC)
                .build());
    }

    /**
     * Setup schematic selection area
     */
    private void setupSchematicSelection() {
        // Schematic selection title
        setItem(10, new ItemCreator(Material.PAPER)
                .setDisplayName("<blue><bold>Schematic Selection")
                .setLore(
                        "<gray>Choose a schematic file for this arena",
                        "<gray>Schematics found: <white>" + availableSchematics.size(),
                        "<gray>Page: <white>" + (schematicPage + 1) + "/" + Math.max(1, (int) Math.ceil((double) availableSchematics.size() / SCHEMATICS_PER_PAGE))
                )
                .setRarity(ItemRarity.UNCOMMON)
                .build());

        // Previous page button
        if (schematicPage > 0) {
            setItem(9, new ItemCreator(Material.ARROW)
                    .setDisplayName("<yellow>Previous Page")
                    .setLore("<gray>Go to previous page of schematics")
                    .build(), player -> {
                        schematicPage--;
                        onSetItems();
                    });
        }

        // Next page button
        if ((schematicPage + 1) * SCHEMATICS_PER_PAGE < availableSchematics.size()) {
            setItem(17, new ItemCreator(Material.ARROW)
                    .setDisplayName("<yellow>Next Page")
                    .setLore("<gray>Go to next page of schematics")
                    .build(), player -> {
                        schematicPage++;
                        onSetItems();
                    });
        }

        // Display schematics for current page
        int startIndex = schematicPage * SCHEMATICS_PER_PAGE;
        int endIndex = Math.min(startIndex + SCHEMATICS_PER_PAGE, availableSchematics.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String schematic = availableSchematics.get(i);
            boolean isSelected = schematic.equals(selectedSchematic);
            int slot = 11 + (i - startIndex);

            setItem(slot, new ItemCreator(isSelected ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                    .setDisplayName((isSelected ? "<green><bold>✓ " : "<gray>") + schematic)
                    .setLore(
                            "<gray>Schematic file: <white>" + schematic,
                            "",
                            isSelected ? "<green>Currently selected" : "<yellow>Click to select this schematic"
                    )
                    .setRarity(isSelected ? ItemRarity.RARE : ItemRarity.COMMON)
                    .build(), player -> {
                        selectedSchematic = schematic;
                        player.sendMessage(MessageUtils.parseMessage("<green>Selected schematic: <white>" + schematic));
                        onSetItems();
                    });
        }

        // No schematic option
        boolean noSchematicSelected = selectedSchematic == null;
        setItem(16, new ItemCreator(noSchematicSelected ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                .setDisplayName((noSchematicSelected ? "<green><bold>✓ " : "<gray>") + "No Schematic")
                .setLore(
                        "<gray>Create arena without a schematic file",
                        "<gray>You can add a schematic later",
                        "",
                        noSchematicSelected ? "<green>Currently selected" : "<yellow>Click to select no schematic"
                )
                .setRarity(noSchematicSelected ? ItemRarity.RARE : ItemRarity.COMMON)
                .build(), player -> {
                    selectedSchematic = null;
                    player.sendMessage(MessageUtils.parseMessage("<yellow>No schematic selected"));
                    onSetItems();
                });
    }

    /**
     * Setup gamemode management
     */
    private void setupGamemodeManagement() {
        setItem(28, new ItemCreator(Material.COMPASS)
                .setDisplayName("<purple><bold>Gamemode Management")
                .setLore(
                        "<gray>Configure which gamemodes can use this arena",
                        "<gray>Click to open gamemode selection"
                )
                .setRarity(ItemRarity.UNCOMMON)
                .build(), player -> {
                    new GamemodeSelectionGUI(arenaName, this).open(player);
                });

        // Display current gamemodes if editing
        if (arena != null && arena.getCompatibleGameModes() != null) {
            List<String> gamemodeLore = new ArrayList<>();
            gamemodeLore.add("<gray>Currently compatible gamemodes:");
            gamemodeLore.add("");
            
            if (arena.getCompatibleGameModes().isEmpty()) {
                gamemodeLore.add("<red>No gamemodes selected!");
            } else {
                for (Gamemode gamemode : arena.getCompatibleGameModes()) {
                    gamemodeLore.add("<green>✓ <white>" + gamemode.getDisplayName());
                }
            }

            setItem(29, new ItemCreator(Material.BOOK)
                    .setDisplayName("<blue>Current Gamemodes")
                    .setLore(gamemodeLore.toArray(new String[0]))
                    .build());
        }
    }

    /**
     * Setup control buttons
     */
    private void setupControlButtons() {
        // Create/Save Arena
        setItem(37, new ItemCreator(Material.EMERALD)
                .setDisplayName(isEditing ? "<green><bold>Save Changes" : "<green><bold>Create Arena")
                .setLore(
                        isEditing ? 
                            new String[]{"<gray>Save all changes to this arena"} :
                            new String[]{"<gray>Create the arena with current settings", "<gray>Arena will be created as disabled"}
                )
                .setRarity(ItemRarity.EPIC)
                .build(), this::createOrSaveArena);

        // Edit Spawn Points (only if arena exists)
        if (isEditing) {
            final String finalArenaName = arenaName;
            setItem(38, new ItemCreator(Material.BLAZE_ROD)
                    .setDisplayName("<blue><bold>Edit Spawn Points")
                    .setLore(
                            "<gray>Enter arena editor mode to place",
                            "<gray>and manage spawn points",
                            "",
                            "<green>✓ Visual spawn point placement",
                            "<green>✓ Team and flag spawn management"
                    )
                    .setRarity(ItemRarity.RARE)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build(), player -> {
                        player.closeInventory();
                        getPlugin().getArenaManager().getArenaEditor().enterEditorMode(player, finalArenaName);
                    });
        }

        // Toggle Enabled/Disabled (only if editing)
        if (isEditing && arena != null) {
            final Arena finalArena = arena;
            setItem(39, new ItemCreator(arena.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName(arena.isEnabled() ? "<green><bold>Enabled" : "<red><bold>Disabled")
                    .setLore(
                            "<gray>Click to " + (arena.isEnabled() ? "disable" : "enable") + " this arena",
                            "",
                            arena.isEnabled() ? 
                                "<green>✓ Arena is active for games" :
                                "<red>✗ Arena is disabled"
                    )
                    .setRarity(arena.isEnabled() ? ItemRarity.COMMON : ItemRarity.UNCOMMON)
                    .build(), player -> {
                        finalArena.setEnabled(!finalArena.isEnabled());
                        getPlugin().getArenaManager().saveArenas();
                        player.sendMessage(MessageUtils.parseMessage(
                            finalArena.isEnabled() ? 
                                "<green>Arena enabled!" :
                                "<red>Arena disabled!"
                        ));
                        onSetItems();
                    });
        }

        // Delete Arena (only if editing)
        if (isEditing) {
            final String finalArenaName = arenaName;
            setItem(41, new ItemCreator(Material.TNT)
                    .setDisplayName("<red><bold>Delete Arena")
                    .setLore(
                            "<gray>Permanently delete this arena",
                            "",
                            "<red>⚠ WARNING: This cannot be undone!",
                            "<red>⚠ All spawn points will be lost!",
                            "",
                            "<yellow>Click to confirm deletion"
                    )
                    .setRarity(ItemRarity.EPIC)
                    .build(), player -> deleteArena(player, finalArenaName));
        }

        // Cancel/Exit
        setItem(43, new ItemCreator(Material.BARRIER)
                .setDisplayName("<red><bold>Cancel")
                .setLore("<gray>Exit without saving changes")
                .build(), player -> player.closeInventory());
    }

    /**
     * Create or save arena
     */
    private void createOrSaveArena(Player player) {
        if (selectedSchematic == null && !isEditing) {
            // Create arena without schematic
            if (getPlugin().getArenaManager().createArena(arenaName, "none")) {
                Arena newArena = getPlugin().getArenaManager().getArenas().get(arenaName.toLowerCase());
                newArena.setEnabled(false); // Automatically disable new arenas
                newArena.setCompatibleGameModes(new HashSet<>());
                getPlugin().getArenaManager().saveArenas();
                
                player.closeInventory();
                player.sendMessage(MessageUtils.parseMessage("<green>Arena '" + arenaName + "' created successfully!"));
                player.sendMessage(MessageUtils.parseMessage("<yellow>Arena is disabled by default. Configure gamemodes and enable it when ready."));
            } else {
                player.sendMessage(MessageUtils.parseMessage("<red>Failed to create arena!"));
            }
            return;
        }

        if (selectedSchematic != null) {
            // Copy schematic from AsyncWorldEdit folder
            if (copySchematicFile(selectedSchematic)) {
                if (isEditing) {
                    // Update existing arena
                    arena.setSchematicFile(selectedSchematic);
                    getPlugin().getArenaManager().saveArenas();
                    player.sendMessage(MessageUtils.parseMessage("<green>Arena updated successfully!"));
                } else {
                    // Create new arena
                    if (getPlugin().getArenaManager().createArena(arenaName, selectedSchematic)) {
                        Arena newArena = getPlugin().getArenaManager().getArenas().get(arenaName.toLowerCase());
                        newArena.setEnabled(false); // Automatically disable new arenas
                        newArena.setCompatibleGameModes(new HashSet<>());
                        getPlugin().getArenaManager().saveArenas();
                        
                        player.closeInventory();
                        player.sendMessage(MessageUtils.parseMessage("<green>Arena '" + arenaName + "' created successfully!"));
                        player.sendMessage(MessageUtils.parseMessage("<yellow>Arena is disabled by default. Configure gamemodes and enable it when ready."));
                    } else {
                        player.sendMessage(MessageUtils.parseMessage("<red>Failed to create arena!"));
                    }
                }
            } else {
                player.sendMessage(MessageUtils.parseMessage("<red>Failed to copy schematic file!"));
            }
        }
    }

    /**
     * Delete arena with confirmation
     */
    private void deleteArena(Player player, String arenaName) {
        new ConfirmationGUI(
            "Delete Arena: " + arenaName,
            "Are you sure you want to delete this arena?",
            "This action cannot be undone!",
            () -> {
                if (getPlugin().getArenaManager().deleteArena(arenaName)) {
                    player.sendMessage(MessageUtils.parseMessage("<green>Arena '" + arenaName + "' deleted successfully!"));
                } else {
                    player.sendMessage(MessageUtils.parseMessage("<red>Failed to delete arena!"));
                }
            },
            () -> new ArenaCreationGUI(arenaName).open(player)
        ).open(player);
    }

    /**
     * Discover available schematics from AsyncWorldEdit folder
     */
    private List<String> discoverSchematics() {
        List<String> schematics = new ArrayList<>();
        
        // Check AsyncWorldEdit schematics folder
        File worldEditFolder = new File("plugins/AsyncWorldEdit");
        File schematicsFolder = new File(worldEditFolder, "schematics");
        
        if (!schematicsFolder.exists()) {
            // Try alternative paths
            schematicsFolder = new File("plugins/WorldEdit/schematics");
            if (!schematicsFolder.exists()) {
                schematicsFolder = new File("plugins/FastAsyncWorldEdit/schematics");
            }
        }
        
        if (schematicsFolder.exists() && schematicsFolder.isDirectory()) {
            File[] files = schematicsFolder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".schem") || 
                name.toLowerCase().endsWith(".schematic"));
            
            if (files != null) {
                for (File file : files) {
                    schematics.add(file.getName());
                }
            }
        }
        
        // Sort alphabetically
        schematics.sort(String.CASE_INSENSITIVE_ORDER);
        
        getPlugin().logInfo("Found " + schematics.size() + " schematic files");
        return schematics;
    }

    /**
     * Copy schematic file from AsyncWorldEdit to plugin folder
     */
    private boolean copySchematicFile(String schematicName) {
        try {
            // Find source file
            File sourceFile = findSchematicFile(schematicName);
            if (sourceFile == null || !sourceFile.exists()) {
                getPlugin().logError("Source schematic file not found: " + schematicName);
                return false;
            }

            // Ensure plugin schematics folder exists
            File pluginSchematicsFolder = new File(getPlugin().getDataFolder(), "schematics");
            if (!pluginSchematicsFolder.exists()) {
                pluginSchematicsFolder.mkdirs();
            }

            // Copy file
            File destFile = new File(pluginSchematicsFolder, schematicName);
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            getPlugin().logInfo("Copied schematic file: " + schematicName);
            return true;
            
        } catch (IOException e) {
            getPlugin().logError("Failed to copy schematic file: " + schematicName, e);
            return false;
        }
    }

    /**
     * Find schematic file in various possible locations
     */
    private File findSchematicFile(String schematicName) {
        String[] possiblePaths = {
            "plugins/AsyncWorldEdit/schematics/",
            "plugins/WorldEdit/schematics/",
            "plugins/FastAsyncWorldEdit/schematics/"
        };
        
        for (String path : possiblePaths) {
            File file = new File(path + schematicName);
            if (file.exists()) {
                return file;
            }
        }
        
        return null;
    }

    /**
     * Inner class for gamemode selection
     */
    private static class GamemodeSelectionGUI extends GUI {
        private final String arenaName;
        private final ArenaCreationGUI parentGUI;
        private Arena arena;

        public GamemodeSelectionGUI(String arenaName, ArenaCreationGUI parentGUI) {
            super(Rows.THREE, MessageUtils.parseMessage("<dark_purple>Select Gamemodes: <yellow>" + arenaName));
            this.arenaName = arenaName;
            this.parentGUI = parentGUI;
            this.arena = getPlugin().getArenaManager().getArenas().get(arenaName.toLowerCase());
        }

        @Override
        public void onSetItems() {
            if (arena == null) {
                // Create temporary arena for gamemode selection
                arena = new Arena(arenaName, "temp");
                Set<Gamemode> defaultGamemodes = new HashSet<>();
                defaultGamemodes.add(Gamemode.TEAM_DEATHMATCH); // Default to Team Deathmatch
                arena.setCompatibleGameModes(defaultGamemodes);
            }

            Set<Gamemode> selectedGamemodes = arena.getCompatibleGameModes();
            if (selectedGamemodes == null || selectedGamemodes.isEmpty()) {
                selectedGamemodes = new HashSet<>();
                selectedGamemodes.add(Gamemode.TEAM_DEATHMATCH); // Ensure at least one gamemode is selected
                arena.setCompatibleGameModes(selectedGamemodes);
            }

            // Display all gamemodes
            Gamemode[] gamemodes = Gamemode.values();
            for (int i = 0; i < gamemodes.length; i++) {
                final Gamemode gamemode = gamemodes[i];
                boolean isSelected = selectedGamemodes.contains(gamemode);
                
                setItem(10 + i, new ItemCreator(isSelected ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                        .setDisplayName((isSelected ? "<green><bold>✓ " : "<red>✗ ") + gamemode.getDisplayName())
                        .setLore(
                                "<gray>Duration: <white>" + MessageUtils.formatTime(gamemode.getDuration()),
                                "<gray>Lives: <white>" + (gamemode.hasUnlimitedLives() ? "Unlimited" : gamemode.getLives()),
                                "<gray>Min Players: <white>" + gamemode.getMinPlayers(),
                                "<gray>Teams: <white>" + (gamemode.isHasTeams() ? "Yes" : "No"),
                                "",
                                isSelected ? "<green>Currently enabled" : "<red>Currently disabled",
                                "<yellow>Click to toggle"
                        )
                        .setRarity(isSelected ? ItemRarity.RARE : ItemRarity.COMMON)
                        .build(), player -> {
                            this.toggleGamemode(gamemode, player);
                        });
            }

            // Save and return button
            setItem(22, new ItemCreator(Material.EMERALD)
                    .setDisplayName("<green><bold>Save & Return")
                    .setLore(
                            "<gray>Save gamemode selection and return",
                            "<gray>to arena management",
                            "",
                            "<yellow>Selected: <white>" + arena.getCompatibleGameModes().size() + " gamemodes"
                    )
                    .setRarity(ItemRarity.EPIC)
                    .build(), player -> {
                        this.saveAndReturn(player);
                    });

            // Cancel button
            setItem(18, new ItemCreator(Material.BARRIER)
                    .setDisplayName("<red><bold>Cancel")
                    .setLore("<gray>Return without saving changes")
                    .build(), player -> parentGUI.open(player));
        }

        /**
         * Toggle a gamemode on/off for the arena
         */
        private void toggleGamemode(final Gamemode gamemode, final Player player) {
            final Set<Gamemode> selectedGamemodes = arena.getCompatibleGameModes();
            
            if (selectedGamemodes.contains(gamemode)) {
                // Prevent removing the last gamemode
                if (selectedGamemodes.size() > 1) {
                    selectedGamemodes.remove(gamemode);
                    player.sendMessage(MessageUtils.parseMessage("<red>Disabled " + gamemode.getDisplayName()));
                } else {
                    player.sendMessage(MessageUtils.parseMessage("<red>Cannot disable the last gamemode! At least one must be selected."));
                }
            } else {
                selectedGamemodes.add(gamemode);
                player.sendMessage(MessageUtils.parseMessage("<green>Enabled " + gamemode.getDisplayName()));
            }
            
            // Refresh the GUI
            onSetItems();
        }

        /**
         * Save gamemode selection and return to parent GUI
         */
        private void saveAndReturn(final Player player) {
            // Save to existing arena or update parent
            if (getPlugin().getArenaManager().getArenas().containsKey(arenaName.toLowerCase())) {
                Arena existingArena = getPlugin().getArenaManager().getArenas().get(arenaName.toLowerCase());
                existingArena.setCompatibleGameModes(arena.getCompatibleGameModes());
                getPlugin().getArenaManager().saveArenas();
            }
            parentGUI.arena = arena;
            parentGUI.open(player);
        }
    }

    /**
     * Confirmation GUI for dangerous actions
     */
    private static class ConfirmationGUI extends GUI {
        private final String title;
        private final String message;
        private final String warning;
        private final Runnable onConfirm;
        private final Runnable onCancel;

        public ConfirmationGUI(String title, String message, String warning, Runnable onConfirm, Runnable onCancel) {
            super(Rows.THREE, MessageUtils.parseMessage("<dark_red>" + title));
            this.title = title;
            this.message = message;
            this.warning = warning;
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
        }

        @Override
        public void onSetItems() {
            // Warning display
            setItem(13, new ItemCreator(Material.BARRIER)
                    .setDisplayName("<red><bold>⚠ CONFIRMATION REQUIRED ⚠")
                    .setLore(
                            "<gray>" + message,
                            "",
                            "<red>" + warning,
                            "",
                            "<yellow>Choose an option below"
                    )
                    .setRarity(ItemRarity.EPIC)
                    .build());

            // Confirm button
            setItem(11, new ItemCreator(Material.LIME_CONCRETE)
                    .setDisplayName("<green><bold>✓ CONFIRM")
                    .setLore(
                            "<gray>Yes, I want to proceed",
                            "<red>This action cannot be undone!"
                    )
                    .setRarity(ItemRarity.EPIC)
                    .build(), player -> {
                        player.closeInventory();
                        onConfirm.run();
                    });

            // Cancel button
            setItem(15, new ItemCreator(Material.RED_CONCRETE)
                    .setDisplayName("<red><bold>✗ CANCEL")
                    .setLore("<gray>No, take me back")
                    .setRarity(ItemRarity.COMMON)
                    .build(), player -> {
                        player.closeInventory();
                        onCancel.run();
                    });
        }
    }
}