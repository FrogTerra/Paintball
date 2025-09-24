package me.FrogTerra.paintball.game;

import lombok.Getter;

@Getter
public enum Gamemode {
    TEAM_DEATHMATCH("Team Deathmatch", 300, 1, 4, true, "Last team standing wins"),
    FREE_FOR_ALL("Free For All", 300, 1, 6, false, "Last player standing wins"),
    FLAG_RUSH("Flag Rush", 600, -1, 6, true, "Capture the enemy flag and return it to your base"),
    JUGGERNAUT("Juggernaut", 300, 3, 8, true, "Players must eliminate the juggernauts to win");

    private final String displayName;
    private final int duration;     // time in seconds
    private final int lives;        // -1 for unlimited
    private final int minPlayers;
    private final boolean hasTeams; // false for ffa, true for everything else
    private final String description;

    Gamemode(final String displayName, final int duration, final int lives, final int minPlayers, final boolean hasTeams, final String description) {
        this.displayName = displayName;
        this.duration = duration;
        this.lives = lives;
        this.minPlayers = minPlayers;
        this.hasTeams = hasTeams;
        this.description = description;
    }

    /**
     * Get a random game mode
     */
    public static Gamemode getRandom() {
        final Gamemode[] values = values();
        return values[(int) (Math.random() * values.length)];
    }

    /**
     * Get game mode by name (case-insensitive)
     */
    public static Gamemode fromString(final String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        for (final Gamemode gameMode : values()) {
            if (gameMode.name().equalsIgnoreCase(name) ||
                    gameMode.displayName.equalsIgnoreCase(name)) {
                return gameMode;
            }
        }
        return null;
    }

    /**
     * Check if game mode has unlimited lives
     */
    public boolean hasUnlimitedLives() {
        return this.lives == -1;
    }

    /**
     * Get respawn time for this game mode
     */
    public int getRespawnTime() {
        return switch (this) {
            case FLAG_RUSH -> 5;
            case JUGGERNAUT -> 10;
            default -> 0; // No respawn for single life modes
        };
    }

    /**
     * Check if gamemode requires flag spawns
     */
    public boolean requiresFlagSpawns() {
        return this == FLAG_RUSH;
    }

    /**
     * Get juggernaut percentage for this gamemode
     */
    public double getJuggernautPercentage() {
        return this == JUGGERNAUT ? 0.05 : 0.0; // 5% of players become juggernauts
    }
}