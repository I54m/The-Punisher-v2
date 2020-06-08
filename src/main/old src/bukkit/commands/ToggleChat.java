package com.i54mpenguin.punisher.bukkit.commands;

import com.i54mpenguin.punisher.bukkit.PunisherBukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ToggleChat implements CommandExecutor {
    private PunisherBukkit plugin = PunisherBukkit.getInstance();
    private String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;

    private static void sendPluginMessage(@NotNull Player player, String channel, @NotNull String... messages) {
        try {
            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outbytes);
            for (String msg : messages){
                out.writeUTF(msg);
            }
            player.sendPluginMessage(PunisherBukkit.getPlugin(PunisherBukkit.class), channel, outbytes.toByteArray());
            out.close();
            outbytes.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] strings) {
        if (label.equalsIgnoreCase("togglechat") || label.equalsIgnoreCase("tchat") || label.equalsIgnoreCase("chattoggle")) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage("You must be a player to use this command!");
                return false;
            }
            Player player = (Player) commandSender;
            if (!player.hasPermission("punisher.togglechat")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return false;
            }
            if (plugin.chatState.equals("on")) {
                plugin.chatState = "off";
                sendPluginMessage(player, "punisher:main", "chatToggled", "off");
                Bukkit.broadcastMessage("\n");
                Bukkit.broadcastMessage(prefix + ChatColor.RED + ChatColor.BOLD + "Chat Has Been toggled off!!");
                Bukkit.broadcastMessage("\n");
                return true;

            } else if (plugin.chatState.equals("off")) {
                plugin.chatState = "on";
                sendPluginMessage(player, "punisher:main", "chatToggled", "on");
                Bukkit.broadcastMessage("\n");
                Bukkit.broadcastMessage(prefix + ChatColor.GREEN + ChatColor.BOLD + "Chat Has Been toggled on!!");
                Bukkit.broadcastMessage("\n");
                return true;
            }
        }
        return false;
    }
}