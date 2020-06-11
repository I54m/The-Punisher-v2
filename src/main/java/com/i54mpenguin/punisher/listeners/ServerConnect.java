package com.i54mpenguin.punisher.listeners;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.utils.UpdateChecker;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ServerConnect implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    public static int lastJoinId = 0;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event){
        ProxiedPlayer player = event.getPlayer();
//        if (player.hasPermission("punisher.staff")) {
//            ArrayList<ProxiedPlayer> server = plugin.staff.get(event.getTarget());
//            if (server == null || server.isEmpty())
//                server = new ArrayList<>();
//            server.add(player);
//            plugin.staff.put(event.getTarget(), server);
//        }
        if (event.getReason().equals(ServerConnectEvent.Reason.JOIN_PROXY)) {
            if (PunisherPlugin.isUpdate() && player.hasPermission("punisher.admin")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Update checker found an update, current version: " + plugin.getDescription().getVersion() + " latest version: "
                        + UpdateChecker.getCurrentVersion() + " this update was released on: " + UpdateChecker.getRealeaseDate()).color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This may fix some bugs and enhance features, You will also no longer receive support for this version!").color(ChatColor.RED).create());
            }
        }
    }
}
