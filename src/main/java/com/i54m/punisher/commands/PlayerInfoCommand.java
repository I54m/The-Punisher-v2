package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.fetchers.PlayerInfo;
import com.i54m.punisher.fetchers.Status;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PlayerInfoCommand extends Command {
    public PlayerInfoCommand() {
        super("playerinfo", "punisher.playerinfo", "info", "pi", "player");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private UUID targetuuid;

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)){
            commandSender.sendMessage(new TextComponent("You need to be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length <= 0){
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("View useful information about a player").color(ChatColor.RED).create());
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /playerinfo <player>").color(ChatColor.WHITE).create());
            return;
        }
        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Collecting info on: " + strings[0] + ". Please wait...").color(ChatColor.GREEN).create());
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
        }
        if (targetuuid == null) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[0] + " is not a player's name!").color(ChatColor.RED).create());
            return;
        }
        String targetname = NameFetcher.getName(targetuuid);
        if (targetname == null) {
            targetname = strings[0];
        }
        PlayerInfo playerInfo = new PlayerInfo();
        playerInfo.setTargetName(targetname);
        playerInfo.setTargetuuid(targetuuid);
        ExecutorService executorServiceinfo = Executors.newSingleThreadExecutor();
        Future<Map<String, String>> futureInfo = executorServiceinfo.submit(playerInfo);
        List<String> notes = new ArrayList<>();
//        if (PunisherPlugin.playerInfoConfig.contains(targetuuid + ".notes")){
//            notes = PunisherPlugin.playerInfoConfig.getStringList(targetuuid + ".notes");
//        }
        ExecutorService executorService1;
        Future<BaseComponent[]> futurestatus;
        Status statusClass = new Status();
        statusClass.setTargetuuid(targetuuid);
        executorService1 = Executors.newSingleThreadExecutor();
        futurestatus = executorService1.submit(statusClass);
        Map<String, String> info;
        try{
            info = futureInfo.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
            DataFetchException dfe = new DataFetchException(this.getName(), "Player Info", strings[0], e, "Player Info Required for next step");
            errorHandler.log(dfe);
            errorHandler.alert(dfe, commandSender);
            executorServiceinfo.shutdown();
            return;
        }
        executorServiceinfo.shutdown();
        BaseComponent[] status;
        try{
            status = futurestatus.get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
            DataFetchException dfe = new DataFetchException(this.getName(), "Punishment Status", targetname, e, "Status was required for Player Info");
            errorHandler.log(dfe);
            errorHandler.alert(dfe, commandSender);
            executorService1.shutdown();
            return;
        }
        executorService1.shutdown();
        player.sendMessage(new ComponentBuilder("|--------").strikethrough(true).color(ChatColor.RED).append(targetname + "'s Player Info").strikethrough(false).color(ChatColor.GREEN).append("--------|").strikethrough(true).color(ChatColor.RED).create());
        player.sendMessage(new ComponentBuilder("UUID: ").color(ChatColor.RED).append(info.get("uuid")).color(ChatColor.GREEN).create());
        if (info.containsKey("prefix"))
            player.sendMessage(new ComponentBuilder("Rank: ").color(ChatColor.RED).append(ChatColor.translateAlternateColorCodes('&', info.get("prefix"))).create());
        player.sendMessage(status);
        if (info.containsKey("alts") && player.hasPermission("punisher.alts"))
            player.sendMessage(new ComponentBuilder("Alts: ").color(ChatColor.RED).append(info.get("alts")).color(ChatColor.GREEN).create());
        if (info.containsKey("ip") && player.hasPermission("punisher.alts.ip"))
            player.sendMessage(new ComponentBuilder("Ip: ").color(ChatColor.RED).append(info.get("ip")).color(ChatColor.GREEN).create());
        if (info.containsKey("firstjoin"))
            player.sendMessage(new ComponentBuilder("First Join Date: ").color(ChatColor.RED).append(info.get("firstjoin")).color(ChatColor.GREEN).create());
        if (info.containsKey("lastlogin"))
            player.sendMessage(new ComponentBuilder("Last Login: ").color(ChatColor.RED).append(info.get("lastlogin")).color(ChatColor.GREEN).create());
        if (info.containsKey("lastlogout"))
            player.sendMessage(new ComponentBuilder("Last Logout: ").color(ChatColor.RED).append(info.get("lastlogout")).color(ChatColor.GREEN).create());
        if (info.containsKey("lastserver"))
            player.sendMessage(new ComponentBuilder("Last Server Played: ").color(ChatColor.RED).append(info.get("lastserver")).color(ChatColor.GREEN).create());
        if (info.containsKey("reputation"))
            player.sendMessage(new ComponentBuilder("Reputation: ").color(ChatColor.RED).append(info.get("reputation")).color(ChatColor.GREEN).create());
        if (info.containsKey("punishmentsreceived") && player.hasPermission("punisher.history"))
            player.sendMessage(new ComponentBuilder("Punishments Received: ").color(ChatColor.RED).append(info.get("punishmentsreceived")).color(ChatColor.GREEN).create());
        if (info.containsKey("punishmentsgiven") && player.hasPermission("punisher.staffhistory"))
            player.sendMessage(new ComponentBuilder("Punishments Given: ").color(ChatColor.RED).append(info.get("punishmentsgiven")).color(ChatColor.GREEN).create());
        if (info.containsKey("punishlevel"))
            player.sendMessage(new ComponentBuilder("Punish Permission level: ").color(ChatColor.RED).append(info.get("punishlevel")).color(ChatColor.GREEN).create());
        if (info.containsKey("punishmentbypass"))
            player.sendMessage(new ComponentBuilder("Can Bypass Punishments: ").color(ChatColor.RED).append(info.get("punishmentbypass")).create());
        if (!notes.isEmpty()) {
            player.sendMessage(new ComponentBuilder("Notes for " + targetname + ":").color(ChatColor.RED).create());
            for (String note : notes) {
                player.sendMessage(new ComponentBuilder("    " + (notes.indexOf(note) + 1) + ". ").color(ChatColor.GREEN).append(note).color(ChatColor.GREEN).create());
            }
        }else{
            player.sendMessage(new ComponentBuilder("No available notes for " + targetname).color(ChatColor.RED).create());
        }
    }
}
