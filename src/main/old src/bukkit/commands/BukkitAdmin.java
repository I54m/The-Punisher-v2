package com.i54mpenguin.punisher.bukkit.commands;

import com.i54mpenguin.punisher.bukkit.PunisherBukkit;
import com.i54mpenguin.punisher.utils.UpdateChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;


public class BukkitAdmin implements CommandExecutor {
    private String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;
    private PunisherBukkit plugin = PunisherBukkit.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender.hasPermission("punisher.bukkit.admin")) {
            if (strings.length <= 0) {
                commandSender.sendMessage(prefix + ChatColor.GREEN + "/punisherbukkit version");
                return true;
            }
            if (strings[0].equalsIgnoreCase("version")) {
                if (!PunisherBukkit.update) {
                    commandSender.sendMessage(prefix + ChatColor.GREEN + "Current Bukkit plugin Version: " + plugin.getDescription().getVersion());
                    commandSender.sendMessage(prefix + ChatColor.GREEN + "Running Compatibility settings for: " + plugin.COMPATIBILITY);
                    commandSender.sendMessage(prefix + ChatColor.GREEN + "This is the latest version!");
                } else {
                    commandSender.sendMessage(prefix + ChatColor.RED + "Current Bukkit plugin Version: " + plugin.getDescription().getVersion());
                    commandSender.sendMessage(prefix + ChatColor.RED + "Running Compatibility settings for: " + plugin.COMPATIBILITY);
                    commandSender.sendMessage(prefix + ChatColor.RED + "Latest version: " + UpdateChecker.getCurrentVersion());
                }
                return true;
            } else {
                commandSender.sendMessage(prefix + ChatColor.RED + "Check the current Bukkit plugin version");
                commandSender.sendMessage(prefix + ChatColor.RED + "Usage: /punisherbukkit version");
            }
        }else{
            commandSender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        }
        return false;
    }
}
