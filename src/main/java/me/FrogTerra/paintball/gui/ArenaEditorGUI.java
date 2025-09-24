package me.FrogTerra.paintball.gui;

import me.FrogTerra.paintball.arena.ArenaEditor;
import me.FrogTerra.paintball.game.Gamemode;
import me.FrogTerra.paintball.item.ItemCreator;
import me.FrogTerra.paintball.utility.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.stream.Collectors;

/**
 * GUI for arena editor tools and spawn point management
 */
public class ArenaEditorGUI extends GUI {

    private final ArenaEditor arenaEditor;
    private final String arenaName;

    public ArenaEditorGUI(ArenaEditor arenaEditor, String arenaName) {
        super(Rows.FIVE, MessageUtils.parseMessage("<dark_gray>Arena Editor: <yellow>" + arenaName));
        this.arenaEditor = arenaEditor;
        this.arenaName = arenaName;
    }

    @Override
    public void onSetItems() {
        // Clear inventory first
        getInventory().clear();

        // Title item
        setItem(4, new ItemCreator(Material.NAME_TAG)
                .setDisplayName("<gold><bold>Arena Editor")
                .setLore(
                        "<gray>Editing arena: <yellow>" + arenaName,
                        "<gray>",
                        "<green>Use the tools below to edit spawn points",
                        "<gray>Right-click to place, left-click armor stands to remove"
                )
                .setRarity(ItemRarity.EPIC)
                .build());

        // Spawn point placement tools
        setupSpawnTools();

        // Control buttons
        setupControlButtons();

        // Information panel
        setupInformationPanel();

        // Set placeholders for empty slots
        if (usePlaceholders()) {
            setPlaceholders();
        }
    }

    /**
     * Setup spawn point placement tools
     */
    private void setupSpawnTools() {
        // Red Team Spawn
        setItem(19, createSpawnTool(
                ArenaEditor.SpawnPointType.RED_SPAWN,
                Material.RED_CONCRETE,
                Color.RED,
                "<red><bold>Red Team Spawn",
                "Place spawn points for the red team"
        ), player -> changeSpawnMode(player, ArenaEditor.SpawnPointType.RED_SPAWN));

        // Blue Team Spawn
        setItem(20, createSpawnTool(
                ArenaEditor.SpawnPointType.BLUE_SPAWN,
                Material.BLUE_CONCRETE,
                Color.BLUE,
                "<blue><bold>Blue Team Spawn",
                "Place spawn points for the blue team"
        ), player -> changeSpawnMode(player, ArenaEditor.SpawnPointType.BLUE_SPAWN));

        // Free For All Spawn
        setItem(21, createSpawnTool(
                ArenaEditor.SpawnPointType.FREE_FOR_ALL_SPAWN,
                Material.WHITE_CONCRETE,
                Color.WHITE,
                "<white><bold>Free For All Spawn",
                "Place spawn points for free for all gamemode"
        ), player -> changeSpawnMode(player, ArenaEditor.SpawnPointType.FREE_FOR_ALL_SPAWN));

        // Red Flag Spawn
        setItem(28, createSpawnTool(
                ArenaEditor.SpawnPointType.FLAG_RED_SPAWN,
                Material.RED_BANNER,
                Color.RED,
                "<red><bold>Red Flag Spawn",
                "Place flag spawn points for red team (Flag Rush)"
        ), player -> changeSpawnMode(player, ArenaEditor.SpawnPointType.FLAG_RED_SPAWN));

        // Blue Flag Spawn
        setItem(29, createSpawnTool(
                ArenaEditor.SpawnPointType.FLAG_BLUE_SPAWN,
                Material.BLUE_BANNER,
                Color.BLUE,
                "<blue><bold>Blue Flag Spawn",
                "Place flag spawn points for blue team (Flag Rush)"
        ), player -> changeSpawnMode(player, ArenaEditor.SpawnPointType.FLAG_BLUE_SPAWN));

        // Spawn Point Placer Tool
        setItem(22, new ItemCreator(Material.BLAZE_ROD)
                .setDisplayName("<green><bold>Spawn Point Placer")
                .setLore(
                        "<gray>Right-click to place spawn points",
                        "<gray>Left-click armor stands to remove them",
                        "<gray>",
                        "<yellow>Select a spawn type first!"
                )
                .setPersistentData("editor_tool", "spawn_placer")
                .setRarity(ItemRarity.EPIC)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build(), player -> {
                    player.closeInventory();
                    player.sendMessage(MessageUtils.parseMessage("<yellow>Use right-click to place spawn points, left-click armor stands to remove them"));
                });
    }

    /**
     * Setup control buttons
     */
    private void setupControlButtons() {
        // Save and Exit
        setItem(35, new ItemCreator(Material.EMERALD)
                .setDisplayName("<green><bold>Save & Exit")
                .setLore(
                        "<gray>Save all changes and exit editor mode",
                        "<gray>",
                        "<green>✓ All spawn points will be saved to schematic",
                        "<green>✓ Arena will be ready for games"
                )
                .setRarity(ItemRarity.EPIC)
                .build(), player -> {
                    player.closeInventory();
                    arenaEditor.exitEditorMode(player, true);
                });

        // Exit Without Saving
        setItem(44, new ItemCreator(Material.BARRIER)
                .setDisplayName("<red><bold>Exit Without Saving")
                .setLore(
                        "<gray>Exit editor mode without saving changes",
                        "<gray>",
                        "<red>⚠ All changes will be lost!",
                        "<red>⚠ Spawn points will not be saved"
                )
                .setRarity(ItemRarity.EPIC)
                .build(), player -> {
                    player.closeInventory();
                    arenaEditor.exitEditorMode(player, false);
                });

        // Refresh GUI
        setItem(40, new ItemCreator(Material.COMPASS)
                .setDisplayName("<aqua><bold>Refresh View")
                .setLore(
                        "<gray>Refresh the editor interface",
                        "<gray>",
                        "<aqua>Updates spawn point counts and status"
                )
                .setRarity(ItemRarity.COMMON)
                .build(), player -> {
                    update();
                    onSetItems();
                    player.sendMessage(MessageUtils.parseMessage("<green>Editor interface refreshed!"));
                });
    }

    /**
     * Setup information panel
     */
    private void setupInformationPanel() {
        // Current spawn counts (would need to be implemented in ArenaEditor)
        setItem(7, new ItemCreator(Material.BOOK)
                .setDisplayName("<yellow><bold>Spawn Point Status")
                .setLore(
                        "<gray>Current spawn points in arena:",
                        "<gray>",
                        "<red>Red Team: <white>0", // TODO: Get actual counts
                        "<blue>Blue Team: <white>0",
                        "<white>Free For All: <white>0",
                        "<red>Red Flags: <white>0",
                        "<blue>Blue Flags: <white>0"
                )
                .setRarity(ItemRarity.COMMON)
                .build());

        // Gamemode compatibility info
        setItem(16, new ItemCreator(Material.ENCHANTED_BOOK)
                .setDisplayName("<purple><bold>Gamemode Compatibility")
                .setLore(
                        "<gray>Supported gamemodes:",
                        "<gray>",
                        "<yellow>Team Deathmatch: <green>Red + Blue spawns",
                        "<yellow>Free For All: <green>FFA spawns",
                        "<yellow>Flag Rush: <green>Team + Flag spawns",
                        "<yellow>Juggernaut: <green>Red + Blue spawns"
                )
                .setRarity(ItemRarity.RARE)
                .build());

        // Help information
        setItem(25, new ItemCreator(Material.PAPER)
                .setDisplayName("<light_purple><bold>Editor Help")
                .setLore(
                        "<gray>How to use the arena editor:",
                        "<gray>",
                        "<green>1. <white>Select a spawn type",
                        "<green>2. <white>Take the placement tool",
                        "<green>3. <white>Right-click to place spawns",
                        "<green>4. <white>Left-click armor stands to remove",
                        "<green>5. <white>Save when finished"
                )
                .setRarity(ItemRarity.COMMON)
                .build());
    }

    /**
     * Create a spawn tool item with colored leather armor
     */
    private ItemStack createSpawnTool(ArenaEditor.SpawnPointType spawnType, Material material, 
                                    Color armorColor, String displayName, String description) {
        return new ItemCreator(material)
                .setDisplayName(displayName)
                .setLore(
                        "<gray>" + description,
                        "<gray>",
                        "<yellow>Compatible with:",
                        spawnType.getCompatibleGamemodes().stream()
                                .map(Gamemode::getDisplayName)
                                .collect(Collectors.joining(", ", "<white>", "")),
                        "<gray>",
                        "<green>Click to select this spawn type"
                )
                .setPersistentData("editor_tool", "mode_" + spawnType.name().toLowerCase())
                .setRarity(ItemRarity.RARE)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Create colored leather armor piece for visual representation
     */
    private ItemStack createColoredLeatherArmor(Material material, Color color) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            armor.setItemMeta(meta);
        }
        return armor;
    }

    /**
     * Change spawn mode for player
     */
    private void changeSpawnMode(Player player, ArenaEditor.SpawnPointType spawnType) {
        arenaEditor.changeSpawnMode(player, spawnType);
        player.closeInventory();
        
        // Give the placement tool
        ItemStack placementTool = new ItemCreator(Material.BLAZE_ROD)
                .setDisplayName("<green><bold>Spawn Point Placer")
                .setLore(
                        "<gray>Right-click to place spawn points",
                        "<gray>Left-click armor stands to remove them",
                        "<gray>",
                        "<yellow>Current Mode: " + spawnType.getDisplayName()
                )
                .setPersistentData("editor_tool", "spawn_placer")
                .setRarity(ItemRarity.EPIC)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        
        player.getInventory().setItem(0, placementTool);
        player.sendMessage(MessageUtils.parseMessage("<green>Selected spawn type: " + spawnType.getDisplayName()));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Use the placement tool to place spawn points!"));
    }
}