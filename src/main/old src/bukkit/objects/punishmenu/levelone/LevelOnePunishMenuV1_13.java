package com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelone;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LevelOnePunishMenuV1_13 implements LevelOnePunishMenu {

    @Override
    public void setupMenu() {
        ItemStack fill = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
        ItemStack Iron_Boots = new ItemStack(Material.IRON_BOOTS, 1);
        ItemMeta ibmeta = Iron_Boots.getItemMeta();
        ibmeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        Iron_Boots.setItemMeta(ibmeta);
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
        MENU.addButton(11, new ItemStack(Material.BLUE_ICE, 1), ChatColor.RED + "Minor Chat Offence", ChatColor.WHITE + "Spam, Flood ETC");
        MENU.addButton(12, new ItemStack(Material.MAGMA_BLOCK, 1), ChatColor.RED + "Major Chat Offence", ChatColor.WHITE + "Racism, Disrespect ETC");
        MENU.addButton(13, Iron_Boots, ChatColor.RED + "DDoS/DoX Threats", ChatColor.WHITE + "Includes Hinting At It and", ChatColor.WHITE + "Saying They Have a Player's DoX");
        MENU.addButton(14, new ItemStack(Material.WRITABLE_BOOK, 1), ChatColor.RED + "Inappropriate Link", ChatColor.WHITE + "Includes Pm's");
        MENU.addButton(15, new ItemStack(Material.DRAGON_HEAD, 1), ChatColor.RED + "Scamming", ChatColor.WHITE + "When a player is unfairly taking a player's money", ChatColor.WHITE + "Through a fake scheme");
        MENU.addButton(16, fill, " ");
        MENU.addButton(17, fill, " ");
    }
}
