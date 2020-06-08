package com.i54mpenguin.punisher.bukkit.objects.punishmenu.leveltwo;

import com.i54mpenguin.punisher.bukkit.PunisherBukkit;
import com.i54mpenguin.punisher.bukkit.objects.IconMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public interface LevelTwoPunishMenu {

    IconMenu MENU = new IconMenu(4);

    void setupMenu();

    default void open(final Player punisher, String targetuuid, String targetName, String reputation) {
        MENU.setClick((player, menu, slot, item) -> {
            if (item.getItemMeta().getLore() != null) {
                menu.close(player);
                PunisherBukkit.getInstance().confirmationGUI.open(punisher, targetuuid, targetName, item, slot);
                return true;
            }
            return false;
        });
        MENU.setName(ChatColor.RED + "Punish: " + targetName + " " + reputation);
        MENU.open(punisher);
    }
}
