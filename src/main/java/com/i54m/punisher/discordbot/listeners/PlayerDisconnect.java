package com.i54m.punisher.discordbot.listeners;

import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.managers.PlayerDataManager;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnect implements Listener {

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (DiscordMain.jda == null) return;
        if (!PlayerDataManager.getINSTANCE().getPlayerData(event.getPlayer(), true).getBoolean("staffHide"))
            DiscordMain.loggingChannel.sendMessage(":heavy_minus_sign: " + event.getPlayer().getName() + " Left the server!").queue();
    }
}
