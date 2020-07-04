package com.i54m.punisher.bukkit.objects.punishmenu.leveltwo;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LevelTwoPunishMenuV1_8 implements LevelTwoPunishMenu {

    @SuppressWarnings("deprecation")
    @Override
    public void setupMenu() {
        ItemStack fill = new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 14);
        ItemStack Iron_Boots = new ItemStack(Material.IRON_BOOTS, 1);
        ItemMeta ibmeta = Iron_Boots.getItemMeta();
        ibmeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        Iron_Boots.setItemMeta(ibmeta);
        ItemStack Wood_Sword = new ItemStack(Material.valueOf("WOOD_SWORD"), 1);
        ItemMeta wsmeta = Wood_Sword.getItemMeta();
        wsmeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        Wood_Sword.setItemMeta(wsmeta);
        ItemStack Iron_Sword = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta ismeta = Iron_Sword.getItemMeta();
        ismeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        Iron_Sword.setItemMeta(ismeta);
        ItemStack Diamond_Sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta dsmeta = Diamond_Sword.getItemMeta();
        dsmeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        Diamond_Sword.setItemMeta(dsmeta);
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
        MENU.addButton(11, new ItemStack(Material.ICE, 1), ChatColor.RED + "Minor Chat Offence", ChatColor.WHITE + "Spam, Flood ETC");
        MENU.addButton(12, new ItemStack(Material.NETHERRACK, 1), ChatColor.RED + "Major Chat Offence", ChatColor.WHITE + "Racism, Disrespect ETC");
        MENU.addButton(13, Iron_Boots, ChatColor.RED + "DDoS/DoX Threats", ChatColor.WHITE + "Includes Hinting At It and", ChatColor.WHITE + "Saying They Have a Player's DoX");
        MENU.addButton(14, new ItemStack(Material.valueOf("BOOK_AND_QUILL"), 1), ChatColor.RED + "Inappropriate Link", ChatColor.WHITE + "Includes Pm's");
        MENU.addButton(15, new ItemStack(Material.PAPER, 1), ChatColor.RED + "Scamming", ChatColor.WHITE + "When a player is unfairly taking a player's money", ChatColor.WHITE + "Through a fake scheme");
        MENU.addButton(16, fill, " ");
        MENU.addButton(17, fill, " ");
        MENU.addButton(18, new ItemStack(Material.GLASS, 1), ChatColor.RED + "X-Raying", ChatColor.WHITE + "Mining Straight to Ores/Bases/Chests", ChatColor.WHITE + "Includes Chest Esp and Player Esp");
        MENU.addButton(19, Wood_Sword, ChatColor.RED + "AutoClicker (Non PvP)", ChatColor.WHITE + "Using AutoClicker to Farm Mobs ETC");
        MENU.addButton(20, new ItemStack(Material.FEATHER, 1), ChatColor.RED + "Fly/Speed Hacking", ChatColor.WHITE + "Includes Hacks Such as Jesus and Spider");
        MENU.addButton(21, Diamond_Sword, ChatColor.RED + "Malicous PvP Hacks", ChatColor.WHITE + "Includes Hacks Such as Kill Aura and Reach");
        MENU.addButton(22, Iron_Sword, ChatColor.RED + "Disallowed Mods", ChatColor.WHITE + "Includes Hacks Such as Derp and Headless");
        MENU.addButton(23, new ItemStack(Material.TNT, 1), ChatColor.RED + "Greifing", ChatColor.WHITE + "Excludes Cobble Monstering and Bypassing land claims", ChatColor.WHITE + "Includes Things Such as 'lava curtaining' and TnT Cannoning");
        MENU.addButton(24, new ItemStack(Material.valueOf("SIGN"), 1), ChatColor.RED + "Server Advertisement", ChatColor.RED + "Warning: Must Also Clear Chat After you have proof!!");
        MENU.addButton(25, new ItemStack(Material.FURNACE, 1), ChatColor.RED + "Exploiting", ChatColor.WHITE + "Includes Bypassing Land Claims and Cobble Monstering");
        MENU.addButton(26, new ItemStack(Material.IRON_TRAPDOOR, 1), ChatColor.RED + "TPA-Trapping", ChatColor.WHITE + "Sending a TPA Request to Someone", ChatColor.WHITE + "and Then Killing Them Once They Tp");
        MENU.addButton(27, fill, " ");
        MENU.addButton(28, fill, " ");
        MENU.addButton(29, fill, " ");
        MENU.addButton(30, new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 2), ChatColor.RED + "Other Minor Offence", ChatColor.WHITE + "For Other Minor Offences", ChatColor.WHITE + "(Will ban for 7 days regardless of offence)");
        MENU.addButton(31, new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3), ChatColor.RED + "Impersonation", ChatColor.WHITE + "Any type of Impersonation");
        MENU.addButton(32, new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 4), ChatColor.RED + "Other Major Offence", ChatColor.WHITE + "Includes Inappropriate IGN's and Other Major Offences", ChatColor.WHITE + "(Will ban for 30 days regardless of offence)");
        MENU.addButton(33, fill, " ");
        MENU.addButton(34, fill, " ");
        MENU.addButton(35, fill, " ");
    }
}
