package me.FrogTerra.paintball.item;

import lombok.NonNull;
import me.FrogTerra.paintball.Paintball;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemCreator {
    private final ItemStack itemStack;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Paintball plugin;
    private final Map<String, Object> persistentData;

    public ItemCreator(@NonNull final Material material) {
        this(material, 1);
    }

    public ItemCreator(@NonNull final Material material, final int amount) {
        this.itemStack = new ItemStack(material, Math.max(1, amount));
        this.plugin = Paintball.getPlugin();
        this.persistentData = new ConcurrentHashMap<>();
    }

    public ItemCreator(@NonNull final ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.plugin = Paintball.getPlugin();
        this.persistentData = new ConcurrentHashMap<>();
    }

    /**
     * Sets the display name of the item using MiniMessage formatting.
     * @param displayName The display name to set
     * @return The current instance for method chaining
     */
    public ItemCreator setDisplayName(@NonNull final String displayName) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.displayName(this.miniMessage.deserialize(displayName));
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets the lore of the item using MiniMessage formatting.
     * @param loreLines The lore lines as an array
     * @return The current instance for method chaining
     */
    public ItemCreator setLore(@NonNull final String... loreLines) {
        if (loreLines.length == 0) {
            return this;
        }

        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            final List<Component> componentLore = Arrays.stream(loreLines)
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .map(this.miniMessage::deserialize)
                    .toList();
            itemMeta.lore(componentLore);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Adds a single line to the item's lore.
     * @param loreLine The lore line to add
     * @return The current instance for method chaining
     */
    public ItemCreator addLoreLine(@NonNull final String loreLine) {
        if (loreLine.trim().isEmpty()) {
            return this;
        }

        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            final List<Component> currentLore = itemMeta.hasLore() ?
                    new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
            currentLore.add(this.miniMessage.deserialize(loreLine));
            itemMeta.lore(currentLore);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Clears all lore from the item.
     * @return The current instance for method chaining
     */
    public ItemCreator clearLore() {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.lore(new ArrayList<>());
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets the custom model data for the item.
     * @param customModelData The custom model data value
     * @return The current instance for method chaining
     */
    public ItemCreator setCustomModelData(final int customModelData) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.setCustomModelData(customModelData);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets the material type of the item.
     * @param material The new material
     * @return The current instance for method chaining
     */
    public ItemCreator setMaterial(@NonNull final Material material) {
        this.itemStack.setType(material);
        return this;
    }

    /**
     * Sets the amount of the item.
     * @param amount The new amount
     * @return The current instance for method chaining
     */
    public ItemCreator setAmount(final int amount) {
        this.itemStack.setAmount(Math.max(1, Math.min(amount, this.itemStack.getMaxStackSize())));
        return this;
    }

    /**
     * Sets whether the item is unbreakable.
     * @param unbreakable Whether the item should be unbreakable
     * @return The current instance for method chaining
     */
    public ItemCreator setUnbreakable(final boolean unbreakable) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Adds item flags to hide certain attributes.
     * @param itemFlags The item flags to add
     * @return The current instance for method chaining
     */
    public ItemCreator addItemFlags(@NonNull final ItemFlag... itemFlags) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(itemFlags);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Removes item flags.
     * @param itemFlags The item flags to remove
     * @return The current instance for method chaining
     */
    public ItemCreator removeItemFlags(@NonNull final ItemFlag... itemFlags) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.removeItemFlags(itemFlags);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets the maximum stack size for the item.
     * @param maxStackSize The maximum stack size (1-99)
     * @return The current instance for method chaining
     */
    public ItemCreator setMaxStackSize(final int maxStackSize) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            final int clampedSize = Math.max(1, Math.min(maxStackSize, 99));
            itemMeta.setMaxStackSize(clampedSize);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets the item rarity.
     * @param rarity The item rarity
     * @return The current instance for method chaining
     */
    public ItemCreator setRarity(@NonNull final ItemRarity rarity) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.setRarity(rarity);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Adds an enchantment to the item.
     * @param enchantment The enchantment to add
     * @param level The enchantment level
     * @param ignoreLevelRestriction Whether to ignore level restrictions
     * @return The current instance for method chaining
     */
    public ItemCreator addEnchantment(@NonNull final Enchantment enchantment, final int level, final boolean ignoreLevelRestriction) {
        this.itemStack.addUnsafeEnchantment(enchantment, Math.max(1, level));
        return this;
    }

    /**
     * Adds an enchantment to the item with level restrictions.
     * @param enchantment The enchantment to add
     * @param level The enchantment level
     * @return The current instance for method chaining
     */
    public ItemCreator addEnchantment(@NonNull final Enchantment enchantment, final int level) {
        this.itemStack.addEnchantment(enchantment, Math.max(1, level));
        return this;
    }

    /**
     * Removes an enchantment from the item.
     * @param enchantment The enchantment to remove
     * @return The current instance for method chaining
     */
    public ItemCreator removeEnchantment(@NonNull final Enchantment enchantment) {
        this.itemStack.removeEnchantment(enchantment);
        return this;
    }

    /**
     * Adds an attribute modifier to the item.
     * @param attribute The attribute to modify
     * @param modifier The attribute modifier
     * @return The current instance for method chaining
     */
    public ItemCreator addAttributeModifier(@NonNull final Attribute attribute, @NonNull final AttributeModifier modifier) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.addAttributeModifier(attribute, modifier);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Removes all attribute modifiers for a specific attribute.
     * @param attribute The attribute to remove modifiers for
     * @return The current instance for method chaining
     */
    public ItemCreator removeAttributeModifier(@NonNull final Attribute attribute) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.removeAttributeModifier(attribute);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Stores persistent data on the item.
     * @param key The data key
     * @param value The data value
     * @return The current instance for method chaining
     */
    public ItemCreator setPersistentData(@NonNull final String key, @NonNull final String value) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            final NamespacedKey namespacedKey = new NamespacedKey(this.plugin, key);
            itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Stores persistent integer data on the item.
     * @param key The data key
     * @param value The integer value
     * @return The current instance for method chaining
     */
    public ItemCreator setPersistentData(@NonNull final String key, final int value) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            final NamespacedKey namespacedKey = new NamespacedKey(this.plugin, key);
            itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, value);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets whether the item should hide tooltips.
     * @param hideTooltip Whether to hide tooltips
     * @return The current instance for method chaining
     */
    public ItemCreator setHideTooltip(final boolean hideTooltip) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.setHideTooltip(hideTooltip);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets the item's fire resistant property (1.21.8 feature).
     * @param fireResistant Whether the item should be fire resistant
     * @return The current instance for method chaining
     */
    public ItemCreator setFireResistant(final boolean fireResistant) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.setFireResistant(fireResistant);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets the item's enchantment glint override (1.21.8 feature).
     * @param hasGlint Whether the item should have enchantment glint
     * @return The current instance for method chaining
     */
    public ItemCreator setEnchantmentGlintOverride(final boolean hasGlint) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.setEnchantmentGlintOverride(hasGlint);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Sets persistent data to track cooldown information.
     * @param cooldownTicks The cooldown in ticks (stored as data)
     * @param cooldownGroup The cooldown group identifier (stored as data)
     * @return The current instance for method chaining
     */
    public ItemCreator setUseCooldown(final int cooldownTicks, @NonNull final String cooldownGroup) {
        // Store cooldown data as persistent data for custom handling
        this.setPersistentData("cooldown_ticks", Math.max(0, cooldownTicks));
        this.setPersistentData("cooldown_group", cooldownGroup);
        return this;
    }

    /**
     * Sets persistent data for food properties (custom food handling).
     * @param nutrition The nutrition value
     * @param saturation The saturation value
     * @param canAlwaysEat Whether the item can always be eaten
     * @return The current instance for method chaining
     */
    public ItemCreator setFoodComponent(final int nutrition, final float saturation, final boolean canAlwaysEat) {
        // Store food data as persistent data for custom handling
        this.setPersistentData("food_nutrition", Math.max(0, nutrition));
        this.setPersistentData("food_saturation", String.valueOf(Math.max(0.0f, saturation)));
        this.setPersistentData("food_always_eat", canAlwaysEat ? 1 : 0);
        return this;
    }

    /**
     * Sets persistent data for tool properties (custom tool handling).
     * @param defaultMiningSpeed The default mining speed
     * @param damagePerBlock The damage per block mined
     * @return The current instance for method chaining
     */
    public ItemCreator setToolComponent(final float defaultMiningSpeed, final int damagePerBlock) {
        // Store tool data as persistent data for custom handling
        this.setPersistentData("tool_mining_speed", String.valueOf(Math.max(0.0f, defaultMiningSpeed)));
        this.setPersistentData("tool_damage_per_block", Math.max(0, damagePerBlock));
        return this;
    }

    /**
     * Sets the item's item name (different from display name, 1.21.8 feature).
     * @param itemName The item name component
     * @return The current instance for method chaining
     */
    public ItemCreator setItemName(@NonNull final String itemName) {
        final ItemMeta itemMeta = this.getOrCreateMeta();
        if (itemMeta != null) {
            itemMeta.itemName(this.miniMessage.deserialize(itemName));
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    /**
     * Creates a glowing effect on the item without adding enchantments.
     * @return The current instance for method chaining
     */
    public ItemCreator makeGlow() {
        this.setEnchantmentGlintOverride(true);
        return this;
    }

    /**
     * Removes the glowing effect from the item.
     * @return The current instance for method chaining
     */
    public ItemCreator removeGlow() {
        this.setEnchantmentGlintOverride(false);
        return this;
    }

    /**
     * Gets the completed ItemStack.
     * @return The final ItemStack
     */
    public ItemStack build() {
        return this.itemStack.clone();
    }

    /**
     * Gets the raw ItemStack (not cloned).
     * @return The raw ItemStack
     */
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    /**
     * Helper method to get or create ItemMeta safely.
     * @return The ItemMeta or null if it cannot be created
     */
    private ItemMeta getOrCreateMeta() {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta == null) {
            itemMeta = Bukkit.getItemFactory().getItemMeta(this.itemStack.getType());
        }
        return itemMeta;
    }
}