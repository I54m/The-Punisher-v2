package com.i54mpenguin.punisher.objects.confirmationgui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ConfirmationGUIv1_13 implements ConfirmationGUI {


    @Override
    public void setupMenu() {
        ItemStack deny = new ItemStack(Material.RED_WOOL, 1);
        ItemStack confirm = new ItemStack(Material.LIME_WOOL, 1);
        MENU.addButton(0, deny, ChatColor.RED + "Deny");
        MENU.addButton(1, deny, ChatColor.RED + "Deny");
        MENU.addButton(2, deny, ChatColor.RED + "Deny");
        DENYSLOTS.add(0);
        DENYSLOTS.add(1);
        DENYSLOTS.add(2);
        MENU.addButton(9, deny, ChatColor.RED + "Deny");
        MENU.addButton(10, deny, ChatColor.RED + "Deny");
        MENU.addButton(11, deny, ChatColor.RED + "Deny");
        DENYSLOTS.add(9);
        DENYSLOTS.add(10);
        DENYSLOTS.add(11);
        MENU.addButton(18, deny, ChatColor.RED + "Deny");
        MENU.addButton(19, deny, ChatColor.RED + "Deny");
        MENU.addButton(20, deny, ChatColor.RED + "Deny");
        DENYSLOTS.add(18);
        DENYSLOTS.add(19);
        DENYSLOTS.add(20);
        MENU.addButton(6, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(7, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(8, confirm, ChatColor.GREEN + "Confirm");
        CONFIRMSLOTS.add(6);
        CONFIRMSLOTS.add(7);
        CONFIRMSLOTS.add(8);
        MENU.addButton(15, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(16, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(17, confirm, ChatColor.GREEN + "Confirm");
        CONFIRMSLOTS.add(15);
        CONFIRMSLOTS.add(16);
        CONFIRMSLOTS.add(17);
        MENU.addButton(24, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(25, confirm, ChatColor.GREEN + "Confirm");
        MENU.addButton(26, confirm, ChatColor.GREEN + "Confirm");
        CONFIRMSLOTS.add(24);
        CONFIRMSLOTS.add(25);
        CONFIRMSLOTS.add(26);
    }
}
