package me.FrogTerra.paintball.game;

import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.arena.Arena;
import me.FrogTerra.paintball.item.ItemCreator;
import me.FrogTerra.paintball.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the overall game flow and state
 */
public final class GameManager {

    private final Paintball plugin;

    @Getter private Gamemode currentGamemode;
    @Getter private Arena currentArena;
    @Getter private GameState gameState = GameState.WAITING;

    @Getter private final Map<UUID, GameStats> activePlayers = new HashMap<>();
    @Getter private final Map<UUID, GameTeam> playerTeams = new HashMap<>();
    @Getter private final Map<UUID, Integer> playerLives = new HashMap<>();

    private BukkitTask gameTask;
    private int gameTimeRemaining;
    private long gameStartTime;

    public GameManager(Paintball plugin) {
        this.plugin = plugin;
    }

    /**
     * Manages the overall game flow and state
     */
    public boolean startGame(final List<UUID> players, final Gamemode gamemode, final Arena arena) {
        if (this.gameState != GameState.WAITING) return false;

        this.currentGamemode = gamemode;
        this.currentArena = arena;
        this.gameState = GameState.ACTIVE;
        this.gameStartTime = System.currentTimeMillis();

        this.playerTeams.clear();

        switch (currentGamemode) {
            case TEAM_DEATHMATCH, FLAG_RUSH -> {
                for (int i = 0; i < players.size(); i++) {
                    final UUID uuid = players.get(i);
                    final GameTeam team = (i % 2 == 0) ? GameTeam.RED:GameTeam.BLUE;
                    playerTeams.put(uuid, team);
                    playerLives.put(uuid, currentGamemode.getLives());
                    giveEquiptment(uuid, team);
                }
            }
            case FREE_FOR_ALL -> {
                players.forEach(uuid -> {
                    this.playerTeams.put(uuid, GameTeam.FREE);
                    this.playerLives.put(uuid, currentGamemode.getLives());
                    giveEquiptment(uuid, GameTeam.FREE);
                });
            }
            case JUGGERNAUT -> {
                // Select juggernauts (20% of players, minimum 1)
                final int juggernautCount = Math.max(1, (int) (players.size() * 0.2));

                for (int i = 0; i < players.size(); i++) {
                    final UUID playerId = players.get(i);
                    if (i < juggernautCount) {
                        this.playerTeams.put(playerId, GameTeam.JUGGERNAUT);
                        this.playerLives.put(playerId, 1);
                    } else {
                        this.playerTeams.put(playerId, GameTeam.PLAYERS);
                        this.playerLives.put(playerId, 3);
                    }
                }
            }
        }

        // Teleport players to arena respective spawn

        // Start game timer

        // Message players
        return true;
    }

    private void giveEquiptment(UUID uuid, GameTeam team) {
        final PlayerProfile profile = this.plugin.getPlayerManager().getPlayerProfile(uuid);
        if (profile == null) return;

        final Player player = Bukkit.getPlayer(uuid);
        player.getInventory().clear();
        // Paintball gun and ammo
        player.getInventory().setItem(0, plugin.getItemRegistery().getCustomItem("paintball_gun"));

        final int ammoCount = profile.getCurrentPaintballCount();
        ItemStack ammo = plugin.getItemRegistery().getCustomItem("paintball");

        ammo.setAmount(ammoCount);
        player.getInventory().setItem(8, ammo);
        // TODO:: Tracking ammo for reloading
        // TODO:: Kit Handling

        final ItemCreator helmet = new ItemCreator(getTeamHelmetMaterial(team))
                .setDisplayName(team.getColor() + team.getDisplayName() + " helmet")
                .setRarity(ItemRarity.RARE)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        final ItemCreator chestplate = new ItemCreator(createColoredLeatherArmor(Material.LEATHER_CHESTPLATE, getTeamArmorColor(team)))
                .setDisplayName(team.getColor() + team.getDisplayName() + " chestplate")
                .setRarity(ItemRarity.RARE)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        final ItemCreator leggings = new ItemCreator(createColoredLeatherArmor(Material.LEATHER_LEGGINGS, getTeamArmorColor(team)))
                .setDisplayName(team.getColor() + team.getDisplayName() + " leggings")
                .setRarity(ItemRarity.RARE)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        final ItemCreator boots = new ItemCreator(createColoredLeatherArmor(Material.LEATHER_BOOTS, getTeamArmorColor(team)))
                .setDisplayName(team.getColor() + team.getDisplayName() + " boots")
                .setRarity(ItemRarity.RARE)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


        player.getInventory().setHelmet(helmet.getItemStack());
        player.getInventory().setChestplate(chestplate.getItemStack());
        player.getInventory().setLeggings(leggings.getItemStack());
        player.getInventory().setBoots(boots.getItemStack());
    }

    /**
     * Get team armor color
     */
    private Color getTeamArmorColor(GameTeam team) {
        return switch (team) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case FREE -> Color.WHITE;
            case PLAYERS -> Color.GREEN;
            case JUGGERNAUT -> Color.PURPLE;
        };
    }

    /**
     * Create colored leather armor piece
     */
    private static ItemStack createColoredLeatherArmor(final Material material, final Color color) {
        final ItemStack armor = new ItemStack(material);
        final LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        armor.setItemMeta(meta);
        return armor;
    }

    /**
     * Get team helmet material (concrete blocks)
     */
    private static Material getTeamHelmetMaterial(final GameTeam team) {
        return switch (team) {
            case RED -> Material.RED_CONCRETE;
            case BLUE -> Material.BLUE_CONCRETE;
            case FREE -> Material.WHITE_CONCRETE; // Free for All
            case JUGGERNAUT -> Material.PURPLE_CONCRETE;
            case PLAYERS -> Material.GREEN_CONCRETE; // Non-juggernaut players
        };
    }


}
