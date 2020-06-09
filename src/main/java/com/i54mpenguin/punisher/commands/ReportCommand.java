package com.i54mpenguin.punisher.commands;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.chats.AdminChat;
import com.i54mpenguin.punisher.chats.StaffChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;

public class ReportCommand extends Command {

    private HashMap<String, Long> cooldowns = new HashMap<String, Long>();
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    public ReportCommand() {
        super("report", "punisher.report");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length == 0 || strings.length == 1) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("report a player who is breaking the rules").color(ChatColor.RED).append("\nUsage: /report <player> <reason>").color(ChatColor.WHITE).create());
            return;
        }
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(strings[0]);
        if (target == null) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not an online player's name!").color(ChatColor.RED).create());
        } else if (player == target) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are not allowed to report yourself!").color(ChatColor.RED).create());
        } else {
            if (target.hasPermission("punisher.report.bypass")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player may not be reported, if you believe this to be a mistake then please contact an Admin+!").color(ChatColor.RED).create());
            } else if (target.hasPermission("punisher.report.staff")) {
                if (!(player.hasPermission("punisher.report.bypass"))) {
                    int cooldownTime = 60;
                    if (cooldowns.containsKey(player.getDisplayName())) {
                        long secondsLeft = ((cooldowns.get(player.getDisplayName()) / 1000) + cooldownTime) - (System.currentTimeMillis() / 1000);
                        if (secondsLeft > 0) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You have recently reported someone please wait " + secondsLeft + "s before making another report!").color(ChatColor.RED).create());
                            return;
                        }
                    }
                }
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Thanks for submitting the report, Admins have been notified!").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Reminder: Abuse of this system will result in punishment!").color(ChatColor.RED).create());
                StringBuilder reason = new StringBuilder();
                for (int i = 1; i < strings.length; i++) {
                    reason.append(strings[i]).append(" ");
                }
                AdminChat.sendMessage(player.getName() + " has Reported: " + target.getName() + " for: " + reason, true);
                cooldowns.put(player.getName(), System.currentTimeMillis());
            } else {
                if (!(player.hasPermission("punisher.report.bypass"))) {
                    int cooldownTime = 60;
                    if (cooldowns.containsKey(player.getName())) {
                        long secondsLeft = ((cooldowns.get(player.getName()) / 1000) + cooldownTime) - (System.currentTimeMillis() / 1000);
                        if (secondsLeft > 0) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You have recently reported someone please wait " + secondsLeft + "s before making another report!").color(ChatColor.RED).create());
                            return;
                        }
                    }
                }
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Thanks for submitting the report, Staff have been notified!").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Reminder: Abuse of this system will result in punishment!").color(ChatColor.RED).create());
                StringBuilder reason = new StringBuilder();
                for (int i = 1; i < strings.length; i++) {
                    reason.append(strings[i]).append(" ");
                }
                StaffChat.sendMessage(player.getName() + " has Reported: " + target.getName() + " for: " + reason, true);
                cooldowns.put(player.getName(), System.currentTimeMillis());
            }
        }
    }
}