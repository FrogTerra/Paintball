package me.FrogTerra.paintball.arena;

import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.game.Gamemode;
import me.FrogTerra.paintball.item.ItemCreator;
import me.FrogTerra.paintball.utility.MessageUtils;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Handles arena editing functionality with armor stand spawn point visualization
 */
public final class ArenaEditor {

    private final Paintball plugin;
    
    @Getter private final Set<UUID> editingPlayers = new HashSet<>();
    @Getter private final Map<UUID, String> playerEditingArena = new HashMap<>();
    @Getter private final Map<String, Set<ArmorStand>> arenaArmorStands = new HashMap<>();
    @Getter private final Map<UUID, SpawnPointType> playerSpawnMode = new HashMap<>();

    public ArenaEditor(Paintball plugin) {
        this.plugin = plugin;
    }

    /**
     * Enter arena editor mode for a player
     */
    public CompletableFuture<Boolean> enterEditorMode(final Player player, final String arenaName) {
        return CompletableFuture.supplyAsync(() -> {
            if (this.editingPlayers.contains(player.getUniqueId())) {
                player.sendMessage(MessageUtils.parseMessage("<red>You are already in arena editor mode!"));
                return false;
            }

            final Arena arena = this.plugin.getArenaManager().getArenas().get(arenaName.toLowerCase());
            if (arena == null) {
                player.sendMessage(MessageUtils.parseMessage("<red>Arena not found: " + arenaName));
                return false;
            }

            // Load arena into editor world
            return this.plugin.getArenaManager().loadArenaInEditor(arenaName).thenCompose(success -> {
                if (!success) {
                    player.sendMessage(MessageUtils.parseMessage("<red>Failed to load arena into editor world!"));
                    return CompletableFuture.completedFuture(false);
                }

                return CompletableFuture.supplyAsync(() -> {
                    // Teleport player to editor world
                    final World editorWorld = this.plugin.getWorldManager().getArenaEditorWorld();
                    final Location editorSpawn = new Location(editorWorld, 0, 105, 0);
                    
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        player.teleport(editorSpawn);
                        player.setGameMode(GameMode.CREATIVE);
                        
                        // Add to editing players
                        this.editingPlayers.add(player.getUniqueId());
                        this.playerEditingArena.put(player.getUniqueId(), arenaName.toLowerCase());
                        this.playerSpawnMode.put(player.getUniqueId(), SpawnPointType.RED_SPAWN);
                        
                        // Give editing tools
                        this.giveEditorTools(player);
                        
                        // Spawn existing armor stands
                        this.spawnArmorStands(arena, editorWorld);
                        
                        player.sendMessage(MessageUtils.parseMessage("<green>Entered arena editor mode for: <yellow>" + arenaName));
                        player.sendMessage(MessageUtils.parseMessage("<gray>Use the tools in your inventory to edit spawn points"));
                        player.sendMessage(MessageUtils.parseMessage("<gray>Right-click to place spawn points, left-click armor stands to remove them"));
                    });
                    
                    return true;
                });
            }).join();
        });
    }

    /**
     * Exit arena editor mode for a player
     */
    public CompletableFuture<Boolean> exitEditorMode(final Player player, final boolean save) {
        return CompletableFuture.supplyAsync(() -> {
            if (!this.editingPlayers.contains(player.getUniqueId())) {
                player.sendMessage(MessageUtils.parseMessage("<red>You are not in arena editor mode!"));
                return false;
            }

            final String arenaName = this.playerEditingArena.get(player.getUniqueId());
            if (arenaName == null) {
                return false;
            }

            if (save) {
                // Save the entire arena including armor stands to schematic
                this.saveArenaWithArmorStands(arenaName);
                player.sendMessage(MessageUtils.parseMessage("<green>Arena saved successfully!"));
            } else {
                // Clean up armor stands without saving
                this.clearArmorStands(arenaName);
            }

            // Remove from editing
            this.editingPlayers.remove(player.getUniqueId());
            this.playerEditingArena.remove(player.getUniqueId());
            this.playerSpawnMode.remove(player.getUniqueId());

            // Teleport back to lobby
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                this.plugin.getWorldManager().teleportToLobby(player);
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
            });

            player.sendMessage(MessageUtils.parseMessage("<yellow>Exited arena editor mode"));
            return true;
        });
    }

    /**
     * Handle spawn point placement
     */
    public void placeSpawnPoint(final Player player, final Location location) {
        if (!this.editingPlayers.contains(player.getUniqueId())) {
            return;
        }

        final String arenaName = this.playerEditingArena.get(player.getUniqueId());
        final SpawnPointType spawnType = this.playerSpawnMode.get(player.getUniqueId());
        
        if (arenaName == null || spawnType == null) {
            return;
        }

        // Create armor stand at location
        final ArmorStand armorStand = this.createSpawnArmorStand(location, spawnType);
        
        // Add to tracking
        this.arenaArmorStands.computeIfAbsent(arenaName, k -> new HashSet<>()).add(armorStand);
        
        player.sendMessage(MessageUtils.parseMessage("<green>Placed " + spawnType.getDisplayName() + " spawn point"));
    }

    /**
     * Handle spawn point removal
     */
    public void removeSpawnPoint(final Player player, final ArmorStand armorStand) {
        if (!this.editingPlayers.contains(player.getUniqueId())) {
            return;
        }

        final String arenaName = this.playerEditingArena.get(player.getUniqueId());
        if (arenaName == null) {
            return;
        }

        final Set<ArmorStand> armorStands = this.arenaArmorStands.get(arenaName);
        if (armorStands != null && armorStands.remove(armorStand)) {
            armorStand.remove();
            player.sendMessage(MessageUtils.parseMessage("<red>Removed spawn point"));
        }
    }

    /**
     * Change player's spawn point placement mode
     */
    public void changeSpawnMode(final Player player, final SpawnPointType spawnType) {
        if (!this.editingPlayers.contains(player.getUniqueId())) {
            return;
        }

        this.playerSpawnMode.put(player.getUniqueId(), spawnType);
        player.sendMessage(MessageUtils.parseMessage("<yellow>Changed spawn mode to: " + spawnType.getDisplayName()));
        
        // Update tool in hand
        this.giveEditorTools(player);
    }

    /**
     * Give editing tools to player
     */
    private void giveEditorTools(final Player player) {
        player.getInventory().clear();
        
        final SpawnPointType currentMode = this.playerSpawnMode.get(player.getUniqueId());
        
        // Spawn point placement tool
        final ItemStack placementTool = new ItemCreator(Material.BLAZE_ROD)
                .setDisplayName("<green><bold>Spawn Point Placer")
                .setLore(
                        "<gray>Right-click to place spawn points",
                        "<gray>Left-click armor stands to remove them",
                        "<gray>",
                        "<yellow>Current Mode: " + (currentMode != null ? currentMode.getDisplayName() : "None")
                )
                .setPersistentData("editor_tool", "spawn_placer")
                .setRarity(ItemRarity.EPIC)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        
        player.getInventory().setItem(0, placementTool);
        
        // Mode selection tools
        int slot = 2;
        for (final SpawnPointType spawnType : SpawnPointType.values()) {
            final ItemStack modeItem = new ItemCreator(spawnType.getMaterial())
                    .setDisplayName(spawnType.getDisplayName())
                    .setLore(
                            "<gray>Click to switch to this spawn mode",
                            "<gray>Compatible with: " + String.join(", ", 
                                spawnType.getCompatibleGamemodes().stream()
                                    .map(Gamemode::getDisplayName)
                                    .toArray(String[]::new))
                    )
                    .setPersistentData("editor_tool", "mode_" + spawnType.name().toLowerCase())
                    .setRarity(currentMode == spawnType ? ItemRarity.RARE : ItemRarity.COMMON)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            
            player.getInventory().setItem(slot++, modeItem);
        }
        
        // Save and exit tools
        final ItemStack saveItem = new ItemCreator(Material.EMERALD)
                .setDisplayName("<green><bold>Save & Exit")
                .setLore("<gray>Save changes and exit editor mode")
                .setPersistentData("editor_tool", "save_exit")
                .setRarity(ItemRarity.EPIC)
                .build();
        
        final ItemStack exitItem = new ItemCreator(Material.BARRIER)
                .setDisplayName("<red><bold>Exit Without Saving")
                .setLore("<gray>Exit editor mode without saving changes")
                .setPersistentData("editor_tool", "exit_no_save")
                .setRarity(ItemRarity.EPIC)
                .build();
        
        player.getInventory().setItem(7, saveItem);
        player.getInventory().setItem(8, exitItem);
    }

    /**
     * Create an armor stand for spawn point visualization
     */
    private ArmorStand createSpawnArmorStand(final Location location, final SpawnPointType spawnType) {
        final World world = location.getWorld();
        final ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        
        // Configure armor stand
        armorStand.setVisible(true);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        armorStand.customName(MessageUtils.parseMessage(spawnType.getDisplayName()));
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        
        // Set armor based on spawn type
        final ItemStack helmet = new ItemStack(spawnType.getHelmetMaterial());
        final ItemStack chestplate = this.createColoredLeatherArmor(Material.LEATHER_CHESTPLATE, spawnType.getArmorColor());
        
        armorStand.getEquipment().setHelmet(helmet);
        armorStand.getEquipment().setChestplate(chestplate);
        
        // Store spawn type in persistent data
        armorStand.getPersistentDataContainer().set(
            new NamespacedKey(this.plugin, "spawn_type"), 
            org.bukkit.persistence.PersistentDataType.STRING, 
            spawnType.name()
        );
        
        return armorStand;
    }

    /**
     * Spawn armor stands for existing spawn points
     */
    private void spawnArmorStands(final Arena arena, final World world) {
        final String arenaName = arena.getName().toLowerCase();
        
        // Clear existing armor stands
        this.clearArmorStands(arenaName);
        
        final Set<ArmorStand> armorStands = new HashSet<>();
        
        // Spawn armor stands for each spawn type
        for (final SpawnPointType spawnType : SpawnPointType.values()) {
            final List<Location> spawns = this.getSpawnsByType(arena, spawnType);
            
            for (final Location spawn : spawns) {
                final Location worldLocation = spawn.clone();
                worldLocation.setWorld(world);
                
                final ArmorStand armorStand = this.createSpawnArmorStand(worldLocation, spawnType);
                armorStands.add(armorStand);
            }
        }
        
        this.arenaArmorStands.put(arenaName, armorStands);
    }

    /**
     * Save the entire arena including armor stands to schematic file
     */
    private void saveArenaWithArmorStands(final String arenaName) {
        final Arena arena = this.plugin.getArenaManager().getArenas().get(arenaName);
        if (arena == null) {
            this.plugin.logError("Arena not found for saving: " + arenaName);
            return;
        }

        final World editorWorld = this.plugin.getWorldManager().getArenaEditorWorld();
        if (editorWorld == null) {
            this.plugin.logError("Editor world not available for saving arena");
            return;
        }

        // Save the arena with armor stands using ArenaManager
        this.plugin.getArenaManager().saveArenaSchematic(arenaName, editorWorld).thenAccept(success -> {
            if (success) {
                this.plugin.logInfo("Successfully saved arena with armor stands: " + arenaName);
                // Clear armor stands after successful save
                this.clearArmorStands(arenaName);
            } else {
                this.plugin.logError("Failed to save arena schematic: " + arenaName);
            }
        });
    }

    /**
     * Clear all armor stands for an arena
     */
    private void clearArmorStands(final String arenaName) {
        final Set<ArmorStand> armorStands = this.arenaArmorStands.remove(arenaName);
        if (armorStands != null) {
            armorStands.forEach(ArmorStand::remove);
        }
    }

    /**
     * Get spawn points by type from arena
     */
    private List<Location> getSpawnsByType(final Arena arena, final SpawnPointType spawnType) {
        return switch (spawnType) {
            case RED_SPAWN -> arena.getRedSpawns();
            case BLUE_SPAWN -> arena.getBlueSpawns();
            case FREE_FOR_ALL_SPAWN -> arena.getFreeForAllSpawns();
            case FLAG_RED_SPAWN -> arena.getRedFlagSpawns();
            case FLAG_BLUE_SPAWN -> arena.getBlueFlagSpawns();
        };
    }

    /**
     * Clear spawn points by type from arena
     */
    private void clearSpawnsByType(final Arena arena, final SpawnPointType spawnType) {
        this.getSpawnsByType(arena, spawnType).clear();
    }

    /**
     * Add spawn point by type to arena
     */
    private void addSpawnByType(final Arena arena, final SpawnPointType spawnType, final Location location) {
        this.getSpawnsByType(arena, spawnType).add(location);
    }

    /**
     * Create colored leather armor piece
     */
    private ItemStack createColoredLeatherArmor(final Material material, final Color color) {
        final ItemStack armor = new ItemStack(material);
        final LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            armor.setItemMeta(meta);
        }
        return armor;
    }

    /**
     * Scan for armor stands in the arena world and extract spawn points
     */
    public Map<SpawnPointType, List<Location>> scanArmorStandsForSpawns(final World gameWorld) {
        final Map<SpawnPointType, List<Location>> spawnPoints = new HashMap<>();
        
        // Scan all armor stands in the game world
        gameWorld.getEntitiesByClass(ArmorStand.class).forEach(armorStand -> {
            final String spawnTypeStr = armorStand.getPersistentDataContainer().get(
                new NamespacedKey(this.plugin, "spawn_type"), 
                org.bukkit.persistence.PersistentDataType.STRING
            );
            
            if (spawnTypeStr != null) {
                try {
                    final SpawnPointType spawnType = SpawnPointType.valueOf(spawnTypeStr);
                    final Location location = armorStand.getLocation().clone();
                    
                    spawnPoints.computeIfAbsent(spawnType, k -> new ArrayList<>()).add(location);
                    
                    this.plugin.logInfo("Found spawn point: " + spawnType + " at " + 
                        String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()));
                        
                } catch (final IllegalArgumentException e) {
                    this.plugin.logError("Invalid spawn type in armor stand: " + spawnTypeStr);
                }
            }
        });
        
        return spawnPoints;
    }

    /**
     * Remove all spawn armor stands from the game world after extracting spawn points
     */
    public void removeSpawnArmorStands(final World gameWorld) {
        final List<ArmorStand> spawnArmorStands = new ArrayList<>();
        
        // Find all spawn armor stands
        gameWorld.getEntitiesByClass(ArmorStand.class).forEach(armorStand -> {
            final String spawnTypeStr = armorStand.getPersistentDataContainer().get(
                new NamespacedKey(this.plugin, "spawn_type"), 
                org.bukkit.persistence.PersistentDataType.STRING
            );
            
            if (spawnTypeStr != null) {
                spawnArmorStands.add(armorStand);
            }
        });
        
        // Remove all spawn armor stands
        spawnArmorStands.forEach(ArmorStand::remove);
        
        this.plugin.logInfo("Removed " + spawnArmorStands.size() + " spawn armor stands from game world");
    }

    /**
     * Enumeration of spawn point types
     */
    public enum SpawnPointType {
        RED_SPAWN("Red Team Spawn", Material.RED_CONCRETE, Color.RED, Set.of(Gamemode.TEAM_DEATHMATCH, Gamemode.FLAG_RUSH, Gamemode.JUGGERNAUT)),
        BLUE_SPAWN("Blue Team Spawn", Material.BLUE_CONCRETE, Color.BLUE, Set.of(Gamemode.TEAM_DEATHMATCH, Gamemode.FLAG_RUSH, Gamemode.JUGGERNAUT)),
        FREE_FOR_ALL_SPAWN("Free For All Spawn", Material.WHITE_CONCRETE, Color.WHITE, Set.of(Gamemode.FREE_FOR_ALL)),
        FLAG_RED_SPAWN("Red Flag Spawn", Material.RED_BANNER, Color.RED, Set.of(Gamemode.FLAG_RUSH)),
        FLAG_BLUE_SPAWN("Blue Flag Spawn", Material.BLUE_BANNER, Color.BLUE, Set.of(Gamemode.FLAG_RUSH));

        @Getter private final String displayName;
        @Getter private final Material material;
        @Getter private final Material helmetMaterial;
        @Getter private final Color armorColor;
        @Getter private final Set<Gamemode> compatibleGamemodes;

        SpawnPointType(final String displayName, final Material material, final Color armorColor, final Set<Gamemode> compatibleGamemodes) {
            this.displayName = "<" + armorColor.toString().toLowerCase() + ">" + displayName;
            this.material = material;
            this.helmetMaterial = material;
            this.armorColor = armorColor;
            this.compatibleGamemodes = compatibleGamemodes;
        }
    }
}