package me.FrogTerra.paintball.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import me.FrogTerra.paintball.game.Gamemode;

import java.util.*;

@Data
@NoArgsConstructor
public final class PlayerProfile {

    private UUID playerId;
    private String playerName;
    private long firstJoined;
    private long lastPlayed;
    private long totalPlayTime;

    // General Statistics
    private int level = 1;
    private int prestige = 0;
    private boolean prestigeReminder = false;
    private long experience = 0;
    private long coins = 0;
    private int totalKills = 0;
    private int totalDeaths = 0;
    private int totalShots = 0;
    private int totalGamesPlayed = 0;
    private int totalWins = 0;
    private int totalLosses = 0;

    // Gamemode-specific statistics
    private final Map<Gamemode, GameModeStats> gameModeStats = new HashMap<>();

    // Upgrades
    private int paintballCountLevel = 0;
    private int reloadSpeedLevel = 0;
    private int juggernautReloadSpeedLevel = 0;
    private int juggernautAmmoCountLevel = 0;
    private int juggernautHPLevel = 0;
    private int juggernautSpeedLevel = 0;
    private int ctfCarrySpeedLevel = 0;

    // Kit cooldown reductions
    private int makeshiftDoorCooldownLevel = 0;
    private int doubleJumpCooldownLevel = 0;
    private int springBootsCooldownLevel = 0;
    private int grenadeCooldownLevel = 0;
    private int uavCooldownLevel = 0;
    private int cloneCooldownLevel = 0;

    // Squad information
    private String squadName;
    private final List<String> unlockedSquadColors = new ArrayList<>();

    // Premium status
    private boolean isPremium = false;

    public PlayerProfile(final UUID playerId, final String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.firstJoined = System.currentTimeMillis();
        this.lastPlayed = System.currentTimeMillis();

        // Initialize gamemode stats
        for (final Gamemode gameMode : Gamemode.values()) {
            this.gameModeStats.put(gameMode, new GameModeStats());
        }

        // Unlock default squad color
        this.unlockedSquadColors.add("WHITE");
    }

    /**
     * Get gamemode-specific statistics
     */
    public GameModeStats getGameModeStats(final Gamemode gameMode) {
        return this.gameModeStats.computeIfAbsent(gameMode, k -> new GameModeStats());
    }

    /**
     * Add experience and handle level ups
     */
    public boolean addExperience(final long amount) {
        this.experience += amount;
        return this.checkLevelUp();
    }

    /**
     * Check if player should level up
     */
    private boolean checkLevelUp() {
        final long requiredExp = this.getRequiredExperience();
        if (this.experience >= requiredExp && this.level < 1000) {
            this.level++;
            this.experience -= requiredExp;
            return true;
        }
        return false;
    }

    /**
     * Get required experience for next level
     */
    public long getRequiredExperience() {
        return (long) (1000 * Math.pow(1.1, this.level - 1));
    }

    /**
     * Calculate K/D ratio
     */
    public double getKDRatio() {
        if (this.totalDeaths == 0) {
            return this.totalKills;
        }
        return (double) this.totalKills / this.totalDeaths;
    }

    /**
     * Calculate win rate
     */
    public double getWinRate() {
        if (this.totalGamesPlayed == 0) {
            return 0.0;
        }
        return (double) this.totalWins / this.totalGamesPlayed * 100;
    }

    /**
     * Add coins
     */
    public void addCoins(final long amount) {
        this.coins += amount;
    }

    /**
     * Remove coins (returns true if successful)
     */
    public boolean removeCoins(final long amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }

    /**
     * Check if player can prestige
     */
    public boolean canPrestige() {
        return this.level >= 1000 && this.prestige < 10;
    }

    /**
     * Prestige the player
     */
    public boolean prestige() {
        if (!this.canPrestige())
            return false;

        this.prestige++;
        this.level = 1;
        this.experience = 0;
        return true;
    }

    /**
     * Has the player reached prestige 1 or higher
     */
    public boolean hasPrestige() {
        return prestige > 0;
    }

    /**
     * Update last played time
     */
    public void updateLastPlayed() {
        this.lastPlayed = System.currentTimeMillis();
    }

    /**
     * Add play time in milliseconds
     */
    public void addPlayTime(final long milliseconds) {
        this.totalPlayTime += milliseconds;
    }

    /**
     * Get current paintball count based on upgrades
     */
    public int getCurrentPaintballCount() {
        return 40 + (this.paintballCountLevel * 5); // 40 base + 5 per level (max 90)
    }

    /**
     * Get current reload speed based on upgrades
     */
    public double getCurrentReloadSpeed() {
        return 5.0 - (this.reloadSpeedLevel * 0.5); // 5.0 base - 0.5 per level (min 3.0)
    }

    /**
     * Inner class for gamemode-specific statistics
     */
    @Data
    @NoArgsConstructor
    public static final class GameModeStats {
        private int kills = 0;
        private int deaths = 0;
        private int shots = 0;
        private int wins = 0;
        private int losses = 0;
        private int gamesPlayed = 0;
        private long totalPlayTime = 0;
        
        // Flag Rush specific stats
        private int flagCaptures = 0;
        private int flagReturns = 0;
        private int flagKills = 0; // Kills while carrying flag
        private int flagCarrierKills = 0; // Kills of flag carriers
        
        // Juggernaut specific stats
        private int juggernautKills = 0; // Kills as juggernaut
        private int juggernautDeaths = 0; // Deaths as juggernaut
        private int juggernautKillsAgainst = 0; // Kills against juggernauts
        private int playerKills = 0; // Kills as regular player in juggernaut mode
        private int playerDeaths = 0; // Deaths as regular player in juggernaut mode
        private long juggernautSurvivalTime = 0; // Time survived as juggernaut (milliseconds)
        private int juggernautGamesWon = 0; // Games won as juggernaut
        private int playerGamesWon = 0; // Games won as regular player

        public double getKDRatio() {
            if (this.deaths == 0) {
                return this.kills;
            }
            return (double) this.kills / this.deaths;
        }

        public double getWinRate() {
            if (this.gamesPlayed == 0) {
                return 0.0;
            }
            return (double) this.wins / this.gamesPlayed * 100;
        }
        
        /**
         * Get juggernaut-specific K/D ratio
         */
        public double getJuggernautKDRatio() {
            if (this.juggernautDeaths == 0) {
                return this.juggernautKills;
            }
            return (double) this.juggernautKills / this.juggernautDeaths;
        }
        
        /**
         * Get player-specific K/D ratio (in juggernaut mode)
         */
        public double getPlayerKDRatio() {
            if (this.playerDeaths == 0) {
                return this.playerKills;
            }
            return (double) this.playerKills / this.playerDeaths;
        }
        
        /**
         * Get flag capture efficiency (captures per game)
         */
        public double getFlagCaptureEfficiency() {
            if (this.gamesPlayed == 0) {
                return 0.0;
            }
            return (double) this.flagCaptures / this.gamesPlayed;
        }
        
        /**
         * Get flag return efficiency (returns per game)
         */
        public double getFlagReturnEfficiency() {
            if (this.gamesPlayed == 0) {
                return 0.0;
            }
            return (double) this.flagReturns / this.gamesPlayed;
        }
        
        /**
         * Get average juggernaut survival time in seconds
         */
        public double getAverageJuggernautSurvivalTime() {
            if (this.juggernautGamesWon + this.juggernautDeaths == 0) {
                return 0.0;
            }
            return (double) this.juggernautSurvivalTime / 1000.0; // Convert to seconds
        }
    }
}