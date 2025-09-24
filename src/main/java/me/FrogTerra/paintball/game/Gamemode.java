package me.FrogTerra.paintball.game;

import lombok.Getter;

@Getter
public enum Gamemode {
    TEAM_DEATHMATCH("Team Deathmatch", 300, 1, 2, true),
    FREE_FOR_ALL("Free For All", 300, 1, 3, false),
    FLAG_RUSH("Flag Rush", 1200, -1, 6, true),
    JUGGERNAUT("Juggernaut", 300, 3, 5, true);

    private final String displayName;
    private final int duration;     // time in seconds
    private final int lives;        // -1 for unlimited
    private final int minPlayers;
    private final boolean hasTeams; // false for ffa, true for everything else

    Gamemode(final String displayName, final int duration, final int lives, final int minPlayers, final boolean hasTeams) {
        this.displayName = displayName;
        this.duration = duration;
        this.lives = lives;
        this.minPlayers = minPlayers;
        this.hasTeams = hasTeams;
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
}