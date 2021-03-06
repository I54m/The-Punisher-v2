package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class IpHistCommand extends Command {

    public IpHistCommand() {
        super("iphist", "punisher.alts.ip", "ihist", "ih");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private UUID targetuuid;

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length < 1) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Please provide a player's name!").create());
                return;
            }
            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
            Future<UUID> future = null;
            ExecutorService executorService = null;
            if (findTarget != null) {
                targetuuid = findTarget.getUniqueId();
            } else {
                UUIDFetcher uuidFetcher = new UUIDFetcher();
                uuidFetcher.fetch(strings[0]);
                executorService = Executors.newSingleThreadExecutor();
                future = executorService.submit(uuidFetcher);
            }
            if (future != null) {
                try {
                    targetuuid = future.get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    DataFetchException dfe = new DataFetchException(this.getName(), "UUID", strings[0], e, "UUID Required for next step");
                    errorHandler.log(dfe);
                    errorHandler.alert(dfe, commandSender);
                    executorService.shutdown();
                    return;
                }
                executorService.shutdown();
            }
            if (targetuuid == null) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name!").color(ChatColor.RED).create());
                return;
            }
            String targetName = NameFetcher.getName(targetuuid);
            if (targetName == null) {
                targetName = strings[0];
            }
            try {
                TreeMap<Long, String> iphist = plugin.getStorageManager().getIpHist(targetuuid);
                if (iphist.isEmpty()) {
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetName + " does not have any ips stored in the database").color(ChatColor.RED).create());
                    return;
                }
                player.sendMessage(new ComponentBuilder("|-----").strikethrough(true).color(ChatColor.GREEN).append(" " + targetName + "'s Ip hist ")
                        .strikethrough(false).color(ChatColor.RED).append("-----|").strikethrough(true).color(ChatColor.GREEN).create());
                while (!iphist.isEmpty()) {
                    long timeago = (System.currentTimeMillis() - iphist.firstEntry().getKey());
                    String ip = iphist.firstEntry().getValue();
                    int daysago = (int) (timeago / (1000 * 60 * 60 * 24));
                    int hoursago = (int) (timeago / (1000 * 60 * 60) % 24);
                    int minutesago = (int) (timeago / (1000 * 60) % 60);
                    if (minutesago <= 0) minutesago = 1;
                    if (daysago >= 1) {
                        player.sendMessage(new ComponentBuilder(ip).color(ChatColor.RED).append(" " + daysago + "d " + hoursago + "h " + minutesago + "m ago").color(ChatColor.GREEN).create());
                    } else if (hoursago >= 1) {
                        player.sendMessage(new ComponentBuilder(ip).color(ChatColor.RED).append(" " + hoursago + "h " + minutesago + "m ago").color(ChatColor.GREEN).create());
                    } else {
                        player.sendMessage(new ComponentBuilder(ip).color(ChatColor.RED).append(" " + minutesago + "m ago").color(ChatColor.GREEN).create());
                    }
                    iphist.remove(iphist.firstKey());
                }
            } catch (Exception sqle) {
                try {
                    throw new PunishmentsStorageException("Checking ip history", targetName, this.getName(), sqle, "/iphist", strings);
                } catch (PunishmentsStorageException pse) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(pse);
                    errorHandler.alert(pse, commandSender);
                }
            }
        } else commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
    }
}
