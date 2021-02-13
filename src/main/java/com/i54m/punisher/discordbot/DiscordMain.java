package com.i54m.punisher.discordbot;


import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.discordbot.commands.DiscordCommand;
import com.i54m.punisher.discordbot.listeners.PlayerDisconnect;
import com.i54m.punisher.discordbot.listeners.ServerConnect;
import com.i54m.punisher.discordbot.listeners.ServerConnected;
import com.i54m.punisher.discordbot.listeners.discord.BotReady;
import com.i54m.punisher.discordbot.listeners.discord.PrivateMessageReceived;
import com.i54m.punisher.handlers.ErrorHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DiscordMain {

    private static final PunisherPlugin plugin = PunisherPlugin.getInstance();
    public static JDA jda;
    public static Map<UUID, String> userCodes = new HashMap<>();
    public static Map<UUID, String> verifiedUsers = new HashMap<>();
    public static final Yaml YAML_LOADER = new Yaml();
    public static List<ScheduledTask> updateTasks = new ArrayList<>();
    private static boolean firstenable = true;
    public static Guild guild = null;
    public static TextChannel loggingChannel = null;

    public static void startBot() {
        plugin.getLogger().info(ChatColor.GREEN + "Starting Discord bot...");
        try {
            jda = JDABuilder.createDefault(plugin.getConfig().getString("DiscordIntegration.BotToken"))
                    .addEventListeners(new BotReady(), new PrivateMessageReceived())
                    .disableCache(CacheFlag.ACTIVITY)
                    .setMemberCachePolicy(MemberCachePolicy.ONLINE.or(MemberCachePolicy.VOICE).or(MemberCachePolicy.OWNER))
                    .disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING)
                    .build();
            if (firstenable) {
                ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new DiscordCommand());
                firstenable = false;
            }

            plugin.getLogger().info(ChatColor.GREEN + "Discord bot Started!");
        } catch (LoginException e) {
            plugin.getLogger().severe(ChatColor.RED + "Could not start Discord bot!");
            ErrorHandler.getINSTANCE().log(e);
        }
    }

    public static void shutdown() {
        if (jda != null) {
            plugin.getLogger().info(ChatColor.GREEN + "Shutting down Discord bot...");
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
            plugin.getLogger().info( ChatColor.GREEN + "Discord bot Shut down!");
        }
    }
}
