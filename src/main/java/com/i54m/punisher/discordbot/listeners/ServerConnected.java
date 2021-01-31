package com.i54m.punisher.discordbot.listeners;

import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.managers.PlayerDataManager;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ServerConnected implements Listener {

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        if (DiscordMain.jda == null) return;
        if (!PlayerDataManager.getINSTANCE().getPlayerData(event.getPlayer(), true).getBoolean("staffHide"))
            DiscordMain.loggingChannel.sendMessage(":arrow_right: " + event.getPlayer().getName() + " **Connected to: " + event.getServer().getInfo().getName() + "!**").queue();
    }
}
