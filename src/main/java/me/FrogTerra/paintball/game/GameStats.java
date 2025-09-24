package me.FrogTerra.paintball.game;

import lombok.Data;

import java.util.UUID;

/**
 * Represents a player's in-game stats
 */
@Data
public class GameStats {

    // General Stats
    private final UUID uuid;
    private int kills = 0;
    private int deaths = 0;
    private int shots = 0;

    // Flag Rush Specific Stats
    private int flagCaptures = 0;
    private int flagReturns = 0;

    public GameStats(UUID uuid) {
        this.uuid = uuid;
    }
    
    /**
     * Add a kill to the stats
     */
    public void addKill() {
        this.kills++;
    }
    
    /**
     * Add a death to the stats
     */
    public void addDeath() {
        this.deaths++;
    }
    
    /**
     * Add a shot to the stats
     */
    public void addShot() {
        this.shots++;
    }
    
    /**
     * Add a flag capture to the stats
     */
    public void addFlagCapture() {
        this.flagCaptures++;
    }
    
    /**
     * Add a flag return to the stats
     */
    public void addFlagReturn() {
        this.flagReturns++;
    }
}
