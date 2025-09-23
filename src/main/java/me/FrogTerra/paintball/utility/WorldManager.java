package me.FrogTerra.paintball.utility;

import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import org.bukkit.*;
import org.bukkit.entity.Player;

public final class WorldManager {

    private final Paintball plugin;

    @Getter private World lobbyWorld;
    @Getter private World arenaWorld;
    @Getter private World arenaEditorWorld;

    public WorldManager(Paintball plugin) {
        this.plugin = plugin;
        initializeWorlds();
    }

    /**
     * Initialize all required worlds
     */
    private void initializeWorlds() {
        // Create lobby world
        String lobbyWorldName = "lobby";
        this.lobbyWorld = this.createVoidWorld(lobbyWorldName);
        if (this.lobbyWorld != null) {
            this.setupLobbyWorld();
            this.plugin.logInfo("Lobby world '" + lobbyWorldName + "' ready");
        } else {
            this.plugin.logError("Failed to create lobby world: " + lobbyWorldName);
        }

        // Create arena world
        String arenaWorldName = "arena";
        this.arenaWorld = this.createVoidWorld(arenaWorldName);
        if (this.arenaWorld != null) {
            this.setuparenaWorld();
            this.plugin.logInfo("Lobby world '" + arenaWorldName + "' ready");
        } else {
            this.plugin.logError("Failed to create lobby world: " + arenaWorldName);
        }
    }

    /**
     * Create a void world using custom generator
     */
    private World createVoidWorld(final String worldName) {
        // Check if world already exists
        World existingWorld = Bukkit.getWorld(worldName);
        if (existingWorld != null) {
            this.plugin.logInfo("Void world '" + worldName + "' already exists, using existing world");
            return existingWorld;
        }

        this.plugin.logInfo("Creating void world: " + worldName);

        try {
            final WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new VoidWorldGenerator());
            creator.generateStructures(false);
            creator.environment(World.Environment.NORMAL);

            final World world = creator.createWorld();

            if (world != null) {
                this.plugin.logInfo("✓ Void world created successfully: " + worldName);
                return world;
            } else {
                this.plugin.logError("✗ Void world creation returned null: " + worldName);
                return null;
            }

        } catch (final Exception exception) {
            this.plugin.logError("✗ Void world creation failed for " + worldName + ": " + exception.getMessage());
            return null;
        }
    }
    /**
     * Teleport player to lobby world spawn
     */
    public boolean teleportToLobby(final Player player) {
        if (this.lobbyWorld == null) {
            this.plugin.logError("Cannot teleport to lobby - lobby world is null");
            return false;
        }

        try {
            final Location spawnLocation = this.lobbyWorld.getSpawnLocation();
            // Ensure spawn location is safe
            spawnLocation.setY(Math.max(spawnLocation.getY(), 64));

            final boolean success = player.teleport(spawnLocation);
            if (success) {
                this.plugin.logInfo("Teleported " + player.getName() + " to lobby");
            } else {
                this.plugin.logError("Failed to teleport " + player.getName() + " to lobby");
            }
            return success;
        } catch (final Exception exception) {
            this.plugin.logError("Exception teleporting " + player.getName() + " to lobby", exception);
            return false;
        }
    }

    /**
     * Setup lobby world properties
     */
    private void setupLobbyWorld() {
        if (this.lobbyWorld == null) return;

        try {
            this.lobbyWorld.setTime(6000); // Noon
            this.lobbyWorld.setStorm(false);
            this.lobbyWorld.setThundering(false);
            this.lobbyWorld.setWeatherDuration(0);

            // Set game rules
            this.setGameRule(this.lobbyWorld, "doDaylightCycle", "false");
            this.setGameRule(this.lobbyWorld, "doWeatherCycle", "false");
            this.setGameRule(this.lobbyWorld, "doMobSpawning", "false");
            this.setGameRule(this.lobbyWorld, "keepInventory", "true");
            this.setGameRule(this.lobbyWorld, "showDeathMessages", "false");
            this.setGameRule(this.lobbyWorld, "announceAdvancements", "false");
            this.setGameRule(this.lobbyWorld, "doImmediateRespawn", "true");

            // Set spawn location to a safe spot
            this.lobbyWorld.setSpawnLocation(0, 64, 0);

        } catch (final Exception exception) {
            this.plugin.logError("Failed to setup lobby world", exception);
        }
    }

    /**
     * Setup arena world properties
     */
    private void setuparenaWorld() {
        if (this.arenaWorld == null) return;

        try {
            this.arenaWorld.setTime(6000); // Noon
            this.arenaWorld.setStorm(false);
            this.arenaWorld.setThundering(false);
            this.arenaWorld.setWeatherDuration(0);

            // Set game rules for arena
            this.setGameRule(this.arenaWorld, "doDaylightCycle", "false");
            this.setGameRule(this.arenaWorld, "doWeatherCycle", "false");
            this.setGameRule(this.arenaWorld, "doMobSpawning", "false");
            this.setGameRule(this.arenaWorld, "keepInventory", "false");
            this.setGameRule(this.arenaWorld, "showDeathMessages", "false");
            this.setGameRule(this.arenaWorld, "doTileDrops", "false");
            this.setGameRule(this.arenaWorld, "mobGriefing", "false");
            this.setGameRule(this.arenaWorld, "announceAdvancements", "false");
            this.setGameRule(this.arenaWorld, "doFireTick", "false");
            this.setGameRule(this.arenaWorld, "doImmediateRespawn", "true");

            // Set spawn location high up for void worlds
            this.arenaWorld.setSpawnLocation(0, 100, 0);

        } catch (final Exception exception) {
            this.plugin.logError("Failed to setup arena world", exception);
        }
    }

    /**
     * Safely set a game rule
     */
    private void setGameRule(final World world, final String rule, final String value) {
        try {
            final GameRule<?> gameRule = GameRule.getByName(rule);
            if (gameRule != null) {
                if (gameRule.getType() == Boolean.class) {
                    world.setGameRule((GameRule<Boolean>) gameRule, Boolean.parseBoolean(value));
                } else if (gameRule.getType() == Integer.class) {
                    world.setGameRule((GameRule<Integer>) gameRule, Integer.parseInt(value));
                } else {
                    this.plugin.logError("Unsupported game rule type for " + rule + ": " + gameRule.getType());
                }
            } else {
                this.plugin.logError("Unknown game rule: " + rule);
            }
        } catch (final Exception exception) {
            this.plugin.logError("Failed to set game rule " + rule + " to " + value + " in world " + world.getName());
        }
    }

    /**
     * Cleanup worlds on plugin disable
     */
    public void cleanup() {
        this.plugin.logInfo("Starting world cleanup...");

        try {
            // Save all worlds
            if (this.lobbyWorld != null) {
                this.lobbyWorld.save();
                this.plugin.logInfo("Saved lobby world");
            }

            this.plugin.logInfo("World cleanup completed successfully");

        } catch (final Exception exception) {
            this.plugin.logError("Error during world cleanup", exception);
        }
    }
}
