package me.FrogTerra.paintball.utility;

import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
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

}
