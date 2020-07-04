package com.i54m.punisher.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class TabComplete implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent e){
        String partialPlayerName = e.getCursor().toLowerCase();
        int lastSpaceIndex = partialPlayerName.lastIndexOf(' ');
        if (lastSpaceIndex >= 0) {
            partialPlayerName = partialPlayerName.substring(lastSpaceIndex + 1);
        }
        e.getSuggestions().clear();
        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()){
            if (p.getName().toLowerCase().startsWith(partialPlayerName)){
                e.getSuggestions().add(p.getName());
            }
        }
    }
}
