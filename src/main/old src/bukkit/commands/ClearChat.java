package com.i54m.punisher.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChat implements CommandExecutor {
    private String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("clearchat") || label.equalsIgnoreCase("cc") || label.equalsIgnoreCase("chatclear")) {
            if (!(sender instanceof Player)) {
                for (Player all : Bukkit.getOnlinePlayers()) {
                    if (!all.hasPermission("punisher.clearchat.bypass")) {
                        for (int x = 0; x < 1000; x++) {
                            all.sendMessage("\n");
                        }
                    }
                }
                Bukkit.broadcastMessage("\n");
                Bukkit.broadcastMessage(prefix + ChatColor.RED + "" + ChatColor.BOLD
                        + "Chat has been Cleared by: CONSOLE");
                Bukkit.broadcastMessage("\n");
                return true;
            }
            Player p = (Player) sender;
            if (p.hasPermission("punisher.clearchat")) {
                if (args.length > 0) {
                    if (args[0].equals("-s") || args[0].equals("-all") && p.hasPermission("punisher.clearchat.staff")){
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            for (int x = 0; x < 1000; x++) {
                                all.sendMessage("\n");
                            }
                        }
                    }else {
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            if (!all.hasPermission("punisher.clearchat.bypass")) {
                                for (int x = 0; x < 1000; x++) {
                                    all.sendMessage("\n");
                                }
                            }
                        }
                    }
                    if ((args[0].equals("-a") && p.hasPermission("punisher.clearchat.anon"))){
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            if (!all.hasPermission("punisher.clearchat.bypass")) {
                                for (int x = 0; x < 1000; x++) {
                                    all.sendMessage("\n");
                                }
                            }
                        }
                        Bukkit.broadcastMessage("\n");
                        Bukkit.broadcastMessage(prefix + ChatColor.RED + "" + ChatColor.BOLD
                                + "Chat has been Cleared" );
                        Bukkit.broadcastMessage("\n");
                        return true;
                    }
                }else {
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (!all.hasPermission("punisher.clearchat.bypass")) {
                            for (int x = 0; x < 1000; x++) {
                                all.sendMessage("\n");
                            }
                        }
                    }
                }
                Bukkit.broadcastMessage("\n");
                Bukkit.broadcastMessage(prefix + ChatColor.RED + "" + ChatColor.BOLD
                        + "Chat has been Cleared by: " + p.getName());
                Bukkit.broadcastMessage("\n");
                return true;
            } else {
                p.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return false;
            }
        }
        return false;
    }
}

