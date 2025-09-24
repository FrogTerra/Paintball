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
    private int flagKills = 0; // Kills while carrying flag
    private int flagCarrierKills = 0; // Kills of flag carriers
    
    // Juggernaut Specific Stats
    private int juggernautKills = 0; // Kills as juggernaut
    private int juggernautDeaths = 0; // Deaths as juggernaut
    private int juggernautKillsAgainst = 0; // Kills against juggernauts
    private int playerKills = 0; // Kills as regular player in juggernaut mode
    private int playerDeaths = 0; // Deaths as regular player in juggernaut mode
    private long juggernautSurvivalTime = 0; // Time survived as juggernaut (milliseconds)

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
    
    /**
     * Add a kill while carrying flag
     */
    public void addFlagKill() {
        this.flagKills++;
        this.kills++; // Also count as regular kill
    }
    
    /**
     * Add a kill of a flag carrier
     */
    public void addFlagCarrierKill() {
        this.flagCarrierKills++;
        this.kills++; // Also count as regular kill
    }
    
    /**
     * Add a kill as juggernaut
     */
    public void addJuggernautKill() {
        this.juggernautKills++;
        this.kills++; // Also count as regular kill
    }
    
    /**
     * Add a death as juggernaut
     */
    public void addJuggernautDeath() {
        this.juggernautDeaths++;
        this.deaths++; // Also count as regular death
    }
    
    /**
     * Add a kill against a juggernaut
     */
    public void addJuggernautKillAgainst() {
        this.juggernautKillsAgainst++;
        this.kills++; // Also count as regular kill
    }
    
    /**
     * Add a kill as regular player in juggernaut mode
     */
    public void addPlayerKill() {
        this.playerKills++;
        this.kills++; // Also count as regular kill
    }
    
    /**
     * Add a death as regular player in juggernaut mode
     */
    public void addPlayerDeath() {
        this.playerDeaths++;
        this.deaths++; // Also count as regular death
    }
    
    /**
     * Add survival time as juggernaut
     */
    public void addJuggernautSurvivalTime(long milliseconds) {
        this.juggernautSurvivalTime += milliseconds;
    }
}
