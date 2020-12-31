package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ToggleChatCommand extends Command {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    public ToggleChatCommand() {
        super("togglechat", "punisher.togglechat", "chattoggle");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
            if (!(commandSender instanceof ProxiedPlayer)) {
                commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
                return;
            }
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (plugin.chatOffServers.contains(player.getServer().getInfo())) {
                plugin.chatOffServers.remove(player.getServer().getInfo());
                for (ProxiedPlayer all : plugin.getProxy().getPlayers()) {
                    if (all.getServer().equals(player.getServer()))
                        all.sendMessage(new ComponentBuilder("\nChat Has Been toggled off!!\n").color(ChatColor.RED).bold(true).create());
                }
            } else if (!plugin.chatOffServers.contains(player.getServer().getInfo())) {
                plugin.chatOffServers.add(player.getServer().getInfo());
                for (ProxiedPlayer all : plugin.getProxy().getPlayers()) {
                    if (all.getServer().equals(player.getServer()))
                        all.sendMessage(new ComponentBuilder("\nChat Has Been toggled on!!\n").color(ChatColor.GREEN).bold(true).create());
                }
            }
    }
}
