package com.i54m.punisher.discordbot.listeners.discord;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.discordbot.listeners.PlayerDisconnect;
import com.i54m.punisher.discordbot.listeners.ServerConnect;
import com.i54m.punisher.discordbot.listeners.ServerConnected;
import com.i54m.punisher.managers.WorkerManager;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BotReady extends ListenerAdapter {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private int status = 1;

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        DiscordMain.guild = DiscordMain.jda.getGuildById("418941534797561857");
        if (DiscordMain.guild == null)
            throw new NullPointerException("Could not find Guild: " + plugin.getConfig().getString("DiscordIntegration.GuildId") + "!");
        if (plugin.getConfig().getBoolean("DiscordIntegration.EnableJoinLogging")) {
            ProxyServer.getInstance().getPluginManager().registerListener(plugin, new ServerConnected());
            ProxyServer.getInstance().getPluginManager().registerListener(plugin, new PlayerDisconnect());
            DiscordMain.loggingChannel = DiscordMain.guild.getTextChannelById(plugin.getConfig().getString("DiscordIntegration.JoinLoggingChannelId"));
            if (DiscordMain.loggingChannel == null)
                throw new NullPointerException("Could not find logging channel!");
            DiscordMain.updateTasks.add(ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                DiscordMain.loggingChannel = DiscordMain.guild.getTextChannelById(plugin.getConfig().getString("DiscordIntegration.JoinLoggingChannelId"));
                if (DiscordMain.loggingChannel == null)
                    throw new NullPointerException("Could not find logging channel!");
            }, 15, 15, TimeUnit.MINUTES));
        }
        if (plugin.getConfig().getBoolean("DiscordIntegration.EnableRoleSync")) {
            DiscordMain.updateTasks.add(ProxyServer.getInstance().getScheduler().schedule(plugin, () -> WorkerManager.getINSTANCE().runWorker(new WorkerManager.Worker(() -> {
                        new HashMap<>(DiscordMain.verifiedUsers).forEach((uuid, id) -> {//clean up old users that have deleted their accounts or have left the guild
                            User user = DiscordMain.jda.getUserById(id);
                            if (user == null)
                                DiscordMain.verifiedUsers.remove(uuid);
                            else if (!DiscordMain.guild.isMember(user))
                                DiscordMain.verifiedUsers.remove(uuid);
                        });
                        DiscordMain.verifiedUsers.forEach((uuid, id) -> {//sync linked user roles over to the discord
                            for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesIdsToAddToLinkedUser")) {
                                User user = DiscordMain.jda.getUserById(id);
                                if (user != null) {
                                    Member member = DiscordMain.guild.getMember(user);
                                    Role role = DiscordMain.guild.getRoleById(roleids);
                                    if (member != null && role != null)
                                        DiscordMain.guild.addRoleToMember(member, role).queue();
                                }
                            }
                        });
                        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {//sync user's synced roles over to the discord
                            if (DiscordMain.verifiedUsers.containsKey(player.getUniqueId())) {
                                User user = DiscordMain.jda.getUserById(DiscordMain.verifiedUsers.get(player.getUniqueId()));
                                if (user != null)
                                    for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesToSync")) {
                                        Member member = DiscordMain.guild.getMember(user);
                                        if (member != null) {
                                            member.getRoles().forEach((role -> {
                                                if (roleids.equals(role.getId()) && !player.hasPermission("punisher.discord.role." + roleids))
                                                    DiscordMain.guild.removeRoleFromMember(member, role).queue();
                                            }));
                                            Role role = DiscordMain.guild.getRoleById(roleids);
                                            if (player.hasPermission("punisher.discord.role." + roleids) && role != null)
                                                DiscordMain.guild.addRoleToMember(member, role).queue();
                                        }
                                    }
                            }
                        }
                    }))
                    , 10, 30, TimeUnit.SECONDS));
        }
        if (plugin.getConfig().getBoolean("DiscordIntegration.EnableRoleSync") || plugin.getConfig().getBoolean("DiscordIntegration.EnableJoinLogging")) {
            ProxyServer.getInstance().getPluginManager().registerListener(plugin, new ServerConnect());
        }
        try {
            Object obj = DiscordMain.YAML_LOADER.load(new FileInputStream(plugin.discordIntegrationFile));
            if (obj instanceof HashMap)
                DiscordMain.verifiedUsers = (HashMap<UUID, String>) obj;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        DiscordMain.updateTasks.add(ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            switch (status) {
                case 1:
                    DiscordMain.jda.getPresence().setActivity(Activity.playing(plugin.getConfig().getString("DiscordIntegration.Playing")));
                    status++;
                    return;
                case 2:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching(DiscordMain.verifiedUsers.size() + " Linked Users!"));
                    status++;
                    return;
                case 3:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching("For Rule Breakers!"));
                    status++;
                    return;
                case 4:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching("For Punishments!"));
                    status++;
                    return;
                case 5:
                    DiscordMain.jda.getPresence().setActivity(Activity.playing("With The Ban Hammer!"));
                    status++;
                    return;
                case 6:
                    DiscordMain.jda.getPresence().setActivity(Activity.listening("I54m"));
                    status++;
                    return;
                case 7:
                    DiscordMain.jda.getPresence().setActivity(Activity.playing("v" + plugin.getDescription().getVersion()));
                    status = 1;

            }
        }, 1, 10, TimeUnit.SECONDS));
    }
}
