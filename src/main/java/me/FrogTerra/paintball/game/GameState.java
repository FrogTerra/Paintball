package me.FrogTerra.paintball.game;

/**
 * Enumeration of all possible game states
 */
public enum GameState {
    
    /**
     * Waiting for players to join the lobby
     */
    WAITING,
    
    /**
     * Countdown before game starts
     */
    COUNTDOWN,
    
    /**
     * Game is currently active
     */
    ACTIVE,
    
    /**
     * Game has ended, showing results
     */
    ENDING,
    
    /**
     * Server is restarting or maintenance
     */
    DISABLED
}