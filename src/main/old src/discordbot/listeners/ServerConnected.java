package me.fiftyfour.punisher.bungee.discordbot.listeners;

import me.fiftyfour.punisher.bungee.PunisherPlugin;
import me.fiftyfour.punisher.bungee.discordbot.DiscordMain;
import net.dv8tion.jda.api.entities.TextChannel;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ServerConnected implements Listener {

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event){
        if (DiscordMain.jda == null) return;
        ProxiedPlayer player = event.getPlayer();
        ServerInfo server = event.getServer().getInfo();
        TextChannel loggingChannel = DiscordMain.jda.getTextChannelById(PunisherPlugin.config.getString("DiscordIntegration.JoinLoggingChannelId"));
        loggingChannel.sendMessage(":arrow_right: " + player.getName() + " **Connected to: " + server.getName() + "!**").queue();
    }
}
