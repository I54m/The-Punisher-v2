package com.i54m.punisher.objects.confirmationgui;

import com.i54m.punisher.bukkit.commands.PunishGUI;
import com.i54m.punisher.bukkit.objects.IconMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public interface ConfirmationGUI {

    IconMenu MENU = new IconMenu(3);
    ArrayList<Integer> DENYSLOTS = new ArrayList<>();
    ArrayList<Integer> CONFIRMSLOTS = new ArrayList<>();

    void setupMenu();

    default void open(final Player punisher, String targetuuid, String targetName, ItemStack chosenItem, int chosenItemSlot) {
        MENU.setName(ChatColor.RED + "Confirm Punishing: " + targetName);
        MENU.setClick((clicker, menu1, slot, item) -> {
            if (clicker == punisher) {
                if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
                if (item.getItemMeta().getDisplayName().contains(ChatColor.GREEN + "Confirm") && CONFIRMSLOTS.contains(slot)) {
                    PunishGUI.punishmentSelected(targetuuid, targetName, chosenItemSlot, chosenItem, clicker);
                    return true;
                } else if (item.getItemMeta().getDisplayName().contains(ChatColor.RED + "Deny") && DENYSLOTS.contains(slot)) {
                    clicker.sendMessage(ChatColor.RED + "Punishment Cancelled!");
                    return true;
                }
            }
            return false;
        });
        MENU.addButton(13, new ItemStack(Material.PAPER), ChatColor.RED + "" + ChatColor.BOLD + "WARNING!!!",
                ChatColor.RED + "Please confirm the following punishment:",
                ChatColor.RED + "Punishing: " + ChatColor.WHITE + targetName,
                ChatColor.RED + "Reason: " + ChatColor.WHITE + ChatColor.stripColor(chosenItem.getItemMeta().getDisplayName()),
                " ",
                ChatColor.RED + "" + ChatColor.BOLD + "Think before you punish!");
        MENU.open(punisher);
    }
}
