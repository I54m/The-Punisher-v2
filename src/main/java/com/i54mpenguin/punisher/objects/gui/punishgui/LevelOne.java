package com.i54mpenguin.punisher.objects.gui.punishgui;

import com.i54mpenguin.protocol.inventory.InventoryType;
import com.i54mpenguin.protocol.items.ItemFlag;
import com.i54mpenguin.protocol.items.ItemStack;
import com.i54mpenguin.protocol.items.ItemType;
import com.i54mpenguin.punisher.objects.IconMenu;
import com.i54mpenguin.punisher.objects.gui.ConfirmationGUI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class LevelOne {

    public static final IconMenu MENU = new IconMenu(InventoryType.GENERIC_9X2);

    public static void open(final ProxiedPlayer punisher, String targetuuid, String targetName, String reputation) {
        MENU.setTitle(new ComponentBuilder("Punish: " + targetName + " ").color(ChatColor.RED).appendLegacy(reputation).create());
        MENU.setClick((player, menu, slot, item) -> {
            if (item.getLore() != null && !item.getLore().isEmpty()) {
//                menu.close(player);
                ConfirmationGUI.open(punisher, targetuuid, targetName, item, slot);
                menu.close(player);
                return true;
            }
            return false;
        });
        MENU.open(punisher);
    }

    public static void setupMenu() {
        ItemStack fill = new ItemStack(ItemType.RED_STAINED_GLASS_PANE, 1);
        ItemStack ironBoots = new ItemStack(ItemType.IRON_BOOTS, 1);
        ironBoots.setFlag(ItemFlag.HIDE_ATTRIBUTES, true);
        MENU.addButton(0, fill, " ");
        MENU.addButton(1, fill, " ");
        MENU.addButton(2, fill, " ");
        MENU.addButton(3, fill, " ");
        MENU.addButton(4, fill, " ");
        MENU.addButton(5, fill, " ");
        MENU.addButton(6, fill, " ");
        MENU.addButton(7, fill, " ");
        MENU.addButton(8, fill, " ");
        MENU.addButton(9, fill, " ");
        MENU.addButton(10, fill, " ");
        MENU.addButton(11, new ItemStack(ItemType.BLUE_ICE, 1), ChatColor.RED + "Minor Chat Offence", ChatColor.WHITE + "Spam, Flood ETC");
        MENU.addButton(12, new ItemStack(ItemType.MAGMA_BLOCK, 1), ChatColor.RED + "Major Chat Offence", ChatColor.WHITE + "Racism, Disrespect ETC");
        MENU.addButton(13, ironBoots, ChatColor.RED + "DDoS/DoX Threats", ChatColor.WHITE + "Includes Hinting At It and", ChatColor.WHITE + "Saying They Have a Player's DoX");
        MENU.addButton(14, new ItemStack(ItemType.WRITABLE_BOOK, 1), ChatColor.RED + "Inappropriate Link", ChatColor.WHITE + "Includes Pm's");
        MENU.addButton(15, new ItemStack(ItemType.DRAGON_HEAD, 1), ChatColor.RED + "Scamming", ChatColor.WHITE + "When a player is unfairly taking a player's money", ChatColor.WHITE + "Through a fake scheme");
        MENU.addButton(16, fill, " ");
        MENU.addButton(17, fill, " ");
    }
}
