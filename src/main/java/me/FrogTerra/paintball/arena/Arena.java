package me.FrogTerra.paintball.arena;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.FrogTerra.paintball.game.Gamemode;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
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
    private boolean enabled = false; // Disabled by default upon creation
    
    @SerializedName("compatibleGameModes")
    private Set<Gamemode> compatibleGameModes = new HashSet<>();

    // Team spawn points
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

    // Arena boundaries (optional)
    @SerializedName("minBoundary")
    private Location minBoundary;
    
    @SerializedName("maxBoundary")
    private Location maxBoundary;

    public Arena(final String name, final String schematicFile) {
        this.name = name;
        this.schematicFile = schematicFile;
        this.enabled = false; // Disabled by default
        this.compatibleGameModes = new HashSet<>();
    }

    /**
     * Check if arena is compatible with gamemode
     */
    public boolean isCompatible(final Gamemode gameMode) {
        return this.enabled && 
               this.compatibleGameModes != null && 
               this.compatibleGameModes.contains(gameMode) &&
               this.hasRequiredSpawns(gameMode);
    }

    /**
     * Get spawn points for a specific team
     */
    public List<Location> getTeamSpawns(final SpawnType spawnType) {
        return switch (spawnType) {
            case RED_SPAWN -> this.redSpawns;
            case BLUE_SPAWN -> this.blueSpawns;
            case FREE_FOR_ALL_SPAWN -> this.freeForAllSpawns;
            case RED_FLAG_SPAWN -> this.redFlagSpawns;
            case BLUE_FLAG_SPAWN -> this.blueFlagSpawns;
        };
    }

    /**
     * Add a spawn point for a team
     */
    public void addSpawn(final SpawnType spawnType, final Location location) {
        if (location == null) return;
        
        // Create location without world reference for storage
        final Location spawnLocation = new Location(null, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), 0.0f); // Fixed pitch looking straight ahead
        this.getTeamSpawns(spawnType).add(spawnLocation);
    }

    /**
     * Remove a spawn point
     */
    public boolean removeSpawn(final SpawnType spawnType, final Location location) {
        if (location == null) return false;
        
        final List<Location> spawns = this.getTeamSpawns(spawnType);
        return spawns.removeIf(spawn -> 
            Math.abs(spawn.getX() - location.getX()) < 0.5 &&
            Math.abs(spawn.getY() - location.getY()) < 0.5 &&
            Math.abs(spawn.getZ() - location.getZ()) < 0.5
        );
    }

    /**
     * Clear all spawn points for a team
     */
    public void clearSpawns(final SpawnType spawnType) {
        this.getTeamSpawns(spawnType).clear();
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
        if (this.minBoundary == null || this.maxBoundary == null || location == null) {
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
        if (this.name == null || this.name.trim().isEmpty()) {
            return false;
        }

        if (this.schematicFile == null || this.schematicFile.trim().isEmpty()) {
            return false;
        }

        if (this.compatibleGameModes == null || this.compatibleGameModes.isEmpty()) {
            return false;
        }

        // Check if arena has required spawns for compatible gamemodes
        for (final Gamemode gameMode : this.compatibleGameModes) {
            if (!this.hasRequiredSpawns(gameMode)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if arena has required spawns for a gamemode
     */
    private boolean hasRequiredSpawns(final Gamemode gameMode) {
        return switch (gameMode) {
            case TEAM_DEATHMATCH, JUGGERNAUT -> 
                this.redSpawns.size() >= 2 && this.blueSpawns.size() >= 2;
            case FREE_FOR_ALL -> 
                this.freeForAllSpawns.size() >= gameMode.getMinPlayers();
            case FLAG_RUSH -> 
                this.redSpawns.size() >= 2 && this.blueSpawns.size() >= 2 && 
                this.redFlagSpawns.size() >= 1 && this.blueFlagSpawns.size() >= 1;
        };
    }

    /**
     * Arena spawn type enumeration
     */
    public enum SpawnType {
        RED_SPAWN,
        BLUE_SPAWN,
        FREE_FOR_ALL_SPAWN,
        RED_FLAG_SPAWN,
        BLUE_FLAG_SPAWN
    }
}