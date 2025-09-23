package me.FrogTerra.paintball.listener;

import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.utility.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Paintball plugin;

    public PlayerListener(Paintball plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // Load player profile asynchronously
        this.plugin.getPlayerManager().loadPlayerProfile(player).thenAccept(profile -> {
            if (profile != null) {
                this.plugin.logInfo("Loaded profile for " + player.getName() +
                        " (Level: " + profile.getLevel() + ", Coins: " + profile.getCoins() + ")");

                // Show custom join message after profile is loaded
                Bukkit.getScheduler().runTask(this.plugin, () -> {

                    // Format { + %staff% %squad% %player% - Joined to Game }
                    final String JoinMsg = "  <gold>+</gold> " + MessageUtils.formatPlayer(profile, MessageUtils.Formats.CONNECTION) + " Joined the game!";
                    final String finalJoinMsg = profile.hasPrestige() ? "<bold>" + JoinMsg + "/<bold>":JoinMsg;
                    event.joinMessage(MessageUtils.parseMessage(finalJoinMsg));

                    MessageUtils.playerTabListFormat(profile);

                    // Create scoreboard
                    // this.plugin.getScoreboardManager().createScoreboard(player);

                    // Auto-join paintball lobby
                    // this.plugin.getLobbyManager().addPlayer(player);
                });
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
    }

}
