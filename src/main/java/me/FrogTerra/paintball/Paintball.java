package me.FrogTerra.paintball;

import lombok.Getter;
import me.FrogTerra.paintball.arena.ArenaManager;
import me.FrogTerra.paintball.command.ArenaCommand;
import me.FrogTerra.paintball.game.GameManager;
import me.FrogTerra.paintball.item.ItemRegistery;
import me.FrogTerra.paintball.listener.ArenaEditorListener;
import me.FrogTerra.paintball.listener.GUIListener;
import me.FrogTerra.paintball.listener.PlayerListener;
import me.FrogTerra.paintball.player.PlayerManager;
import me.FrogTerra.paintball.utility.LevelManager;
import me.FrogTerra.paintball.utility.LobbyManager;
import me.FrogTerra.paintball.utility.MessageUtils;
import me.FrogTerra.paintball.utility.WorldManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Paintball extends JavaPlugin {

    @Getter private static Paintball plugin;

    @Getter private ItemRegistery itemRegistery;
    @Getter private WorldManager worldManager;
    @Getter private PlayerManager playerManager;
    @Getter private ArenaManager arenaManager;
    @Getter private GameManager gameManager;

    @Getter
    private LobbyManager lobbyManager;
    @Getter
    private LevelManager levelManager;


    @Getter
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        plugin = this;
        try {
            // Plugin startup logic
            initializeLuckPerms();
            itemRegistery = new ItemRegistery(this);
            worldManager = new WorldManager(this);
            playerManager = new PlayerManager(this);
            arenaManager = new ArenaManager(this);
            gameManager = new GameManager(this);
            lobbyManager = new LobbyManager(this);
            levelManager = new LevelManager(this);

            this.getLogger().info("Core systems initialised!");

            registerCommands();
            this.getLogger().info("Commands Registered!");

            PluginManager pm = Bukkit.getServer().getPluginManager();
            pm.registerEvents(new PlayerListener(this), this);
            pm.registerEvents(new ArenaEditorListener(this), this);
            pm.registerEvents(new GUIListener(), this);
            this.getLogger().info("Listeners Registered!");

        } catch (Exception exception) {
            this.getLogger().log(Level.SEVERE, "Failed to initialize Paintball Plugin", exception);
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        getWorldManager().cleanup();
    }

    private void registerCommands() {
        this.getCommand("arena").setExecutor(new ArenaCommand(this));
    }

    private void initializeLuckPerms() {
        // Initialize LuckPerms API
        RegisteredServiceProvider<LuckPerms> provider = this.getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            this.getLogger().info("Successfully connected to LuckPerms API");
        } else {
            this.getLogger().warning("LuckPerms not found! Some features may not work correctly.");
        }
    }

    /**
     * Send a formatted message to console
     *
     * @param message the message to send
     */
    public void logInfo(String message) {
        this.getLogger().info(MessageUtils.stripColors(message));
    }

    /**
     * Send a formatted warning to console
     *
     * @param message the warning message to send
     */
    public void logWarning(String message) {
        this.getLogger().warning(MessageUtils.stripColors(message));
    }

    /**
     * Send a formatted error to console
     *
     * @param message the error message to send
     */
    public void logError(String message) {
        this.getLogger().severe(MessageUtils.stripColors(message));
    }

    /**
     * Send a formatted error to console with exception
     *
     * @param message   the error message to send
     * @param exception the exception that occurred
     */
    public void logError(String message, Throwable exception) {
        this.getLogger().log(Level.SEVERE, MessageUtils.stripColors(message), exception);
    }
}
