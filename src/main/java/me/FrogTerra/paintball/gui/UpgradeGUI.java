package me.FrogTerra.paintball.gui;

import me.FrogTerra.paintball.utility.MessageUtils;
import net.kyori.adventure.text.Component;

public class UpgradeGUI extends GUI {

    public UpgradeGUI(Rows rows, Component title) {
        super(rows, MessageUtils.parseMessage("<black>Player Upgrade Menu!"));
    }

    @Override
    public void onSetItems() {
        setItem(1, getPlugin().getItemRegistery().getCustomItem("paintball_gun"));
        setItem(2, getPlugin().getItemRegistery().getCustomItem("paintball"));
    }
}
