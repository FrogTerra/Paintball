package me.FrogTerra.paintball.arena;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.utility.LocationAdapter;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ArenaManager {

    private final Paintball plugin;
    private final Gson gson;
    private final File schematicsFolder;
    private final File arenasConfigFile;

    @Getter private final Map<String, Arena> arenas = new HashMap<>();
    @Getter private Arena currentArena;

    public ArenaManager(Paintball plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Location.class, new LocationAdapter())
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes field) {
                        // Skip any fields that might cause serialization issues
                        return field.getName().equals("referent") ||
                                field.getName().equals("queue") ||
                                field.getName().equals("discovered");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        // Skip problematic classes
                        return clazz.getName().contains("java.lang.ref") ||
                                clazz.getName().contains("java.util.concurrent");
                    }
                })
                .create();

        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        this.arenasConfigFile = new File(plugin.getDataFolder(), "arenas.json");

        if (!this.schematicsFolder.exists()) {
            this.schematicsFolder.mkdirs();
        }
        // Loading Arenas
        if (!this.arenasConfigFile.exists()) {
            this.saveArenas();
            return;
        }

        try (final FileReader reader = new FileReader(this.arenasConfigFile)) {
            final Type type = new TypeToken<Map<String, Arena>>(){}.getType();
            final Map<String, Arena> loadedArenas = this.gson.fromJson(reader, type);

            if (loadedArenas != null) {
                this.arenas.putAll(loadedArenas);
                this.plugin.logInfo("Loaded " + loadedArenas.size() + " arenas from configuration");
            } else {
                this.plugin.logInfo("No arenas found in configuration file, starting with empty arena list");
            }
        } catch (final IOException exception) {
            this.plugin.logError("Failed to load arenas configuration", exception);
        } catch (final com.google.gson.JsonSyntaxException exception) {
            this.plugin.logError("Invalid JSON in arenas configuration, creating default arenas", exception);
            this.saveArenas();
        }

        this.plugin.logInfo("Arena manager initialized with " + this.arenas.size() + " arenas");
    }

    public void saveArenas() {
        try {
            // Ensure parent directory exists
            if (!this.arenasConfigFile.getParentFile().exists()) {
                this.arenasConfigFile.getParentFile().mkdirs();
            }

            try (final FileWriter writer = new FileWriter(this.arenasConfigFile)) {
                this.gson.toJson(this.arenas, writer);
                writer.flush();
                this.plugin.logInfo("Saved " + this.arenas.size() + " arenas to configuration");
            }
        } catch (final IOException exception) {
            this.plugin.logError("Failed to save arenas configuration", exception);
        } catch (final Exception exception) {
            this.plugin.logError("Unexpected error saving arenas configuration", exception);
        }
    }

    /**
     * Load an arena schematic into a specific world
     */
    private CompletableFuture<Boolean> loadArenaInWorld(final String arenaName, final World targetWorld) {
        return CompletableFuture.supplyAsync(() -> {
            final Arena arena = this.arenas.get(arenaName.toLowerCase());
            if (arena == null) {
                this.plugin.logError("Arena not found: " + arenaName);
                return false;
            }

            final File schematicFile = new File(this.schematicsFolder, arena.getSchematicFile());
            if (!schematicFile.exists()) {
                this.plugin.logError("Schematic file not found: " + arena.getSchematicFile());
                return false;
            }

            try {
                if (targetWorld == null) {
                    this.plugin.logError("Arena world not available");
                    return false;
                }

                // Clear the arena world first
                // TODO:: Clear the schematic on game completion-= this.clearWorld(targetWorld);

                // Load and paste the schematic
                final ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    this.plugin.logError("Unsupported schematic format: " + schematicFile.getName());
                    return false;
                }

                try (final ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    final Clipboard clipboard = reader.read();

                    try (final EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(targetWorld))) {
                        final Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(0, 100, 0))
                                .build();

                        Operations.complete(operation);
                        editSession.flushSession();
                    }
                }

                // Scan for spawn points
                // Always scan spawn points when loading arena
                // TODO:: Scan for armor stand -= this.scanSpawnPoints(arena, targetWorld);

                this.currentArena = arena;

                this.plugin.logInfo("Successfully loaded arena: " + arenaName);
                return true;

            } catch (final Exception exception) {
                this.plugin.logError("Failed to load arena: " + arenaName, exception);
                return false;
            }
        });
    }

    /**
     * Unload an arena by setting all blocks to air based on the arena's schematic dimensions
     */
    private CompletableFuture<Boolean> unloadArenaInWorld(final String arenaName, final World targetWorld) {
        return CompletableFuture.supplyAsync(() -> {
            final Arena arena = this.arenas.get(arenaName.toLowerCase());
            if (arena == null) {
                this.plugin.logError("Arena not found for unloading: " + arenaName);
                return false;
            }

            final File schematicFile = new File(this.schematicsFolder, arena.getSchematicFile());
            if (!schematicFile.exists()) {
                this.plugin.logError("Schematic file not found for unloading: " + arena.getSchematicFile());
                return false;
            }

            try {
                if (targetWorld == null) {
                    this.plugin.logError("Target world not available for unloading arena");
                    return false;
                }

                // Load the schematic to get its dimensions
                final ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    this.plugin.logError("Unsupported schematic format for unloading: " + schematicFile.getName());
                    return false;
                }

                try (final ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    final Clipboard clipboard = reader.read();
                    // Get the schematic dimensions and calculate the region to clear
                    final BlockVector3 schematicMin = clipboard.getMinimumPoint();
                    final BlockVector3 schematicMax = clipboard.getMaximumPoint();

                    // Calculate the actual world coordinates where the schematic was pasted
                    // The schematic was pasted at (0, 100, 0) in loadArenaInWorld
                    final BlockVector3 pasteLocation = BlockVector3.at(0, 100, 0);
                    final BlockVector3 offset = pasteLocation.subtract(clipboard.getOrigin());

                    final BlockVector3 worldMin = schematicMin.add(offset);
                    final BlockVector3 worldMax = schematicMax.add(offset);

                    final CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(targetWorld), worldMin, worldMax);

                    try (final EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(targetWorld))) {
                        // Set all blocks in the region to air
                        editSession.setBlocks((Region) region, BlockTypes.AIR.getDefaultState());
                        editSession.close();
                    }
                }

                // Clear the current arena reference
                this.currentArena = null;
                this.plugin.logInfo("Successfully unloaded arena: " + arenaName);
                return true;

            } catch (final Exception exception) {
                this.plugin.logError("Failed to unload arena: " + arenaName, exception);
                return false;
            }
        });
    }

    /**
     * Load an arena schematic into the arena world
     */
    public CompletableFuture<Boolean> loadArena(final String arenaName) {
        return this.loadArenaInWorld(arenaName, this.plugin.getWorldManager().getArenaWorld());
    }

    /**
     * Load an arena schematic into the arena editor world
     */
    public CompletableFuture<Boolean> loadArenaInEditor(final String arenaName) {
        return this.loadArenaInWorld(arenaName, this.plugin.getWorldManager().getArenaEditorWorld());
    }
}
