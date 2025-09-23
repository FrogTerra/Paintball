package me.FrogTerra.paintball.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final Paintball plugin;
    private final Gson gson;
    private final File dataFolder;

    @Getter
    private final Map<UUID, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();

    public PlayerManager(Paintball plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFolder = new File(this.plugin.getDataFolder(), "players");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }

        this.plugin.logInfo("Player manager initialized");
    }

    /**
     * Load or create a player profile
     */
    public CompletableFuture<PlayerProfile> loadPlayerProfile(final Player player) {
        return CompletableFuture.supplyAsync(() -> {
            final UUID playerId = player.getUniqueId();

            // Check if already loaded
            PlayerProfile profile = this.playerProfiles.get(playerId);
            if (profile != null) {
                profile.updateLastPlayed();
                return profile;
            }

            // Try to load from file
            final File playerFile = new File(this.dataFolder, playerId + ".json");
            if (playerFile.exists()) {
                try (final FileReader reader = new FileReader(playerFile)) {
                    profile = this.gson.fromJson(reader, PlayerProfile.class);
                    if (profile != null) {
                        profile.setPlayerName(player.getName());
                        profile.updateLastPlayed();
                        profile.setPrestigeReminder(false);
                        this.playerProfiles.put(playerId, profile);
                        this.plugin.logInfo("Loaded profile for player: " + player.getName());
                        return profile;
                    }
                } catch (final IOException exception) {
                    this.plugin.logError("Failed to load player profile: " + player.getName(), exception);
                }
            }

            // Create new profile
            profile = new PlayerProfile(playerId, player.getName());
            this.playerProfiles.put(playerId, profile);
            this.savePlayerProfile(profile);
            this.plugin.logInfo("Created new profile for player: " + player.getName());

            return profile;
        });
    }

    /**
     * Save a player profile to file
     */
    public CompletableFuture<Boolean> savePlayerProfile(final PlayerProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            final File playerFile = new File(this.dataFolder, profile.getPlayerId() + ".json");

            try (final FileWriter writer = new FileWriter(playerFile)) {
                this.gson.toJson(profile, writer);
                return true;
            } catch (final IOException exception) {
                this.plugin.logError("Failed to save player profile: " + profile.getPlayerName(), exception);
                return false;
            }
        });
    }

    /**
     * Get a player profile by UUID
     */
    public PlayerProfile getPlayerProfile(final UUID playerId) {
        return this.playerProfiles.get(playerId);
    }

    /**
     * Get a player profile by player
     */
    public PlayerProfile getPlayerProfile(final Player player) {
        return this.getPlayerProfile(player.getUniqueId());
    }

    /**
     * Remove a player profile from memory
     */
    public void unloadPlayerProfile(final UUID playerId) {
        final PlayerProfile profile = this.playerProfiles.remove(playerId);
        if (profile != null) {
            this.savePlayerProfile(profile);
        }
    }

    /**
     * Save all loaded player profiles
     */
    public void saveAllPlayers() {
        this.plugin.logInfo("Saving all player profiles...");

        final CompletableFuture<?>[] futures = this.playerProfiles.values().stream()
                .map(this::savePlayerProfile)
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).thenRun(() -> {
            this.plugin.logInfo("All player profiles saved successfully");
        }).exceptionally(throwable -> {
            this.plugin.logError("Failed to save some player profiles", throwable);
            return null;
        });
    }

    /**
     * Check if player has played before
     */
    public boolean hasPlayedBefore(final UUID playerId) {
        final File playerFile = new File(this.dataFolder, playerId + ".json");
        return playerFile.exists();
    }

    /**
     * Get total number of registered players
     */
    public int getTotalPlayers() {
        final File[] files = this.dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        return files != null ? files.length : 0;
    }

    /**
     * Reset a player's statistics
     */
    public CompletableFuture<Boolean> resetPlayerStats(final UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            final PlayerProfile profile = this.playerProfiles.get(playerId);
            if (profile == null) {
                return false;
            }

            // Reset all statistics but keep progression
            profile.setTotalKills(0);
            profile.setTotalDeaths(0);
            profile.setTotalShots(0);
            profile.setTotalGamesPlayed(0);
            profile.setTotalWins(0);
            profile.setTotalLosses(0);
            profile.setTotalPlayTime(0);

            // Reset gamemode stats
            profile.getGameModeStats().values().forEach(stats -> {
                stats.setKills(0);
                stats.setDeaths(0);
                stats.setShots(0);
                stats.setWins(0);
                stats.setLosses(0);
                stats.setGamesPlayed(0);
                stats.setTotalPlayTime(0);
            });

            this.savePlayerProfile(profile);
            return true;
        });
    }

    /**
     * Get player profile by name (case-insensitive)
     */
    public PlayerProfile getPlayerProfileByName(final String playerName) {
        return this.playerProfiles.values().stream()
                .filter(profile -> profile.getPlayerName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);
    }
}