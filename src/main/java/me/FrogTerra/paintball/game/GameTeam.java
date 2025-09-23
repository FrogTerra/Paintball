package me.FrogTerra.paintball.game;

import lombok.Getter;

/**
 * Enumeration of all possible game teams
 */
@Getter
public enum GameTeam {
    
    RED("Red", "<red>"),
    BLUE("Blue", "<blue>"),
    JUGGERNAUT("Juggernaut", "<dark_purple>"),
    PLAYERS("Players", "<green>"),
    FREE("Free", "<white>");
    
    private final String displayName;
    private final String color;
    
    GameTeam(final String displayName, final String color) {
        this.displayName = displayName;
        this.color = color;
    }
}