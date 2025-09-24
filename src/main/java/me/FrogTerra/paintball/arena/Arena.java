package me.FrogTerra.paintball.arena;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.FrogTerra.paintball.game.Gamemode;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a paintball arena with spawn points and metadata
 */
@Data
@NoArgsConstructor
public final class Arena {

    @SerializedName("name")
    private String name;
    @SerializedName("schematicFile")
    private String schematicFile;
    @SerializedName("enabled")
    private boolean enabled = true;
    @SerializedName("compatibleGameModes")
    private Set<Gamemode> compatibleGameModes;

    // Spawn points based on banner colors
    @SerializedName("redSpawns")
    private List<Location> redSpawns = new ArrayList<>();
    @SerializedName("blueSpawns")
    private List<Location> blueSpawns = new ArrayList<>();
    @SerializedName("freeForAllSpawns")
    private List<Location> freeForAllSpawns = new ArrayList<>();

    // Flag spawn points for Flag Rush gamemode
    @SerializedName("redFlagSpawns")
    private List<Location> redFlagSpawns = new ArrayList<>();
    @SerializedName("blueFlagSpawns")
    private List<Location> blueFlagSpawns = new ArrayList<>();

    // Arena boundaries
    @SerializedName("minBoundary")
    private Location minBoundary;
    @SerializedName("maxBoundary")
    private Location maxBoundary;

    public Arena(final String name, final String schematicFile) {
        this.name = name;
        this.schematicFile = schematicFile;
        this.enabled = false; // Automatically disable new arenas
    }

    /**
     * Check if arena is compatible with gamemode
     */
    public boolean isCompatible(final Gamemode gameMode) {
        return this.enabled && this.compatibleGameModes != null && this.compatibleGameModes.contains(gameMode);
    }

    /**
     * Get spawn points for a specific team
     */
    public List<Location> getTeamSpawns(final ArenaTeam team) {
        return switch (team) {
            case RED -> this.redSpawns;
            case BLUE -> this.blueSpawns;
            case FREE_FOR_ALL -> this.freeForAllSpawns;
        };
    }

    /**
     * Get flag spawn points for a specific team
     */
    public List<Location> getFlagSpawns(final ArenaTeam team) {
        return switch (team) {
            case RED -> this.redFlagSpawns;
            case BLUE -> this.blueFlagSpawns;
            case FREE_FOR_ALL -> new ArrayList<>(); // No flags in FFA
        };
    }

    /**
     * Add a spawn point for a team
     */
    public void addSpawn(final ArenaTeam team, final Location location) {
        // Create location without world reference for storage
        final Location spawnLocation = new Location(null, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        this.getTeamSpawns(team).add(spawnLocation);
    }

    /**
     * Add a flag spawn point for a team
     */
    public void addFlagSpawn(final ArenaTeam team, final Location location) {
        // Create location without world reference for storage
        final Location spawnLocation = new Location(null, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        this.getFlagSpawns(team).add(spawnLocation);
    }

    /**
     * Clear all spawn points for a team
     */
    public void clearSpawns(final ArenaTeam team) {
        this.getTeamSpawns(team).clear();
    }

    /**
     * Clear all flag spawn points for a team
     */
    public void clearFlagSpawns(final ArenaTeam team) {
        this.getFlagSpawns(team).clear();
    }

    /**
     * Get total number of spawn points
     */
    public int getTotalSpawns() {
        return this.redSpawns.size() + this.blueSpawns.size() + this.freeForAllSpawns.size();
    }

    /**
     * Get total number of flag spawn points
     */
    public int getTotalFlagSpawns() {
        return this.redFlagSpawns.size() + this.blueFlagSpawns.size();
    }

    /**
     * Check if location is within arena boundaries
     */
    public boolean isWithinBoundaries(final Location location) {
        if (this.minBoundary == null || this.maxBoundary == null) {
            return true; // No boundaries set
        }

        return location.getX() >= this.minBoundary.getX() && location.getX() <= this.maxBoundary.getX() &&
                location.getY() >= this.minBoundary.getY() && location.getY() <= this.maxBoundary.getY() &&
                location.getZ() >= this.minBoundary.getZ() && location.getZ() <= this.maxBoundary.getZ();
    }

    /**
     * Validate arena configuration
     */
    public boolean isValid() {
        if (this.name == null || this.name.isEmpty()) {
            return false;
        }

        if (this.schematicFile == null || this.schematicFile.isEmpty()) {
            return false;
        }

        if (this.compatibleGameModes == null || this.compatibleGameModes.isEmpty()) {
            return false;
        }

        // Check if arena has required spawns for compatible gamemodes
        for (final Gamemode gameMode : this.compatibleGameModes)
            if (!this.hasRequiredSpawns(gameMode)) return false;

        return true;
    }

    /**
     * Check if arena has required spawns for a gamemode
     */
    private boolean hasRequiredSpawns(final Gamemode gameMode) {
        return switch (gameMode) {
            case TEAM_DEATHMATCH, JUGGERNAUT -> this.redSpawns.size() >= 2 && this.blueSpawns.size() >= 2;
            case FREE_FOR_ALL -> this.freeForAllSpawns.size() >= 2;
            case FLAG_RUSH -> this.redSpawns.size() >= 2 && this.blueSpawns.size() >= 2 && 
                             this.redFlagSpawns.size() >= 1 && this.blueFlagSpawns.size() >= 1;
        };
    }

    /**
     * Arena team enumeration
     */
    public enum ArenaTeam {
        RED,
        BLUE,
        FREE_FOR_ALL,
    }
}
