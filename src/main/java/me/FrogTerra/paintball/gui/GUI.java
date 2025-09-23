package me.FrogTerra.paintball.gui;

import lombok.Getter;
import me.FrogTerra.paintball.Paintball;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// new TestPagedMenu().open(player);

// Custom GUI Creator that simplifies the creation and use of inventorys.
public abstract class GUI implements Menu, Listener {
    protected static final ItemStack PLACEHOLDER_ITEM;
    @Getter
    private final Paintball plugin = Paintball.getPlugin();

    static {
        PLACEHOLDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = PLACEHOLDER_ITEM.getItemMeta();
        meta.displayName(Component.space());
        PLACEHOLDER_ITEM.setItemMeta(meta);
    }

    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Inventory inventory;
    private boolean usePlaceholders;

    public GUI(Rows rows, Component title) {
        this.usePlaceholders = true;
        this.inventory = Bukkit.createInventory(this, rows.getSize(), title);
    }

    @Override
    public void click(Player player, int slot) {
        Consumer<Player> action = this.actions.get(slot);

        if (action != null) action.accept(player);
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, player -> {
        });
    }

    @Override
    public void setItem(int slot, ItemStack item, Consumer<Player> action) {
        this.actions.put(slot, action);
        this.items.put(slot, item);
        getInventory().setItem(slot, item);
    }

    public void setUsePlaceholders(boolean usePlaceholders) {
        this.usePlaceholders = usePlaceholders;
    }

    @Override
    public void setPlaceholders() {
        for (int i = 0; i < getInventory().getSize(); i++) {
            if (getInventory().getItem(i) == null) getInventory().setItem(i, PLACEHOLDER_ITEM);
        }
    }

    @Override
    public boolean usePlaceholders() {
        return usePlaceholders;
    }

    @Override
    public void update() {
        getInventory().clear();
        for (int i = 0; i < getInventory().getSize(); i++) {
            ItemStack item = getItemsMap().get(i);
            if (item != null) getInventory().setItem(i, item);
        }
    }

    @Override
    public abstract void onSetItems();

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public Map<Integer, ItemStack> getItemsMap() {
        return items;
    }

    @Override
    public Map<Integer, Consumer<Player>> getActionsMap() {
        return actions;
    }

    public enum Rows {
        ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5);

        private final int size;

        Rows(int rows) {
            this.size = rows * 9;
        }

        public int getSize() {
            return size;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        final Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory==null) return;
        if (!(clickedInventory.getHolder() instanceof final Menu menu))  return; // Non-custom gui clicked

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getType() == Material.BARRIER) { // Universal close inventory item
            event.getWhoClicked().closeInventory();
            return;
        }
        menu.click((Player) event.getWhoClicked(), event.getSlot());
    }
}

interface Menu extends InventoryHolder {
    void click(Player player, int slot);

    void setItem(int slot, ItemStack item);

    void setItem(int slot, ItemStack item, Consumer<Player> action);

    void onSetItems();

    boolean usePlaceholders();

    void setPlaceholders();

    void update();

    Map<Integer, ItemStack> getItemsMap();

    Map<Integer, Consumer<Player>> getActionsMap();

    default void open(Player player) {
        if (usePlaceholders()) setPlaceholders();

        onSetItems();
        player.openInventory(getInventory());
    }
}

