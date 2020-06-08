package me.fiftyfour.punisher.bungee.discordbot.listeners;

import me.fiftyfour.punisher.bungee.PunisherPlugin;
import me.fiftyfour.punisher.bungee.discordbot.DiscordMain;
import net.dv8tion.jda.api.entities.TextChannel;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnect implements Listener {

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event){
        if (DiscordMain.jda == null) return;
        ProxiedPlayer player = event.getPlayer();
        if (PunisherPlugin.config.getBoolean("DiscordIntegration.EnableJoinLogging")) {
            TextChannel loggingChannel = DiscordMain.jda.getTextChannelById(PunisherPlugin.config.getString("DiscordIntegration.JoinLoggingChannelId"));
            loggingChannel.sendMessage(":heavy_minus_sign: " + player.getName() + " Left the server!").queue();
        }
    }
}
