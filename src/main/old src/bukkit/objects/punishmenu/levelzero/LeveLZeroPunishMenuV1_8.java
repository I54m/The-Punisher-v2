package com.i54m.punisher.bukkit.objects.punishmenu.levelzero;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LeveLZeroPunishMenuV1_8 implements LevelZeroPunishMenu {

    @SuppressWarnings("deprecation")
    @Override
    public void setupMenu() {
        ItemStack fill = new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 14);
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
        MENU.addButton(11, fill, " ");
        MENU.addButton(12, new ItemStack(Material.ICE, 1), ChatColor.RED + "Minor Chat Offence", ChatColor.WHITE + "Spam, Flood ETC");
        MENU.addButton(13, new ItemStack(Material.NETHERRACK, 1), ChatColor.RED + "Major Chat Offence", ChatColor.WHITE + "Racism, Disrespect ETC");
        MENU.addButton(14, new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 2), ChatColor.RED + "Other Offence", ChatColor.WHITE + "For Other Not Listed Offences", ChatColor.WHITE + "(Will ban for 30 minutes regardless of offence)");
        MENU.addButton(15, fill, " ");
        MENU.addButton(16, fill, " ");
        MENU.addButton(17, fill, " ");

    }
}
