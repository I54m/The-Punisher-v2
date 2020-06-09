package com.i54mpenguin.punisher.commands;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.DataFecthException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.ReputationManager;
import com.i54mpenguin.punisher.utils.NameFetcher;
import com.i54mpenguin.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class viewRepCommand extends Command {
    private String targetuuid;
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    public viewRepCommand() {
        super("viewrep", "punisher.viewrep", "vrep");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length == 0) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("View a player's reputation score").color(ChatColor.RED).append("\nUsage: /viewrep <player name>").color(ChatColor.WHITE).create());
                return;
            }
            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
            Future<String> future = null;
            ExecutorService executorService = null;
            if (findTarget != null){
                targetuuid = findTarget.getUniqueId().toString().replace("-", "");
            }else {
                UUIDFetcher uuidFetcher = new UUIDFetcher();
                uuidFetcher.fetch(strings[0]);
                executorService = Executors.newSingleThreadExecutor();
                future = executorService.submit(uuidFetcher);
            }
            if (future != null) {
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
            if (targetuuid != null) {
                String targetname = NameFetcher.getName(targetuuid);
                if (targetname == null) {
                    targetname = strings[0];
                }
                StringBuilder reputation = new StringBuilder();
                String rep = ReputationManager.getRep(targetuuid);
                if (!(rep == null)) {
                    double repDouble;
                    try {
                        repDouble = Double.parseDouble(rep);
                    }catch(NumberFormatException e){
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("An internal error occurred: That player's reputation is not a number! Reputation System returned: ").color(ChatColor.RED).append(rep).color(ChatColor.RED).create());
                        return;
                    }
                    if (repDouble == 5){
                        reputation.append(ChatColor.WHITE).append("(").append(rep).append("/10").append(")");
                    }else if (repDouble > 5){
                        reputation.append(ChatColor.GREEN).append("(").append(rep).append("/10").append(")");
                    }else if (repDouble < 5 && repDouble > -1){
                        reputation.append(ChatColor.YELLOW).append("(").append(rep).append("/10").append(")");
                    }else if (repDouble < -1 && repDouble > -8){
                        reputation.append(ChatColor.GOLD).append("(").append(rep).append("/10").append(")");
                    }else if (repDouble < -8){
                        reputation.append(ChatColor.RED).append("(").append(rep).append("/10").append(")");
                    }
                } else {
                    reputation.append(ChatColor.WHITE).append("(").append("-").append(ChatColor.WHITE).append(")");
                }
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + "'s Reputation is: ").color(ChatColor.RED).append(reputation.toString()).color(ChatColor.RED).create());
            } else {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name").color(ChatColor.RED).create());
            }
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }

}
