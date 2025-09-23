package me.FrogTerra.paintball.utility;

import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.player.PlayerProfile;
import org.bukkit.entity.Player;

public class LevelManager {

    private final Paintball plugin;

    public LevelManager(Paintball plugin) {
        this.plugin = plugin;
    }

    /**
     * Add experience to a player, automatical levelup and prestige check.
     * @param player The player to receive the experience.
     * @param experience Amount of experience given to the player.
     */
    public void addExperience(final Player player, final long experience) {
        final PlayerProfile profile = this.plugin.getPlayerManager().getPlayerProfile(player.getUniqueId());
        if (profile == null) return;

        final int currentLevel = profile.getLevel();
        final boolean leveledUp = profile.addExperience(experience);

        player.sendMessage(MessageUtils.parseMessage("<aqua>+" + experience));
        if (profile.addExperience(experience)) { // Player has leveled up
            final int newLevel = profile.getLevel();
            player.sendMessage(MessageUtils.parseMessage("<white>You have levelup to:" + MessageUtils.getLevelColor(newLevel) + MessageUtils.convertToRoman(newLevel) + "<white>!"));
            // Check for unlocks
            this.plugin.getPlayerManager().savePlayerProfile(profile);
        }

        if (profile.isPrestigeReminder()) {
            player.sendMessage(MessageUtils.parseMessage("<white>You can now prestige by using <green>/prestige"));
            profile.setPrestigeReminder(true);
        }
    }

    /**
     * Attempt to prestige a player
     * @param player The player to prestige.
     * Returns is the player has prestiged or not
     */
    public boolean prestige(final Player player) {
        final PlayerProfile profile = this.plugin.getPlayerManager().getPlayerProfile(player.getUniqueId());
        if (profile == null) return false;

        if (profile.prestige()) {
            final int prestige = profile.getPrestige();
            player.sendMessage(MessageUtils.parseMessage("<white>You have prestiged to: " + MessageUtils.getPrestigeColor(prestige) + prestige + "<white>!"));
            this.plugin.getPlayerManager().savePlayerProfile(profile);
            return true;
        } else {
            player.sendMessage(MessageUtils.parseMessage("<red>You cannot prestige yet! Reach level 1000 first."));
            return false;
        }
    }
}
