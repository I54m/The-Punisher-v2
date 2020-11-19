package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.ReputationManager;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class viewRepCommand extends Command {
    private UUID targetuuid;
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
            Future<UUID> future = null;
            ExecutorService executorService = null;
            if (findTarget != null){
                targetuuid = findTarget.getUniqueId();
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
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    DataFetchException dfe = new DataFetchException(this.getName(), "UUID", strings[0], e, "UUID Required for next step");
                    errorHandler.log(dfe);
                    errorHandler.alert(dfe, commandSender);
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
                String rep = ReputationManager.getINSTANCE().getFormattedRep(targetuuid);
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + "'s Reputation is: ").color(ChatColor.RED).appendLegacy(rep).create());
            } else {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name").color(ChatColor.RED).create());
            }
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }

}
