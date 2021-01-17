package com.i54m.punisher.discordbot.listeners;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.handlers.ErrorHandler;
import net.dv8tion.jda.api.entities.TextChannel;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnect implements Listener {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (DiscordMain.jda == null) return;
        ProxiedPlayer player = event.getPlayer();
        if (plugin.getConfig().getBoolean("DiscordIntegration.EnableJoinLogging")) {
            TextChannel loggingChannel = DiscordMain.jda.getTextChannelById(plugin.getConfig().getString("DiscordIntegration.JoinLoggingChannelId"));
            if (loggingChannel != null)
                loggingChannel.sendMessage(":heavy_minus_sign: " + player.getName() + " Left the server!").queue();
            else
                ErrorHandler.getINSTANCE().log(new NullPointerException("Could not find join logging channel!"));
        }
    }
}
