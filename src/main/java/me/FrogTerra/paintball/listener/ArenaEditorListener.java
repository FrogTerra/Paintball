package me.FrogTerra.paintball.listener;

import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.arena.ArenaEditor;
import me.FrogTerra.paintball.gui.ArenaEditorGUI;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles arena editor interactions
 */
public class ArenaEditorListener implements Listener {

    private final Paintball plugin;
    private final ArenaEditor arenaEditor;

    public ArenaEditorListener(Paintball plugin) {
        this.plugin = plugin;
        this.arenaEditor = plugin.getArenaManager().getArenaEditor();
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        final String toolType = item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(this.plugin, "editor_tool"), 
            PersistentDataType.STRING
        );

        if (toolType == null) {
            return;
        }

        event.setCancelled(true);

        switch (toolType) {
            case "spawn_placer" -> {
                if (event.getAction().isRightClick() && event.getClickedBlock() != null) {
                    final var location = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);
                    this.arenaEditor.placeSpawnPoint(player, location);
                }
            }
            case "open_gui" -> {
                if (event.getAction().isRightClick()) {
                    final String arenaName = this.arenaEditor.getPlayerEditingArena().get(player.getUniqueId());
                    if (arenaName != null) {
                        final ArenaEditorGUI gui = new ArenaEditorGUI(this.arenaEditor, arenaName);
                        gui.open(player);
                    }
                }
            }
            case "save_exit" -> this.arenaEditor.exitEditorMode(player, true);
            case "exit_no_save" -> this.arenaEditor.exitEditorMode(player, false);
            default -> {
                // Handle spawn mode changes
                if (toolType.startsWith("mode_")) {
                    final String spawnTypeName = toolType.substring(5).toUpperCase();
                    try {
                        final ArenaEditor.SpawnPointType spawnType = ArenaEditor.SpawnPointType.valueOf(spawnTypeName);
                        this.arenaEditor.changeSpawnMode(player, spawnType);
                    } catch (final IllegalArgumentException e) {
                        player.sendMessage("Invalid spawn type: " + spawnTypeName);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArmorStandDamage(final EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // Check if this is a spawn point armor stand
        final String spawnType = armorStand.getPersistentDataContainer().get(
            new NamespacedKey(this.plugin, "spawn_type"), 
            PersistentDataType.STRING
        );

        if (spawnType != null && this.arenaEditor.getEditingPlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);
            this.arenaEditor.removeSpawnPoint(player, armorStand);
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!this.arenaEditor.getEditingPlayers().contains(player.getUniqueId())) {
            return;
        }

        final ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        final String toolType = item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(this.plugin, "editor_tool"), 
            PersistentDataType.STRING
        );

        if (toolType != null) {
            event.setCancelled(true);
            
            switch (toolType) {
                case "open_gui" -> {
                    final String arenaName = this.arenaEditor.getPlayerEditingArena().get(player.getUniqueId());
                    if (arenaName != null) {
                        final ArenaEditorGUI gui = new ArenaEditorGUI(this.arenaEditor, arenaName);
                        gui.open(player);
                    }
                }
                case "save_exit" -> this.arenaEditor.exitEditorMode(player, true);
                case "exit_no_save" -> this.arenaEditor.exitEditorMode(player, false);
                default -> {
                    if (toolType.startsWith("mode_")) {
                        final String spawnTypeName = toolType.substring(5).toUpperCase();
                        try {
                            final ArenaEditor.SpawnPointType spawnType = ArenaEditor.SpawnPointType.valueOf(spawnTypeName);
                            this.arenaEditor.changeSpawnMode(player, spawnType);
                        } catch (final IllegalArgumentException e) {
                            player.sendMessage("Invalid spawn type: " + spawnTypeName);
                        }
                    }
                }
            }
        }
    }
}