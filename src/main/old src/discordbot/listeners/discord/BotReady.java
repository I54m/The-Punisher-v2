package me.fiftyfour.punisher.bungee.discordbot.listeners.discord;

import com.i54m.punisher.listeners.ServerConnect;
import com.i54m.punisher.managers.PunishmentManager;
import me.fiftyfour.punisher.bungee.PunisherPlugin;
import me.fiftyfour.punisher.bungee.discordbot.DiscordMain;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;

import java.util.concurrent.TimeUnit;

public class BotReady extends ListenerAdapter {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punishMngr = PunishmentManager.getINSTANCE();
    private int status = 1;

    @Override
    public void onReady(ReadyEvent event) {
        DiscordMain.updateTasks.add(ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            switch (status) {
                case 1:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching(punishMngr.totalBans() + " Bans!"));
                    status++;
                    return;
                case 2:
                    DiscordMain.jda.getPresence().setActivity(Activity.listening(punishMngr.totalMutes() + " Mutes!"));
                    status++;
                    return;
                case 3:
                    DiscordMain.jda.getPresence().setActivity(Activity.playing(plugin.getConfig().getString("DiscordIntegration.Playing")));
                    status++;
                    return;
                case 4:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching(DiscordMain.verifiedUsers.size() + " Linked Users!"));
                    status++;
                    return;
                case 5:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching(ServerConnect.lastJoinId + " Total Joins!"));
                    status++;
                    return;
                case 6:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching("For Rule Breakers!"));
                    status++;
                    return;
                case 7:
                    DiscordMain.jda.getPresence().setActivity(Activity.watching("For Punishments!"));
                    status++;
                    return;
                case 8:
                    DiscordMain.jda.getPresence().setActivity(Activity.playing("With The Ban Hammer!"));
                    status++;
                    return;
                case 9:
                    DiscordMain.jda.getPresence().setActivity(Activity.listening("54mpenguin"));
                    status ++;
                    return;
                case 10:
                    DiscordMain.jda.getPresence().setActivity(Activity.playing("v" + plugin.getDescription().getVersion()));
                    status = 1;

            }
        }, 1, 10, TimeUnit.SECONDS));
    }
}
