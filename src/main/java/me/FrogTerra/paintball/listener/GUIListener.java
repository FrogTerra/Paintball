package me.FrogTerra.paintball.listener;

import me.FrogTerra.paintball.gui.GUI;
import me.FrogTerra.paintball.gui.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Handles all GUI-related events
 */
public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        final Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;
        
        // Check if the clicked inventory belongs to a custom GUI
        if (!(clickedInventory.getHolder() instanceof Menu menu)) return;

        // Cancel the event to prevent item movement
        event.setCancelled(true);
        
        // Check if clicked item exists
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        // Handle universal close inventory item (barrier)
        if (event.getCurrentItem().getType() == Material.BARRIER) {
            event.getWhoClicked().closeInventory();
            return;
        }
        
        // Call the menu's click handler
        menu.click((Player) event.getWhoClicked(), event.getSlot());
    }
}