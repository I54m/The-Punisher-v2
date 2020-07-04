package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.chats.StaffChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class KickAllCommand extends Command {
    public KickAllCommand() {
        super("kickall", "punisher.kick.all");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (ProxyServer.getInstance().getPlayers().size() == 0){
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "No players online to kick!"));
            return;
        }
        if (commandSender instanceof ProxiedPlayer){
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length >= 1){
                if (strings[0].equalsIgnoreCase("-network")){
                    for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                        if (!player.equals(all)) {
                            all.disconnect(new TextComponent(ChatColor.RED + "You have been Kicked from the server!\nYou were kicked for the reason: Manually Kicked!\nYou may reconnect at anytime, but make sure to read the /rules!"));
                            StaffChat.sendMessage(player.getName() + " Kicked: " + all.getName() + " for: Manually Kicked", true);
                        }
                    }
                }else{
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot supply a message for a kick all, use -network to kick all players network-wide.").color(ChatColor.RED).create());
                }
            }else {
                for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                    if (!player.equals(all)  && player.getServer().getInfo().getName().equals(all.getServer().getInfo().getName())) {
                        all.disconnect(new TextComponent(ChatColor.RED + "You have been Kicked from the server!\nYou were kicked for the reason: Manually Kicked!\nYou may reconnect at anytime, but make sure to read the /rules!"));
                        StaffChat.sendMessage(player.getName() + " Kicked: " + all.getName() + " for: Manually Kicked", true);
                    }
                }
            }
        }else{
            for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()){
                all.disconnect(new TextComponent(ChatColor.RED + "You have been Kicked from the server!\nYou were kicked for the reason: Manually Kicked!\nYou may reconnect at anytime, but make sure to read the /rules!"));
                ProxyServer.getInstance().getLogger().info(ChatColor.RED + "Kicked: " + all.getName());
            }
        }
    }
}
