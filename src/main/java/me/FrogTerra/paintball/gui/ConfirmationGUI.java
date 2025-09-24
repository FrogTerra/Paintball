package me.FrogTerra.paintball.gui;

import me.FrogTerra.paintball.item.ItemCreator;
import me.FrogTerra.paintball.utility.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;

import java.util.List;

/**
 * Generic confirmation GUI for dangerous actions
 */
public class ConfirmationGUI extends GUI {

    private final String title;
    private final String question;
    private final List<String> details;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmationGUI(final String title, final String question, final List<String> details, 
                          final Runnable onConfirm, final Runnable onCancel) {
        super(Rows.THREE, MessageUtils.parseMessage(title));
        this.title = title;
        this.question = question;
        this.details = details;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public void onSetItems() {
        // Question display
        this.setItem(13, new ItemCreator(Material.PAPER)
                .setDisplayName(this.question)
                .setLore(this.details.toArray(new String[0]))
                .setRarity(ItemRarity.COMMON)
                .build());

        // Confirm button
        this.setItem(11, new ItemCreator(Material.LIME_CONCRETE)
                .setDisplayName("<green><bold>CONFIRM")
                .setLore(
                        "<gray>Click to confirm this action",
                        "",
                        "<red><bold>This action cannot be undone!"
                )
                .setRarity(ItemRarity.EPIC)
                .build(), player -> {
                    player.closeInventory();
                    if (this.onConfirm != null) {
                        this.onConfirm.run();
                    }
                });

        // Cancel button
        this.setItem(15, new ItemCreator(Material.RED_CONCRETE)
                .setDisplayName("<red><bold>CANCEL")
                .setLore("<gray>Click to cancel and go back")
                .setRarity(ItemRarity.COMMON)
                .build(), player -> {
                    player.closeInventory();
                    if (this.onCancel != null) {
                        this.onCancel.run();
                    }
                });
    }
}