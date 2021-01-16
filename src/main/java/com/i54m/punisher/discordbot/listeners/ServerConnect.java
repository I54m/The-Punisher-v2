package com.i54m.punisher.discordbot.listeners;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.WorkerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ServerConnect implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        if (DiscordMain.jda == null) return;
        ProxiedPlayer player = event.getPlayer();
        if (plugin.getConfig().getBoolean("DiscordIntegration.EnableRoleSync") && DiscordMain.verifiedUsers.containsKey(player.getUniqueId()) && event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            workerManager.runWorker(new WorkerManager.Worker(() -> {
                User user = DiscordMain.jda.getUserById(DiscordMain.verifiedUsers.get(player.getUniqueId()));
                Guild guild = DiscordMain.jda.getGuildById(plugin.getConfig().getString("DiscordIntegration.GuildId"));
                if (guild != null && user != null)
                    for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesToSync")) {
                        try {
                            guild.getMember(user).getRoles().forEach((role -> {
                                if (roleids.equals(role.getId()) && !player.hasPermission("punisher.discord.role." + roleids))
                                    guild.removeRoleFromMember(guild.getMember(user), role).queue();

                            }));
                            if (player.hasPermission("punisher.discord.role." + roleids) && guild.getRoleById(roleids) != null)
                                guild.addRoleToMember(guild.getMember(user), guild.getRoleById(roleids)).queue();
                        } catch (NullPointerException npe) {
                            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                            errorHandler.log(npe);
                        }
                    }
            }));
        }
        if (plugin.getConfig().getBoolean("DiscordIntegration.EnableJoinLogging")) {
            TextChannel loggingChannel = DiscordMain.jda.getTextChannelById(plugin.getConfig().getString("DiscordIntegration.JoinLoggingChannelId"));
            if (loggingChannel != null)
                switch (event.getReason()) {
                    case JOIN_PROXY:
                        loggingChannel.sendMessage(":heavy_plus_sign: " + player.getName() + " **Joined the server!**").queue();
                        return;
                    case SERVER_DOWN_REDIRECT:
                        loggingChannel.sendMessage(":x: " + player.getName() + " **Was previously on a server but it went down!**").queue();
                        return;
                    case KICK_REDIRECT:
                        loggingChannel.sendMessage(":boot: " + player.getName() + " **Was previously on a server but was kicked!**").queue();
                        return;
                    case LOBBY_FALLBACK:
                        loggingChannel.sendMessage(":electric_plug: " + player.getName() + " **Was unable to join the default server and was redirected to a fallback server!**").queue();
                        return;
                    case PLUGIN:
                        loggingChannel.sendMessage(":gear: **A plugin caused " + player.getName() + " to be redirected!**").queue();
                }
        }
    }
}
