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

    // FFA Specific Stats
    private int flagCaptures = 0;
    private int flagReturns = 0;

    private GameStats(UUID uuid) {
        this.uuid = uuid;
    }
}
