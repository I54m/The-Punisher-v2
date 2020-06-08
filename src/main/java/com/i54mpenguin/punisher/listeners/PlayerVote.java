package com.i54mpenguin.punisher.listeners;

import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import me.fiftyfour.punisher.bungee.PunisherPlugin;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.ReputationManager;
import com.i54mpenguin.punisher.exceptions.DataFecthException;
import me.fiftyfour.punisher.universal.util.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class PlayerVote implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVote(VotifierEvent event){
        com.vexsoftware.votifier.model.Vote vote = event.getVote();
        String username = vote.getUsername();
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(username);
        if (player != null){
            ReputationManager.addRep(username, player.getUniqueId().toString().replace("-", ""), PunisherPlugin.config.getDouble("Voting.amountOfRepToAdd"));
            player.sendMessage(new ComponentBuilder(plugin.prefix).append("Thanks for Voting, " + PunisherPlugin.config.getDouble("Voting.amountOfRepToAdd") + " Reputation added!").color(ChatColor.GREEN).create());
        }else{
            UUIDFetcher uuidFetcher = new UUIDFetcher();
            uuidFetcher.fetch(username);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<String> future = executorService.submit(uuidFetcher);
            try {
                ReputationManager.addRep(username, future.get(1, TimeUnit.SECONDS), PunisherPlugin.config.getDouble("Voting.amountOfRepToAdd"));
            } catch (Exception e) {
                try {
                    throw new DataFecthException("UUID Required for next step", username, "UUID", PlayerVote.class.getName(), e);
                } catch (DataFecthException dfe) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(dfe);
                    errorHandler.adminChatAlert(dfe, plugin.getProxy().getConsole());
                }
                executorService.shutdown();
                return;
            }
            executorService.shutdown();
        }
    }
}
