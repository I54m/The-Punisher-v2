package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PlayerDataManager;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SeenCommand extends Command {
    public SeenCommand() {
        super("seen", "punisher.seen", "lastseen");
    }
    
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PlayerDataManager playerDataManager = PlayerDataManager.getINSTANCE();

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (strings.length != 1) {
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("View the last seen information about a player.").color(ChatColor.RED).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /seen <player>").color(ChatColor.WHITE).create());
            return;
        }
        if (strings[0].contains("#")) {
            int joinid;
            try {
                joinid = Integer.parseInt(strings[0].replace("#", ""));
            } catch (NumberFormatException nfe) {
                commandSender.sendMessage(new ComponentBuilder(strings[0] + " is not a valid join id, use #<joinid> to get seen information on the person with that id.").color(ChatColor.RED).create());
                return;
            }
            if (joinid > playerDataManager.getLastJoinId()) {
                commandSender.sendMessage(new ComponentBuilder(strings[0] + " is higher than the last used join id. Highest last used join id: " + playerDataManager.getLastJoinId()).color(ChatColor.RED).create());
                return;
            }
            Configuration playerData = playerDataManager.getPlayerData(joinid);
            String targetname = NameFetcher.getName(playerDataManager.getMainDataConfig().getString(String.valueOf(joinid)));
            if (targetname == null) {
                targetname = strings[0];
            }
            long lastlogin = (System.currentTimeMillis() - playerData.getLong("lastLogin"));
            long lastlogout = (System.currentTimeMillis() - playerData.getLong("lastLogout"));
            String lastloginString, lastlogoutString;
            int daysago = (int) (lastlogin / (1000 * 60 * 60 * 24));
            int hoursago = (int) (lastlogin / (1000 * 60 * 60) % 24);
            int minutesago = (int) (lastlogin / (1000 * 60) % 60);
            int secondsago = (int) (lastlogin / 1000 % 60);
            if (secondsago <= 0) secondsago = 1;
            if (daysago >= 1) lastloginString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (hoursago >= 1) lastloginString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (minutesago >= 1) lastloginString = minutesago + "m " + secondsago + "s " + " ago";
            else lastloginString = secondsago + "s " + " ago";
            daysago = (int) (lastlogout / (1000 * 60 * 60 * 24));
            hoursago = (int) (lastlogout / (1000 * 60 * 60) % 24);
            minutesago = (int) (lastlogout / (1000 * 60) % 60);
            secondsago = (int) (lastlogout / 1000 % 60);
            if (secondsago <= 0) secondsago = 1;
            if (daysago >= 1) lastlogoutString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (hoursago >= 1) lastlogoutString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (minutesago >= 1) lastlogoutString = minutesago + "m " + secondsago + "s " + " ago";
            else lastlogoutString = secondsago + "s " + " ago";
            boolean online = plugin.getProxy().getPlayer(targetname) != null;
            TextComponent onlineText;
            if (online) {
                onlineText = new TextComponent("Online");
                onlineText.setColor(ChatColor.GREEN);
            } else {
                onlineText = new TextComponent("Offline");
                onlineText.setColor(ChatColor.RED);
            }

            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Last seen Information for " + targetname).color(ChatColor.RED).append(" [").color(ChatColor.GRAY).append(onlineText).append("] ").color(ChatColor.GRAY).append("(#" + playerData.getInt("joinId") + ")").color(ChatColor.RED).create());
            commandSender.sendMessage(new ComponentBuilder("First Joined Date: ").color(ChatColor.RED).append(playerData.getString("firstJoin")).color(ChatColor.GREEN).create());
            commandSender.sendMessage(new ComponentBuilder("Last Login: ").color(ChatColor.RED).append(lastloginString).color(ChatColor.GREEN).create());
            commandSender.sendMessage(new ComponentBuilder("Last Server Played: ").color(ChatColor.RED).append(playerData.getString("lastServer")).color(ChatColor.GREEN).create());
            if (playerData.contains("lastLogout") && !online)
                commandSender.sendMessage(new ComponentBuilder("Last Logout: ").color(ChatColor.RED).append(lastlogoutString).color(ChatColor.GREEN).create());
            return;
        }
        ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
        Future<UUID> future;
        ExecutorService executorService;
        UUID targetuuid;
        String targetname = null;
        if (findTarget != null) {
            targetuuid = findTarget.getUniqueId();
            targetname = findTarget.getName();
        } else {
            UUIDFetcher uuidFetcher = new UUIDFetcher();
            uuidFetcher.fetch(strings[0]);
            executorService = Executors.newSingleThreadExecutor();
            future = executorService.submit(uuidFetcher);
            try {
                targetuuid = future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                try {
                    throw new DataFecthException("UUID Required for next step", strings[0], "UUID", this.getName(), e);
                }catch (DataFecthException dfe){
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(dfe);
                    errorHandler.alert(dfe, commandSender);
                }
                executorService.shutdown();
                return;
            }
            executorService.shutdown();
        }
        if (targetuuid == null) {
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[0] + " is not a player's name!").color(ChatColor.RED).create());
            return;
        }
        if (targetname == null) {
            targetname = NameFetcher.getName(targetuuid);
            if (targetname == null) {
                targetname = strings[0];
            }
        }
        Configuration playerData = playerDataManager.getPlayerData(targetuuid, false);
        if (playerData == null){
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + " has not joined the server yet!").color(ChatColor.RED).create());
            return;
        }
        long lastlogin = (System.currentTimeMillis() - playerData.getLong("lastLogin"));
        long lastlogout = (System.currentTimeMillis() - playerData.getLong("lastLogout"));
        String lastloginString, lastlogoutString;
        int daysago = (int) (lastlogin / (1000 * 60 * 60 * 24));
        int hoursago = (int) (lastlogin / (1000 * 60 * 60) % 24);
        int minutesago = (int) (lastlogin / (1000 * 60) % 60);
        int secondsago = (int) (lastlogin / 1000 % 60);
        if (secondsago <= 0) secondsago = 1;
        if (daysago >= 1) lastloginString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
        else if (hoursago >= 1) lastloginString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
        else if (minutesago >= 1) lastloginString = minutesago + "m " + secondsago + "s " + " ago";
        else lastloginString = secondsago + "s " + " ago";
        daysago = (int) (lastlogout / (1000 * 60 * 60 * 24));
        hoursago = (int) (lastlogout / (1000 * 60 * 60) % 24);
        minutesago = (int) (lastlogout / (1000 * 60) % 60);
        secondsago = (int) (lastlogout / 1000 % 60);
        if (secondsago <= 0) secondsago = 1;
        if (daysago >= 1) lastlogoutString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
        else if (hoursago >= 1) lastlogoutString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
        else if (minutesago >= 1) lastlogoutString = minutesago + "m " + secondsago + "s " + " ago";
        else lastlogoutString = secondsago + "s " + " ago";
        boolean online = plugin.getProxy().getPlayer(targetuuid) != null;
        TextComponent onlineText;
        if (online) {
            onlineText = new TextComponent("Online");
            onlineText.setColor(ChatColor.GREEN);
        } else {
            onlineText = new TextComponent("Offline");
            onlineText.setColor(ChatColor.RED);
        }
        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Last seen Information for " + targetname).color(ChatColor.RED).append(" [").color(ChatColor.GRAY).append(onlineText).append("] ").color(ChatColor.GRAY).append("(#" + playerData.getInt("joinId") + ")").color(ChatColor.RED).create());
        commandSender.sendMessage(new ComponentBuilder("First Joined Date: ").color(ChatColor.RED).append(playerData.getString("firstJoin")).color(ChatColor.GREEN).create());
        commandSender.sendMessage(new ComponentBuilder("Last Login: ").color(ChatColor.RED).append(lastloginString).color(ChatColor.GREEN).create());
        commandSender.sendMessage(new ComponentBuilder("Last Server Played: ").color(ChatColor.RED).append(playerData.getString("lastServer")).color(ChatColor.GREEN).create());
        if (playerData.contains("lastLogout") && !online)
            commandSender.sendMessage(new ComponentBuilder("Last Logout: ").color(ChatColor.RED).append(lastlogoutString).color(ChatColor.GREEN).create());
    }
}
