package com.i54mpenguin.punisher.listeners;

import me.fiftyfour.punisher.bungee.PunisherPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;

public class PlayerDisconnect implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player.hasPermission("punisher.staff")) {
            ArrayList<ProxiedPlayer> server = plugin.staff.get(player.getServer().getInfo());
            if (server == null || server.isEmpty())
                return;
            server.remove(player);
            plugin.staff.put(player.getServer().getInfo(), server);
        }
        PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".lastlogout", System.currentTimeMillis());
        PunisherPlugin.saveInfo();
    }
}
