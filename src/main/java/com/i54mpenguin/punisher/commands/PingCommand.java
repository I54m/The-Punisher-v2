package com.i54mpenguin.punisher.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PingCommand extends Command {
    public PingCommand() {
        super("ping");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (strings.length == 0) {
            if (commandSender instanceof ProxiedPlayer) {
                ProxiedPlayer player = (ProxiedPlayer) commandSender;
                int ping = player.getPing();
                if (ping <= 50)
                    player.sendMessage(new TextComponent(ChatColor.DARK_AQUA + "Ping: " + ChatColor.GREEN + ping + "ms"));
                else if (ping <= 150)
                    player.sendMessage(new TextComponent(ChatColor.DARK_AQUA + "Ping: " + ChatColor.GOLD + ping + "ms"));
                else
                    player.sendMessage(new TextComponent(ChatColor.DARK_AQUA + "Ping: " + ChatColor.RED + ping + "ms"));
            }else{
                commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            }
        } else if (strings[0] != null) {
            if (commandSender instanceof ProxiedPlayer) {
                ProxiedPlayer player = (ProxiedPlayer) commandSender;
                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(strings[0]);
                if (target == null){
                    player.sendMessage(new TextComponent(ChatColor.RED + "That is not an online player's name!"));
                    return;
                }
                int ping = target.getPing();
                if (ping <= 50)
                    player.sendMessage(new TextComponent(ChatColor.DARK_AQUA + "" + target + "'s Ping: " + ChatColor.GREEN + ping + "ms"));
                else if (ping <= 150)
                    player.sendMessage(new TextComponent(ChatColor.DARK_AQUA + "" + target + "'s Ping: " + ChatColor.GOLD + ping + "ms"));
                else
                    player.sendMessage(new TextComponent(ChatColor.DARK_AQUA + "" + target + "'s Ping: " + ChatColor.RED + ping + "ms"));
            }else{
                commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            }
        } else {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            player.sendMessage(new TextComponent(ChatColor.RED + "That is not an online player's name!"));
        }
    }
}