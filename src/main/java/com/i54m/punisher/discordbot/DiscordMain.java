package com.i54m.punisher.discordbot;


import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.discordbot.commands.DiscordCommand;
import com.i54m.punisher.discordbot.listeners.PlayerDisconnect;
import com.i54m.punisher.discordbot.listeners.ServerConnect;
import com.i54m.punisher.discordbot.listeners.ServerConnected;
import com.i54m.punisher.discordbot.listeners.discord.BotReady;
import com.i54m.punisher.discordbot.listeners.discord.PrivateMessageReceived;
import com.i54m.punisher.managers.WorkerManager;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DiscordMain {

    private static final PunisherPlugin plugin = PunisherPlugin.getInstance();
    public static JDA jda;
    public static Map<UUID, String> userCodes = new HashMap<>();
    public static Map<UUID, String> verifiedUsers = new HashMap<>();
    private static final Yaml YAML_LOADER = new Yaml();
    public static List<ScheduledTask> updateTasks = new ArrayList<>();
    private static boolean firstenable = true;
    public static Guild guild;

    public static void startBot() {
        plugin.getLogger().info(plugin.getPrefix() + ChatColor.GREEN + "Starting Discord bot...");
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(plugin.getConfig().getString("DiscordIntegration.BotToken")).build();
            jda.addEventListener(new BotReady());
            jda.addEventListener(new PrivateMessageReceived());
            if (firstenable) {
                ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new DiscordCommand());
                firstenable = false;
            }
            guild = jda.getGuildById(plugin.getConfig().getString("DiscordIntegration.GuildId"));
            if (plugin.getConfig().getBoolean("DiscordIntegration.EnableJoinLogging")) {
                ProxyServer.getInstance().getPluginManager().registerListener(plugin, new ServerConnected());
                ProxyServer.getInstance().getPluginManager().registerListener(plugin, new PlayerDisconnect());
                updateTasks.add(ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                    TextChannel loggingChannel = DiscordMain.jda.getTextChannelById(plugin.getConfig().getString("DiscordIntegration.JoinLoggingChannelId"));
                    if (loggingChannel == null)
                        throw new NullPointerException("Could not find logging channel!");
                }, 10, 5, TimeUnit.SECONDS));
            }
            if (plugin.getConfig().getBoolean("DiscordIntegration.EnableRoleSync")) {
                updateTasks.add(ProxyServer.getInstance().getScheduler().schedule(plugin, () -> WorkerManager.getINSTANCE().runWorker(new WorkerManager.Worker(() -> {
                            new HashMap<>(verifiedUsers).forEach((uuid, id) -> {//clean up old users that have deleted their accounts or have left the guild
                                User user = jda.getUserById(id);
                                if (user == null)
                                    verifiedUsers.remove(uuid);
                                else if (!guild.isMember(user))
                                    verifiedUsers.remove(uuid);
                            });
                            verifiedUsers.forEach((uuid, id) -> {//sync linked user roles over to the discord
                                for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesIdsToAddToLinkedUser")) {
                                    User user = jda.getUserById(id);
                                    if (user != null) {
                                        Member member = guild.getMember(user);
                                        Role role = guild.getRoleById(roleids);
                                        if (member != null && role != null)
                                            guild.addRoleToMember(member, role).queue();
                                    }
                                }
                            });
                            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {//sync user's synced roles over to the discord
                                if (verifiedUsers.containsKey(player.getUniqueId())) {
                                    User user = jda.getUserById(verifiedUsers.get(player.getUniqueId()));
                                    if (user != null)
                                        for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesToSync")) {
                                            Member member = guild.getMember(user);
                                            if (member != null) {
                                                member.getRoles().forEach((role -> {
                                                    if (roleids.equals(role.getId()) && !player.hasPermission("punisher.discord.role." + roleids))
                                                        guild.removeRoleFromMember(member, role).queue();
                                                }));
                                                Role role = guild.getRoleById(roleids);
                                                if (player.hasPermission("punisher.discord.role." + roleids) && role != null)
                                                    guild.addRoleToMember(member, role).queue();
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
                Object obj = YAML_LOADER.load(new FileInputStream(plugin.discordIntegrationFile));
                if (obj instanceof HashMap)
                    verifiedUsers = (HashMap<UUID, String>) obj;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            plugin.getLogger().info(plugin.getPrefix() + ChatColor.GREEN + "Discord bot Started!");
        } catch (LoginException e) {
            plugin.getLogger().severe(plugin.getPrefix() + ChatColor.RED + "Could not start Discord bot!");
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        if (jda != null) {
            plugin.getLogger().info(plugin.getPrefix() + ChatColor.GREEN + "Shutting down Discord bot...");
            try {
                YAML_LOADER.dump(verifiedUsers, new FileWriter(plugin.discordIntegrationFile));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            jda.shutdownNow();
            jda = null;
            ProxyServer.getInstance().getPluginManager().unregisterListener(new ServerConnected());
            ProxyServer.getInstance().getPluginManager().unregisterListener(new PlayerDisconnect());
            ProxyServer.getInstance().getPluginManager().unregisterListener(new ServerConnect());
            for (ScheduledTask task : updateTasks)
                task.cancel();
            plugin.getLogger().info(plugin.getPrefix() + ChatColor.GREEN + "Discord bot Shut down!");
        }
    }
}