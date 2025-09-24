package me.FrogTerra.paintball.game;

import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.arena.Arena;
import me.FrogTerra.paintball.arena.ArenaEditor;
import me.FrogTerra.paintball.item.ItemCreator;
import me.FrogTerra.paintball.player.PlayerProfile;
import me.FrogTerra.paintball.utility.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    private boolean gameLoadedSuccessfully = false;
    private BukkitTask gameTask;
    private int gameTimeRemaining;
    private long gameStartTime;

    public GameManager(Paintball plugin) {
        this.plugin = plugin;
    }

    /**
     * Start a game with the given parameters
     */
    public CompletableFuture<Boolean> startGame(final List<UUID> players, final Gamemode gamemode, final Arena arena) {
        return CompletableFuture.supplyAsync(() -> {
            if (this.gameState != GameState.WAITING) return false;

            this.currentGamemode = gamemode;
            this.currentArena = arena;
            this.gameState = GameState.ACTIVE;
            this.gameStartTime = System.currentTimeMillis();
            this.gameLoadedSuccessfully = false;

            this.playerTeams.clear();
            this.playerLives.clear();
            this.activePlayers.clear();

            // Initialize player stats
            players.forEach(uuid -> this.activePlayers.put(uuid, new GameStats(uuid)));

            // Setup teams and equipment
            this.setupTeamsAndEquipment(players);

            // Check if arena is already preloaded
            if (this.plugin.getArenaManager().isArenaPreloaded(arena.getName())) {
                this.plugin.logInfo("Using preloaded arena: " + arena.getName());
                
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    this.teleportPlayersToSpawns(players);
                    this.startGameTimer();
                    this.messagePlayersGameStart();
                    
                    // Remove spawn armor stands after teleporting players
                    this.plugin.getArenaManager().getArenaEditor().removeSpawnArmorStands(
                        this.plugin.getWorldManager().getArenaWorld()
                    );
                    
                    // Mark game as successfully loaded
                    this.gameLoadedSuccessfully = true;
                });
                
                return true;
            } else {
                // Load arena if not preloaded
                this.plugin.logInfo("Loading arena for game: " + arena.getName());
                this.plugin.getArenaManager().loadArena(arena.getName()).thenAccept(success -> {
                    if (success) {
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            this.teleportPlayersToSpawns(players);
                            this.startGameTimer();
                            this.messagePlayersGameStart();
                            
                            // Remove spawn armor stands after teleporting players
                            this.plugin.getArenaManager().getArenaEditor().removeSpawnArmorStands(
                                this.plugin.getWorldManager().getArenaWorld()
                            );
                            
                            // Mark game as successfully loaded
                            this.gameLoadedSuccessfully = true;
                        });
                    } else {
                        this.plugin.logError("Failed to load arena for game: " + arena.getName());
                        // Game failed to load - do not save stats
                        this.endGame();
                    }
                });
                
                return true;
            }
        });
    }

    /**
     * Setup teams and equipment for all players
     */
    private void setupTeamsAndEquipment(final List<UUID> players) {
        switch (currentGamemode) {
            case TEAM_DEATHMATCH, FLAG_RUSH -> {
                for (int i = 0; i < players.size(); i++) {
                    final UUID uuid = players.get(i);
                    final GameTeam team = (i % 2 == 0) ? GameTeam.RED : GameTeam.BLUE;
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
                        giveEquiptment(playerId, GameTeam.JUGGERNAUT);
                    } else {
                        this.playerTeams.put(playerId, GameTeam.PLAYERS);
                        this.playerLives.put(playerId, 3);
                        giveEquiptment(playerId, GameTeam.PLAYERS);
                    }
                }
            }
        }

    }

    /**
     * Teleport players to their respective spawn points
     */
    private void teleportPlayersToSpawns(final List<UUID> players) {
        // Scan armor stands in the arena world for spawn points
        final Map<ArenaEditor.SpawnPointType, List<Location>> spawnPoints = 
            this.plugin.getArenaManager().getArenaEditor().scanArmorStandsForSpawns(
                this.plugin.getWorldManager().getArenaWorld()
            );
        
        this.plugin.logInfo("Found spawn points for game: " + spawnPoints.size() + " types");

        for (final UUID playerId : players) {
            final Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;

            final GameTeam team = this.playerTeams.get(playerId);
            final List<Location> teamSpawns = this.getSpawnPointsForTeam(team, spawnPoints);

            if (!teamSpawns.isEmpty()) {
                final Location spawn = teamSpawns.get((int) (Math.random() * teamSpawns.size()));
                player.teleport(spawn);
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                this.plugin.logInfo("Teleported " + player.getName() + " to " + team + " spawn at " + 
                    String.format("%.1f, %.1f, %.1f", spawn.getX(), spawn.getY(), spawn.getZ()));
            } else {
                this.plugin.logWarning("No spawn points found for team " + team + " in arena " + this.currentArena.getName());
                // Fallback to arena center
                final Location fallback = new Location(this.plugin.getWorldManager().getArenaWorld(), 0, 100, 0);
                player.teleport(fallback);
                this.plugin.logWarning("Used fallback spawn for " + player.getName());
            }
        }
    }

    /**
     * Get spawn points for a specific team
     */
    private List<Location> getSpawnPointsForTeam(final GameTeam team, final Map<ArenaEditor.SpawnPointType, List<Location>> spawnPoints) {
        return switch (team) {
            case RED -> spawnPoints.getOrDefault(ArenaEditor.SpawnPointType.RED_SPAWN, new ArrayList<>());
            case BLUE -> spawnPoints.getOrDefault(ArenaEditor.SpawnPointType.BLUE_SPAWN, new ArrayList<>());
            case FREE -> spawnPoints.getOrDefault(ArenaEditor.SpawnPointType.FREE_FOR_ALL_SPAWN, new ArrayList<>());
            case JUGGERNAUT -> spawnPoints.getOrDefault(ArenaEditor.SpawnPointType.RED_SPAWN, new ArrayList<>());
            case PLAYERS -> spawnPoints.getOrDefault(ArenaEditor.SpawnPointType.BLUE_SPAWN, new ArrayList<>());
        };
    }

    /**
     * Start the game timer
     */
    private void startGameTimer() {
        this.gameTimeRemaining = this.currentGamemode.getDuration();
        
        this.gameTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            this.gameTimeRemaining--;
            
            if (this.gameTimeRemaining <= 0) {
                this.endGame();
            } else if (this.gameTimeRemaining % 60 == 0 || this.gameTimeRemaining <= 10) {
                // Broadcast time remaining
                final String timeMsg = "<yellow>Time remaining: <white>" + this.gameTimeRemaining + "s";
                this.activePlayers.keySet().forEach(uuid -> {
                    final Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(MessageUtils.parseMessage(timeMsg));
                    }
                });
            }
        }, 20L, 20L); // Run every second
    }

    /**
     * Message players that the game has started
     */
    private void messagePlayersGameStart() {
        final String startMsg = "<green><bold>Game Started! <yellow>" + this.currentGamemode.getDisplayName() + 
                               " <gray>on <white>" + this.currentArena.getName();
        
        this.activePlayers.keySet().forEach(uuid -> {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(MessageUtils.parseMessage(startMsg));
                player.sendTitle(
                    MessageUtils.stripColors(MessageUtils.parseMessage("<green><bold>GAME START!")),
                    MessageUtils.stripColors(MessageUtils.parseMessage("<yellow>" + this.currentGamemode.getDisplayName())),
                    10, 40, 10
                );
            }
        });
    }

    /**
     * End the current game
     */
    public void endGame() {
        if (this.gameState != GameState.ACTIVE) return;

        this.gameState = GameState.ENDING;

        // Cancel game timer
        if (this.gameTask != null) {
            this.gameTask.cancel();
            this.gameTask = null;
        }

        // Save game statistics if game loaded successfully
        if (this.gameLoadedSuccessfully) {
            this.saveGameStatistics();
        } else {
            this.plugin.logWarning("Game did not load successfully - skipping stat saving");
        }

        // Show game results and teleport players back to lobby
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.activePlayers.keySet().forEach(uuid -> {
                final Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    this.plugin.getWorldManager().teleportToLobby(player);
                    player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                    player.getInventory().clear();
                }
            });

            // Reset game state
            this.gameState = GameState.WAITING;
            this.currentGamemode = null;
            this.currentArena = null;
            this.gameLoadedSuccessfully = false;
            this.activePlayers.clear();
            this.playerTeams.clear();
            this.playerLives.clear();

        }, 100L); // 5 second delay
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
     * Save game statistics to player profiles
     */
    private void saveGameStatistics() {
        if (this.currentGamemode == null) {
            this.plugin.logWarning("Cannot save stats - no current gamemode");
            return;
        }

        final long gameEndTime = System.currentTimeMillis();
        final long gameDuration = gameEndTime - this.gameStartTime;
        
        this.plugin.logInfo("Saving game statistics for " + this.activePlayers.size() + " players");
        
        // Determine winners based on gamemode
        final List<UUID> winners = this.determineWinners();
        
        // Save stats for each player
        this.activePlayers.forEach((uuid, gameStats) -> {
            final PlayerProfile profile = this.plugin.getPlayerManager().getPlayerProfile(uuid);
            if (profile == null) {
                this.plugin.logWarning("Could not find profile for player: " + uuid);
                return;
            }
            
            final boolean isWinner = winners.contains(uuid);
            final Player player = Bukkit.getPlayer(uuid);
            final String playerName = player != null ? player.getName() : "Unknown";
            
            // Update general statistics
            profile.setTotalKills(profile.getTotalKills() + gameStats.getKills());
            profile.setTotalDeaths(profile.getTotalDeaths() + gameStats.getDeaths());
            profile.setTotalShots(profile.getTotalShots() + gameStats.getShots());
            profile.setTotalGamesPlayed(profile.getTotalGamesPlayed() + 1);
            profile.addPlayTime(gameDuration);
            
            if (isWinner) {
                profile.setTotalWins(profile.getTotalWins() + 1);
            } else {
                profile.setTotalLosses(profile.getTotalLosses() + 1);
            }
            
            // Update gamemode-specific statistics
            final PlayerProfile.GameModeStats gameModeStats = profile.getGameModeStats(this.currentGamemode);
            gameModeStats.setKills(gameModeStats.getKills() + gameStats.getKills());
            gameModeStats.setDeaths(gameModeStats.getDeaths() + gameStats.getDeaths());
            gameModeStats.setShots(gameModeStats.getShots() + gameStats.getShots());
            gameModeStats.setGamesPlayed(gameModeStats.getGamesPlayed() + 1);
            gameModeStats.setTotalPlayTime(gameModeStats.getTotalPlayTime() + gameDuration);
            
            if (isWinner) {
                gameModeStats.setWins(gameModeStats.getWins() + 1);
            } else {
                gameModeStats.setLosses(gameModeStats.getLosses() + 1);
            }
            
            // Update gamemode-specific stats
            if (this.currentGamemode == Gamemode.FLAG_RUSH) {
                // Flag Rush specific stats
                gameModeStats.setFlagCaptures(gameModeStats.getFlagCaptures() + gameStats.getFlagCaptures());
                gameModeStats.setFlagReturns(gameModeStats.getFlagReturns() + gameStats.getFlagReturns());
                gameModeStats.setFlagKills(gameModeStats.getFlagKills() + gameStats.getFlagKills());
                gameModeStats.setFlagCarrierKills(gameModeStats.getFlagCarrierKills() + gameStats.getFlagCarrierKills());
            } else if (this.currentGamemode == Gamemode.JUGGERNAUT) {
                // Juggernaut specific stats
                final GameTeam playerTeam = this.playerTeams.get(uuid);
                if (playerTeam == GameTeam.JUGGERNAUT) {
                    gameModeStats.setJuggernautKills(gameModeStats.getJuggernautKills() + gameStats.getJuggernautKills());
                    gameModeStats.setJuggernautDeaths(gameModeStats.getJuggernautDeaths() + gameStats.getJuggernautDeaths());
                    gameModeStats.setJuggernautSurvivalTime(gameModeStats.getJuggernautSurvivalTime() + gameStats.getJuggernautSurvivalTime());
                    if (isWinner) {
                        gameModeStats.setJuggernautGamesWon(gameModeStats.getJuggernautGamesWon() + 1);
                    }
                } else if (playerTeam == GameTeam.PLAYERS) {
                    gameModeStats.setPlayerKills(gameModeStats.getPlayerKills() + gameStats.getPlayerKills());
                    gameModeStats.setPlayerDeaths(gameModeStats.getPlayerDeaths() + gameStats.getPlayerDeaths());
                    gameModeStats.setJuggernautKillsAgainst(gameModeStats.getJuggernautKillsAgainst() + gameStats.getJuggernautKillsAgainst());
                    if (isWinner) {
                        gameModeStats.setPlayerGamesWon(gameModeStats.getPlayerGamesWon() + 1);
                    }
                }
            }
            
            // Award experience based on performance
            long experienceGained = this.calculateExperience(gameStats, isWinner);
            profile.addExperience(experienceGained);
            
            // Award coins based on performance
            long coinsGained = this.calculateCoins(gameStats, isWinner);
            profile.addCoins(coinsGained);
            
            // Save the profile asynchronously
            this.plugin.getPlayerManager().savePlayerProfile(profile);
            
            // Notify player of their performance
            if (player != null) {
                this.sendGameSummary(player, gameStats, isWinner, experienceGained, coinsGained);
            }
            
            this.plugin.logInfo("Saved stats for " + playerName + " - K:" + gameStats.getKills() + 
                               " D:" + gameStats.getDeaths() + " Winner:" + isWinner);
        });
    }
    
    /**
     * Determine winners based on gamemode
     */
    private List<UUID> determineWinners() {
        final List<UUID> winners = new ArrayList<>();
        
        switch (this.currentGamemode) {
            case TEAM_DEATHMATCH -> {
                // Team with most kills wins
                final Map<GameTeam, Integer> teamKills = new HashMap<>();
                
                this.activePlayers.forEach((uuid, stats) -> {
                    final GameTeam team = this.playerTeams.get(uuid);
                    if (team != null) {
                        teamKills.merge(team, stats.getKills(), Integer::sum);
                    }
                });
                
                final GameTeam winningTeam = teamKills.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                
                if (winningTeam != null) {
                    this.playerTeams.entrySet().stream()
                        .filter(entry -> entry.getValue() == winningTeam)
                        .map(Map.Entry::getKey)
                        .forEach(winners::add);
                }
            }
            case FREE_FOR_ALL -> {
                // Player with most kills wins
                this.activePlayers.entrySet().stream()
                    .max(Map.Entry.comparingByValue(Comparator.comparing(GameStats::getKills)))
                    .map(Map.Entry::getKey)
                    .ifPresent(winners::add);
            }
            case JUGGERNAUT -> {
                // Juggernauts win if they survive, players win if they eliminate all juggernauts
                final boolean juggernauts Survive = this.playerTeams.entrySet().stream()
                    .anyMatch(entry -> entry.getValue() == GameTeam.JUGGERNAUT && 
                             this.playerLives.getOrDefault(entry.getKey(), 0) > 0);
                
                if (juggernauts Survive) {
                    // Juggernauts win
                    this.playerTeams.entrySet().stream()
                        .filter(entry -> entry.getValue() == GameTeam.JUGGERNAUT)
                        .map(Map.Entry::getKey)
                        .forEach(winners::add);
                } else {
                    // Players win
                    this.playerTeams.entrySet().stream()
                        .filter(entry -> entry.getValue() == GameTeam.PLAYERS)
                        .map(Map.Entry::getKey)
                        .forEach(winners::add);
                }
            }
            case FLAG_RUSH -> {
                // Team that captured the most flags wins (placeholder logic)
                // This would need to be implemented with actual flag capture tracking
                final Map<GameTeam, Integer> teamCaptures = new HashMap<>();
                
                this.activePlayers.forEach((uuid, stats) -> {
                    final GameTeam team = this.playerTeams.get(uuid);
                    if (team != null) {
                        teamCaptures.merge(team, stats.getFlagCaptures(), Integer::sum);
                    }
                });
                
                final GameTeam winningTeam = teamCaptures.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                
                if (winningTeam != null) {
                    this.playerTeams.entrySet().stream()
                        .filter(entry -> entry.getValue() == winningTeam)
                        .map(Map.Entry::getKey)
                        .forEach(winners::add);
                }
            }
        }
        
        return winners;
    }
    
    /**
     * Calculate experience gained based on performance
     */
    private long calculateExperience(final GameStats stats, final boolean isWinner) {
        long experience = 0;
        
        // Base experience for participation
        experience += 50;
        
        // Experience for kills
        experience += stats.getKills() * 25;
        
        // Experience for flag actions (Flag Rush)
        experience += stats.getFlagCaptures() * 100;
        experience += stats.getFlagReturns() * 50;
        
        // Bonus for winning
        if (isWinner) {
            experience += 100;
        }
        
        // Bonus for good K/D ratio
        if (stats.getDeaths() > 0) {
            final double kd = (double) stats.getKills() / stats.getDeaths();
            if (kd >= 2.0) {
                experience += 50;
            } else if (kd >= 1.5) {
                experience += 25;
            }
        } else if (stats.getKills() > 0) {
            experience += 75; // No deaths bonus
        }
        
        return experience;
    }
    
    /**
     * Calculate coins gained based on performance
     */
    private long calculateCoins(final GameStats stats, final boolean isWinner) {
        long coins = 0;
        
        // Base coins for participation
        coins += 25;
        
        // Coins for kills
        coins += stats.getKills() * 10;
        
        // Coins for flag actions
        coins += stats.getFlagCaptures() * 50;
        coins += stats.getFlagReturns() * 25;
        
        // Bonus for winning
        if (isWinner) {
            coins += 50;
        }
        
        return coins;
    }
    
    /**
     * Send game summary to player
     */
    private void sendGameSummary(final Player player, final GameStats stats, final boolean isWinner, 
                                final long experienceGained, final long coinsGained) {
        player.sendMessage(MessageUtils.parseMessage("<green><bold>===== GAME SUMMARY ====="));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Result: " + (isWinner ? "<green>VICTORY!" : "<red>DEFEAT")));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Gamemode: <white>" + this.currentGamemode.getDisplayName()));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Arena: <white>" + this.currentArena.getName()));
        player.sendMessage(MessageUtils.parseMessage(""));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Performance:"));
        player.sendMessage(MessageUtils.parseMessage("  <green>Kills: <white>" + stats.getKills()));
        player.sendMessage(MessageUtils.parseMessage("  <red>Deaths: <white>" + stats.getDeaths()));
        player.sendMessage(MessageUtils.parseMessage("  <blue>Shots: <white>" + stats.getShots()));
        
        if (this.currentGamemode == Gamemode.FLAG_RUSH) {
            player.sendMessage(MessageUtils.parseMessage("  <gold>Flag Captures: <white>" + stats.getFlagCaptures()));
            player.sendMessage(MessageUtils.parseMessage("  <aqua>Flag Returns: <white>" + stats.getFlagReturns()));
            if (stats.getFlagKills() > 0) {
                player.sendMessage(MessageUtils.parseMessage("  <yellow>Flag Kills: <white>" + stats.getFlagKills()));
            }
            if (stats.getFlagCarrierKills() > 0) {
                player.sendMessage(MessageUtils.parseMessage("  <orange>Flag Carrier Kills: <white>" + stats.getFlagCarrierKills()));
            }
        } else if (this.currentGamemode == Gamemode.JUGGERNAUT) {
            final GameTeam playerTeam = this.playerTeams.get(player.getUniqueId());
            if (playerTeam == GameTeam.JUGGERNAUT) {
                player.sendMessage(MessageUtils.parseMessage("  <dark_purple>Juggernaut Kills: <white>" + stats.getJuggernautKills()));
                if (stats.getJuggernautSurvivalTime() > 0) {
                    long survivalSeconds = stats.getJuggernautSurvivalTime() / 1000;
                    player.sendMessage(MessageUtils.parseMessage("  <dark_purple>Survival Time: <white>" + survivalSeconds + "s"));
                }
            } else if (playerTeam == GameTeam.PLAYERS) {
                if (stats.getJuggernautKillsAgainst() > 0) {
                    player.sendMessage(MessageUtils.parseMessage("  <green>Juggernaut Kills: <white>" + stats.getJuggernautKillsAgainst()));
                }
            }
        }
        
        final double kd = stats.getDeaths() > 0 ? (double) stats.getKills() / stats.getDeaths() : stats.getKills();
        player.sendMessage(MessageUtils.parseMessage("  <purple>K/D Ratio: <white>" + String.format("%.2f", kd)));
        player.sendMessage(MessageUtils.parseMessage(""));
        player.sendMessage(MessageUtils.parseMessage("<yellow>Rewards:"));
        player.sendMessage(MessageUtils.parseMessage("  <aqua>Experience: <white>+" + experienceGained));
        player.sendMessage(MessageUtils.parseMessage("  <gold>Coins: <white>+" + coinsGained));
        player.sendMessage(MessageUtils.parseMessage("<green><bold>========================"));
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
