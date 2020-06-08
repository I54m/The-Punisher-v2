package com.i54mpenguin.punisher.listeners;

import me.fiftyfour.punisher.bungee.PunisherPlugin;
import com.i54mpenguin.punisher.utils.UpdateChecker;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class ServerConnect implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    public static int lastJoinId = 0;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event){
        ProxiedPlayer player = event.getPlayer();
        if (player.hasPermission("punisher.staff")) {
            ArrayList<ProxiedPlayer> server = plugin.staff.get(event.getTarget());
            if (server == null || server.isEmpty())
                server = new ArrayList<>();
            server.add(player);
            plugin.staff.put(event.getTarget(), server);
        }
        if (event.getReason().equals(ServerConnectEvent.Reason.JOIN_PROXY)) {
            if (PunisherPlugin.update && player.hasPermission("punisher.admin")) {
                player.sendMessage(new ComponentBuilder(plugin.prefix).append("Update checker found an update, current version: " + plugin.getDescription().getVersion() + " latest version: "
                        + UpdateChecker.getCurrentVersion() + " this update was released on: " + UpdateChecker.getRealeaseDate()).color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.prefix).append("This may fix some bugs and enhance features, You will also no longer receive support for this version!").color(ChatColor.RED).create());
            }
        }
        if (PunisherPlugin.playerInfoConfig.contains(player.getUniqueId().toString().replace("-", ""))) {
            if (event.getReason().equals(ServerConnectEvent.Reason.JOIN_PROXY)) {
                PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".lastlogin", System.currentTimeMillis());
                if (event.getTarget() != null)
                    PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".lastserver", event.getTarget().getName());
                PunisherPlugin.saveInfo();
            } else if (event.getTarget() != null) {
                PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".lastserver", event.getTarget().getName());
                PunisherPlugin.saveInfo();
            }
        }else{
            Date date = new Date();
            date.setTime(System.currentTimeMillis());
            DateFormat df = new SimpleDateFormat("dd MMM yyyy, hh:mm (Z)");
            df.setTimeZone(TimeZone.getDefault());
            PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".firstjoin", df.format(date));
            PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".joinid", (lastJoinId + 1));
            PunisherPlugin.playerInfoConfig.set("lastjoinid", (lastJoinId + 1));
            PunisherPlugin.playerInfoConfig.set(String.valueOf((lastJoinId + 1)), player.getUniqueId().toString().replace("-", ""));
            lastJoinId++;
            PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".lastlogin", System.currentTimeMillis());
            if (event.getTarget() != null)
                PunisherPlugin.playerInfoConfig.set(player.getUniqueId().toString().replace("-", "") + ".lastserver", event.getTarget().getName());
            PunisherPlugin.saveInfo();
        }
    }
}
