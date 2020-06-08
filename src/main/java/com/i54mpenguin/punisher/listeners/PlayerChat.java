package com.i54mpenguin.punisher.listeners;

import me.fiftyfour.punisher.bungee.PunisherPlugin;
import com.i54mpenguin.punisher.chats.StaffChat;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.objects.Punishment;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PlayerChat implements Listener {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punishmentManager = PunishmentManager.getINSTANCE();

    @EventHandler
    public void onChat(ChatEvent event) {
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        ServerInfo server = player.getServer().getInfo();
        if (PluginMessage.chatOffServers.contains(server)) {
            if (!player.hasPermission("punisher.togglechat.bypass")) {
                if (event.isCommand()) {
                    String[] args = event.getMessage().split(" ");
                    List<String> mutedcommands;
                    mutedcommands = PunisherPlugin.config.getStringList("Muted Commands");
                    if (mutedcommands.contains(args[0])) {
                        event.setCancelled(true);
                        player.sendMessage(new ComponentBuilder(plugin.prefix).append("You may not use that command at this time!").color(ChatColor.RED).create());
                    } else {
                        event.setCancelled(false);
                    }
                    return;
                }
                event.setCancelled(true);
                player.sendMessage(new ComponentBuilder(plugin.prefix).append("You may not chat at this time!").color(ChatColor.RED).create());
            } else {
                event.setCancelled(false);
            }
            return;
        }
        UUID uuid = player.getUniqueId();
        String fetcheduuid = uuid.toString().replace("-", "");
        String targetname = player.getName();
        try {
            if (punishmentManager.isMuted(fetcheduuid)) {
                Punishment mute = punishmentManager.getMute(fetcheduuid);
                if (player.hasPermission("punisher.bypass")) {
                    punishmentManager.remove(mute, null, true, true, false);
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Bypassed their mute, Unmuting...").color(ChatColor.RED).event(mute.getHoverEvent()).create(), true);
                    return;
                }
                if (System.currentTimeMillis() > mute.getExpiration()) {
                    punishmentManager.remove(mute, null, false, false, false);
                    player.sendMessage(new ComponentBuilder(plugin.prefix).append("Your Mute has expired!").color(ChatColor.GREEN).create());
                    PunisherPlugin.LOGS.info(player.getName() + "'s mute expired so they were unmuted");
                } else {
                    String timeLeft = punishmentManager.getTimeLeft(mute);
                    if (event.isCommand()) {
                        String[] args = event.getMessage().split(" ");
                        List<String> mutedcommands;
                        mutedcommands = PunisherPlugin.config.getStringList("Muted Commands");
                        if (!mutedcommands.contains(args[0])) {
                            event.setCancelled(true);
                            String muteMessage;
                            if (mute.isPermanent())
                                muteMessage = PunisherPlugin.config.getString("PermMute Deny Message").replace("%reason%", mute.getMessage()).replace("%timeleft%", timeLeft);
                            else
                                muteMessage = PunisherPlugin.config.getString("TempMute Deny Message").replace("%reason%", mute.getMessage()).replace("%timeleft%", timeLeft);

                            player.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', muteMessage)).create());
                        }
                    } else {
                        event.setCancelled(true);
                        String muteMessage;
                        if (mute.isPermanent())
                            muteMessage = PunisherPlugin.config.getString("PermMute Deny Message").replace("%reason%", mute.getMessage()).replace("%timeleft%", timeLeft);
                        else
                            muteMessage = PunisherPlugin.config.getString("TempMute Deny Message").replace("%reason%", mute.getMessage()).replace("%timeleft%", timeLeft);
                        player.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', muteMessage)).create());
                        if (PunisherPlugin.config.getBoolean("SendPlayersMessageToStaffChatOnMuteDeny"))
                            StaffChat.sendMessage(player.getName() + " Tried to speak but is muted: " + event.getMessage(), true);
                        else if (PunisherPlugin.config.getBoolean("StaffChatOnMuteDeny"))
                            StaffChat.sendMessage(player.getName() + " Tried to speak but is muted!", true);
                    }
                }
            }
        } catch (SQLException e) {
            try {
                throw new PunishmentsDatabaseException("Removing mute on a player", targetname, this.getClass().getName(), e);
            } catch (PunishmentsDatabaseException pde) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(pde);
                errorHandler.alert(pde, player);
                errorHandler.adminChatAlert(pde, player);
                event.setCancelled(true);
            }
        }
    }
}