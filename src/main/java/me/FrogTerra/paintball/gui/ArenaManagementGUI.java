package me.FrogTerra.paintball.gui;

import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.arena.Arena;
import me.FrogTerra.paintball.game.Gamemode;
import me.FrogTerra.paintball.item.ItemCreator;
import me.FrogTerra.paintball.utility.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GUI for managing arena settings
 */
public class ArenaManagementGUI extends GUI {

    private final String arenaName;
    private Arena arena;

    public ArenaManagementGUI(final String arenaName) {
        super(Rows.FIVE, MessageUtils.parseMessage("<dark_blue>Arena Management: " + arenaName));
        this.arenaName = arenaName;
        this.arena = Paintball.getPlugin().getArenaManager().getArenas().get(arenaName.toLowerCase());
    }

    @Override
    public void onSetItems() {
        if (this.arena == null) {
            // Arena doesn't exist, show error
            this.setItem(22, new ItemCreator(Material.BARRIER)
                    .setDisplayName("<red><bold>Arena Not Found")
                    .setLore("<gray>The arena '" + this.arenaName + "' could not be found.")
                    .build(), player -> player.closeInventory());
            return;
        }

        // Arena info display
        this.setItem(4, new ItemCreator(Material.MAP)
                .setDisplayName("<aqua><bold>" + this.arena.getName())
                .setLore(
                        "<gray>Schematic: <white>" + this.arena.getSchematicFile(),
                        "<gray>Status: " + (this.arena.isEnabled() ? "<green>Enabled" : "<red>Disabled"),
                        "<gray>Valid: " + (this.arena.isValid() ? "<green>Yes" : "<red>No"),
                        "",
                        "<gray>Spawns: <white>" + this.arena.getTotalSpawns(),
                        "<gray>Flag Spawns: <white>" + this.arena.getTotalFlagSpawns(),
                        "<gray>Gamemodes: <white>" + this.arena.getCompatibleGameModes().size()
                )
                .setRarity(ItemRarity.EPIC)
                .build());

        // Enable/Disable toggle
        this.setItem(10, new ItemCreator(this.arena.isEnabled() ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                .setDisplayName(this.arena.isEnabled() ? "<green><bold>Arena Enabled" : "<red><bold>Arena Disabled")
                .setLore(
                        this.arena.isEnabled() ? 
                            "<gray>This arena is currently enabled and" :
                            "<gray>This arena is currently disabled and",
                        this.arena.isEnabled() ? 
                            "<gray>can be used in games." :
                            "<gray>cannot be used in games.",
                        "",
                        "<yellow>Click to " + (this.arena.isEnabled() ? "disable" : "enable")
                )
                .setRarity(this.arena.isEnabled() ? ItemRarity.RARE : ItemRarity.COMMON)
                .build(), this::toggleArenaEnabled);

        // Gamemode configuration
        this.setItem(12, new ItemCreator(Material.COMPASS)
                .setDisplayName("<gold><bold>Configure Gamemodes")
                .setLore(
                        "<gray>Set which gamemodes this arena supports",
                        "",
                        "<yellow>Compatible Gamemodes:",
                        this.arena.getCompatibleGameModes().isEmpty() ? 
                            "<red>None selected" :
                            this.arena.getCompatibleGameModes().stream()
                                .map(gm -> "<green>• " + gm.getDisplayName())
                                .collect(Collectors.joining("\n")),
                        "",
                        "<yellow>Click to configure"
                )
                .setRarity(ItemRarity.UNCOMMON)
                .build(), this::openGamemodeSelection);

        // Spawn point editor
        this.setItem(14, new ItemCreator(Material.ARMOR_STAND)
                .setDisplayName("<blue><bold>Edit Spawn Points")
                .setLore(
                        "<gray>Configure team and flag spawn locations",
                        "",
                        "<red>Red Spawns: <white>" + this.arena.getRedSpawns().size(),
                        "<blue>Blue Spawns: <white>" + this.arena.getBlueSpawns().size(),
                        "<white>FFA Spawns: <white>" + this.arena.getFreeForAllSpawns().size(),
                        "<gold>Red Flags: <white>" + this.arena.getRedFlagSpawns().size(),
                        "<aqua>Blue Flags: <white>" + this.arena.getBlueFlagSpawns().size(),
                        "",
                        "<yellow>Click to enter editing mode"
                )
                .setRarity(ItemRarity.UNCOMMON)
                .build(), this::enterSpawnEditor);

        // Delete arena (with confirmation)
        this.setItem(16, new ItemCreator(Material.TNT)
                .setDisplayName("<dark_red><bold>Delete Arena")
                .setLore(
                        "<gray>Permanently delete this arena",
                        "",
                        "<red><bold>WARNING: This action cannot be undone!",
                        "<red>All spawn points and configuration will be lost.",
                        "",
                        "<yellow>Click to delete"
                )
                .setRarity(ItemRarity.EPIC)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build(), this::confirmDelete);

        // Save changes
        this.setItem(37, new ItemCreator(Material.EMERALD)
                .setDisplayName("<green><bold>Save Changes")
                .setLore(
                        "<gray>Save all changes to the arena",
                        "",
                        "<green>This will update the arena configuration",
                        "<green>and make it available for games if enabled.",
                        "",
                        "<yellow>Click to save"
                )
                .setRarity(ItemRarity.EPIC)
                .build(), this::saveChanges);

        // Cancel/Close
        this.setItem(43, new ItemCreator(Material.BARRIER)
                .setDisplayName("<red><bold>Close")
                .setLore("<gray>Close without saving changes")
                .build(), player -> player.closeInventory());
    }

    private void toggleArenaEnabled(final Player player) {
        this.arena.setEnabled(!this.arena.isEnabled());
        player.sendMessage(MessageUtils.parseMessage(
            this.arena.isEnabled() ? 
                "<green>Arena enabled!" : 
                "<red>Arena disabled!"
        ));
        this.onSetItems();
    }

    private void openGamemodeSelection(final Player player) {
        final GamemodeSelectionGUI gamemodeGUI = new GamemodeSelectionGUI(this.arena, this);
        gamemodeGUI.open(player);
    }

    private void enterSpawnEditor(final Player player) {
        Paintball.getPlugin().getArenaManager().getArenaEditor().enterEditorMode(player, this.arenaName)
            .thenAccept(success -> {
                if (!success) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Failed to enter arena editor mode!"));
                }
            });
    }

    private void confirmDelete(final Player player) {
        final ConfirmationGUI confirmGUI = new ConfirmationGUI(
            "<dark_red>Delete Arena: " + this.arenaName,
            "<red><bold>Are you sure you want to delete this arena?",
            Arrays.asList(
                "<gray>Arena: <white>" + this.arena.getName(),
                "<gray>This action cannot be undone!",
                "",
                "<red>All spawn points and configuration will be lost."
            ),
            () -> {
                if (Paintball.getPlugin().getArenaManager().deleteArena(this.arenaName)) {
                    player.sendMessage(MessageUtils.parseMessage("<green>Arena '" + this.arenaName + "' deleted successfully!"));
                    player.closeInventory();
                } else {
                    player.sendMessage(MessageUtils.parseMessage("<red>Failed to delete arena!"));
                }
            },
            () -> this.open(player)
        );
        confirmGUI.open(player);
    }

    private void saveChanges(final Player player) {
        if (!this.arena.isValid() && this.arena.isEnabled()) {
            player.sendMessage(MessageUtils.parseMessage("<red>Cannot save: Arena configuration is invalid!"));
            player.sendMessage(MessageUtils.parseMessage("<yellow>Please ensure the arena has:"));
            player.sendMessage(MessageUtils.parseMessage("<yellow>• At least one compatible gamemode"));
            player.sendMessage(MessageUtils.parseMessage("<yellow>• Required spawn points for each gamemode"));
            return;
        }

        Paintball.getPlugin().getArenaManager().saveArenas();
        player.sendMessage(MessageUtils.parseMessage("<green>Arena changes saved successfully!"));
        player.closeInventory();
    }

    /**
     * Inner class for gamemode selection
     */
    private static class GamemodeSelectionGUI extends GUI {
        private final Arena arena;
        private final ArenaManagementGUI parentGUI;

        public GamemodeSelectionGUI(final Arena arena, final ArenaManagementGUI parentGUI) {
            super(Rows.THREE, MessageUtils.parseMessage("<gold>Select Gamemodes: " + arena.getName()));
            this.arena = arena;
            this.parentGUI = parentGUI;
        }

        @Override
        public void onSetItems() {
            if (this.arena.getCompatibleGameModes() == null) {
                this.arena.setCompatibleGameModes(new HashSet<>());
            }

            // Ensure at least one gamemode is selected
            if (this.arena.getCompatibleGameModes().isEmpty()) {
                this.arena.getCompatibleGameModes().add(Gamemode.TEAM_DEATHMATCH);
            }

            // Display all gamemodes
            final Gamemode[] gamemodes = Gamemode.values();
            for (int i = 0; i < gamemodes.length; i++) {
                final Gamemode gamemode = gamemodes[i];
                final boolean isSelected = this.arena.getCompatibleGameModes().contains(gamemode);
                
                this.setItem(10 + i, new ItemCreator(isSelected ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                        .setDisplayName((isSelected ? "<green><bold>✓ " : "<red>✗ ") + gamemode.getDisplayName())
                        .setLore(
                                "<gray>Description: <white>" + gamemode.getDescription(),
                                "<gray>Duration: <white>" + MessageUtils.formatTime(gamemode.getDuration()),
                                "<gray>Lives: <white>" + (gamemode.hasUnlimitedLives() ? "Unlimited" : gamemode.getLives()),
                                "<gray>Min Players: <white>" + gamemode.getMinPlayers(),
                                "<gray>Teams: <white>" + (gamemode.isHasTeams() ? "Yes" : "No"),
                                "",
                                isSelected ? "<green>Currently enabled" : "<red>Currently disabled",
                                "<yellow>Click to toggle"
                        )
                        .setRarity(isSelected ? ItemRarity.RARE : ItemRarity.COMMON)
                        .build(), player -> this.toggleGamemode(gamemode, player));
            }

            // Save and return button
            this.setItem(22, new ItemCreator(Material.EMERALD)
                    .setDisplayName("<green><bold>Save & Return")
                    .setLore(
                            "<gray>Save gamemode selection and return",
                            "<gray>to arena management",
                            "",
                            "<yellow>Selected: <white>" + this.arena.getCompatibleGameModes().size() + " gamemodes"
                    )
                    .setRarity(ItemRarity.EPIC)
                    .build(), player -> {
                        this.parentGUI.onSetItems();
                        this.parentGUI.open(player);
                    });

            // Cancel button
            this.setItem(18, new ItemCreator(Material.BARRIER)
                    .setDisplayName("<red><bold>Cancel")
                    .setLore("<gray>Return without saving changes")
                    .build(), player -> this.parentGUI.open(player));
        }

        private void toggleGamemode(final Gamemode gamemode, final Player player) {
            final Set<Gamemode> selectedGamemodes = this.arena.getCompatibleGameModes();
            
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
            
            this.onSetItems();
        }
    }
}