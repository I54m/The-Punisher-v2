package com.i54m.punisher.objects.gui;

import com.i54m.protocol.inventory.InventoryType;
import com.i54m.protocol.items.ItemStack;
import com.i54m.protocol.items.ItemType;
import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.objects.IconMenu;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
@Deprecated
public class ConfirmationGUI {

    public static final IconMenu MENU = new IconMenu(InventoryType.GENERIC_9X3);
    private static final ArrayList<Integer> DENYSLOTS = new ArrayList<>();
    private static final ArrayList<Integer> CONFIRMSLOTS = new ArrayList<>();

    public static void open(final ProxiedPlayer punisher, String targetuuid, String targetName, ItemStack chosenItem, int chosenItemSlot) {
        MENU.setTitle(new ComponentBuilder("Confirm Punishing: " + targetName).color(ChatColor.RED).create());
        MENU.setClick((clicker, menu1, slot, item) -> {
            if (clicker == punisher) {
                if (item == null || item.getDisplayName().isEmpty()) return false;
                if (item.getDisplayName().contains(ChatColor.GREEN + "Confirm") && CONFIRMSLOTS.contains(slot)) {
                    PunisherPlugin.getInstance().getLogger().info("Now punishing " + targetName);
                    //PunishGUI.punishmentSelected(targetuuid, targetName, chosenItemSlot, chosenItem, clicker);
                    return true;
                } else if (item.getDisplayName().contains(ChatColor.RED + "Deny") && DENYSLOTS.contains(slot)) {
                    clicker.sendMessage(new ComponentBuilder("Punishment Cancelled!").color(ChatColor.RED).create());
                    return true;
                }
            }
            return false;
        });
        MENU.addButton(13, new ItemStack(ItemType.PAPER), ChatColor.RED + "" + ChatColor.BOLD + "WARNING!!!",
                ChatColor.RED + "Please confirm the following punishment:",
                ChatColor.RED + "Punishing: " + ChatColor.WHITE + targetName,
                ChatColor.RED + "Reason: " + ChatColor.WHITE + ChatColor.stripColor(chosenItem.getDisplayName()),
                " ",
                ChatColor.RED + "" + ChatColor.BOLD + "Think before you punish!");
        MENU.open(punisher);
    }

    public static void setupMenu() {
        ItemStack deny = new ItemStack(ItemType.RED_WOOL, 1);
        ItemStack confirm = new ItemStack(ItemType.LIME_WOOL, 1);
        ItemStack fill = new ItemStack(ItemType.GRAY_STAINED_GLASS_PANE, 1);
        MENU.addButton(0, deny, ChatColor.RED + "Deny");
        MENU.addButton(1, deny, ChatColor.RED + "Deny");
        MENU.addButton(2, deny, ChatColor.RED + "Deny");
        DENYSLOTS.add(0);
        DENYSLOTS.add(1);
        DENYSLOTS.add(2);
        MENU.addButton(3, fill, "");
        MENU.addButton(4, fill, "");
        MENU.addButton(5, fill, "");
        MENU.addButton(6, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(7, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(8, confirm, ChatColor.GREEN + "Confirm");
        CONFIRMSLOTS.add(6);
        CONFIRMSLOTS.add(7);
        CONFIRMSLOTS.add(8);
        MENU.addButton(9, deny, ChatColor.RED + "Deny");
        MENU.addButton(10, deny, ChatColor.RED + "Deny");
        MENU.addButton(11, deny, ChatColor.RED + "Deny");
        DENYSLOTS.add(9);
        DENYSLOTS.add(10);
        DENYSLOTS.add(11);
        MENU.addButton(12, fill, "");
        MENU.addButton(13, fill, "");
        MENU.addButton(14, fill, "");
        MENU.addButton(15, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(16, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(17, confirm, ChatColor.GREEN + "Confirm");
        CONFIRMSLOTS.add(15);
        CONFIRMSLOTS.add(16);
        CONFIRMSLOTS.add(17);
        MENU.addButton(18, deny, ChatColor.RED + "Deny");
        MENU.addButton(19, deny, ChatColor.RED + "Deny");
        MENU.addButton(20, deny, ChatColor.RED + "Deny");
        DENYSLOTS.add(18);
        DENYSLOTS.add(19);
        DENYSLOTS.add(20);
        MENU.addButton(21, fill, "");
        MENU.addButton(22, fill, "");
        MENU.addButton(23, fill, "");
        MENU.addButton(24, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(25, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(26, confirm, ChatColor.GREEN + "Confirm");
        CONFIRMSLOTS.add(24);
        CONFIRMSLOTS.add(25);
        CONFIRMSLOTS.add(26);
    }
}