package me.FrogTerra.paintball.utility;

import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.arena.Arena;
import me.FrogTerra.paintball.game.Gamemode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {

    private final Paintball plugin;

    @Getter
    private final Set<UUID> lobbyPlayers = ConcurrentHashMap.newKeySet();
    @Getter
    private int countdown;

    @Getter
    private Gamemode nextGamemode;
    @Getter
    private Arena nextArena;

    private BukkitTask countdownTask;

    public LobbyManager(Paintball plugin) {
        this.plugin = plugin;
    }

    /**
     * Add a player to the lobby
     */
    public boolean addPlayer(Player player) {
        if (lobbyPlayers.contains(player.getUniqueId())) {
            player.sendMessage(MessageUtils.parseMessage("<red>You are already in the lobby!"));
            return false;
        }

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            final boolean success = this.plugin.getWorldManager().teleportToLobby(player);
            if (success) {

            }

            this.lobbyPlayers.add(player.getUniqueId());

        });
        return false;
    }

    /**
     * Remove a player from the lobby
     */
    public boolean removePlayer(Player player) {

        return false;
    }

    /**
     * Set the next game parameters and preload the arena
     */
    public void setNextGame(final Gamemode gamemode, final Arena arena) {
        this.nextGamemode = gamemode;
        this.nextArena = arena;
        
        // Preload the arena to reduce game start lag
        this.plugin.getArenaManager().preloadArena(arena.getName()).thenAccept(success -> {
            if (success) {
                this.plugin.logInfo("Successfully preloaded arena for next game: " + arena.getName());
                
                // Notify players that arena is ready
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    this.lobbyPlayers.forEach(uuid -> {
                        final Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage(MessageUtils.parseMessage(
                                "<green>Next game: <yellow>" + gamemode.getDisplayName() + 
                                " <gray>on <white>" + arena.getName() + " <green>(Ready!)"
                            ));
                        }
                    });
                });
            } else {
                this.plugin.logError("Failed to preload arena: " + arena.getName());
            }
        });
    }

    /**
     * Start the next game with preloaded arena
     */
    public void startNextGame() {
        if (this.nextGamemode == null || this.nextArena == null) {
            this.plugin.logError("Cannot start game - no game parameters set");
            return;
        }

        if (this.lobbyPlayers.size() < this.nextGamemode.getMinPlayers()) {
            this.plugin.logWarning("Not enough players to start game. Need " + 
                this.nextGamemode.getMinPlayers() + ", have " + this.lobbyPlayers.size());
            return;
        }

        final var playerList = new java.util.ArrayList<>(this.lobbyPlayers);
        
        // Start the game (arena should already be preloaded)
        this.plugin.getGameManager().startGame(playerList, this.nextGamemode, this.nextArena)
            .thenAccept(success -> {
                if (success) {
                    this.plugin.logInfo("Successfully started game with preloaded arena");
                    // Clear lobby
                    this.lobbyPlayers.clear();
                    // Reset next game parameters
                    this.nextGamemode = null;
                    this.nextArena = null;
                } else {
                    this.plugin.logError("Failed to start game");
                }
            });
    }

    /**
     * Get a random arena compatible with the gamemode
     */
    private Arena getRandomCompatibleArena(final Gamemode gamemode) {
        final var compatibleArenas = this.plugin.getArenaManager().getArenas().values().stream()
            .filter(arena -> arena.isCompatible(gamemode))
            .toList();
            
        if (compatibleArenas.isEmpty()) {
            return null;
        }
        
        return compatibleArenas.get((int) (Math.random() * compatibleArenas.size()));
    }

    /**
     * Automatically set next game with random parameters
     */
    public void setRandomNextGame() {
        final Gamemode randomGamemode = Gamemode.getRandom();
        final Arena randomArena = this.getRandomCompatibleArena(randomGamemode);
        
        if (randomArena != null) {
            this.setNextGame(randomGamemode, randomArena);
        } else {
            this.plugin.logError("No compatible arenas found for gamemode: " + randomGamemode.getDisplayName());
        }
    }
}
