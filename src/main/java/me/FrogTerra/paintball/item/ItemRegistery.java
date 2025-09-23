package me.FrogTerra.paintball.item;

import lombok.NonNull;
import me.FrogTerra.paintball.Paintball;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.unmodifiableSet;

public final class ItemRegistery {

    private final Paintball plugin;
    private final ConcurrentMap<String, ItemStack> registeredItems;

    public ItemRegistery(Paintball plugin) {
        this.plugin = plugin;
        this.registeredItems = new ConcurrentHashMap<>();
        this.registerDefaultItems();
    }

    /**
     * Registers a custom item with a unique identifier.
     * @param identifier The unique identifier for the item
     * @param itemStack The ItemStack to register
     */
    public void registerItem(@NonNull final String identifier, @NonNull final ItemStack itemStack) {
        this.registeredItems.put(identifier.toLowerCase(), itemStack.clone());
        this.plugin.getLogger().info("Registered custom item: " + identifier);
    }

    /**
     * Gets a registered custom item by its identifier.
     * @param identifier The unique identifier
     * @return The custom ItemStack or null if not found
     */
    public ItemStack getCustomItem(@NonNull final String identifier) {
        final ItemStack item = this.registeredItems.get(identifier.toLowerCase());
        return item != null ? item.clone() : null;
    }

    /**
     * Checks if a custom item is registered.
     * @param identifier The unique identifier
     * @return True if the item is registered, false otherwise
     */
    public boolean isRegistered(@NonNull final String identifier) {
        return this.registeredItems.containsKey(identifier.toLowerCase());
    }

    /**
     * Unregisters a custom item.
     * @param identifier The unique identifier
     * @return True if the item was removed, false if it wasn't registered
     */
    public boolean unregisterItem(@NonNull final String identifier) {
        return this.registeredItems.remove(identifier.toLowerCase()) != null;
    }

    /**
     * Gets all registered item identifiers.
     * @return A set of all registered identifiers
     */
    public java.util.Set<String> getRegisteredItems() {
        return unmodifiableSet(this.registeredItems.keySet());
    }

    /**
     * Registers default custom items.
     */
    private void registerDefaultItems() {
        // Paintball Gun - Custom weapon with texture
        final ItemStack paintballGun = new ItemCreator(Material.IRON_HOE)
                .setDisplayName("<gradient:#ff4757:#2ed573><bold>Paintball Gun</bold></gradient>")
                .setLore(
                        "<gray>A high-tech paintball marker for competitive play",
                        "<gray>",
                        "<yellow>✦ <green>Shoots paintballs at high velocity",
                        "<yellow>✦ <blue>Requires paintball ammunition",
                        "<yellow>✦ <purple>Right-click to shoot",
                        "<gray>",
                        "<italic><aqua>\"Splat your enemies with style!\""
                )
                .setUnbreakable(true)
                .setFireResistant(true)
                .setRarity(ItemRarity.RARE)
                .setPersistentData("cooldown_ticks", 10)
                .setPersistentData("cooldown_group", "paintball_weapons")
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setPersistentData("custom_weapon", "paintball_gun")
                .setPersistentData("weapon_type", "ranged")
                .setPersistentData("ammo_type", "paintball")
                .setPersistentData("damage", 2)
                .setPersistentData("range", 90)
                .build();

        this.registerItem("paintball_gun", paintballGun);

        // Paintball - Ammunition for the paintball gun
        final ItemStack paintball = new ItemCreator(Material.SNOWBALL)
                .setDisplayName("<gradient:#ff6b6b:#4ecdc4><bold>Paintball</bold></gradient>")
                .setLore(
                        "<gray>Colorful ammunition for paintball guns",
                        "<gray>",
                        "<yellow>✦ <green>Non-Biodegradable paint capsule",
                        "<yellow>✦ <blue>Splatters on impact",
                        "<yellow>✦ <purple>Non-lethal projectile",
                        "<gray>",
                        "<italic><light_purple>\"Leaves a colorful mark!\""
                )
                .setAmount(16) // TODO Update with player upgrades down the line
                .setMaxStackSize(90)
                .setRarity(ItemRarity.COMMON)
                .setPersistentData("custom_item", "paintball")
                .setPersistentData("projectile_type", "paintball")
                .setPersistentData("paint_color", "random")
                .setPersistentData("damage", 1)
                .build();

        this.registerItem("paintball", paintball);
    }
}
