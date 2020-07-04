package com.i54m.punisher.listeners;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.ReputationManager;
import com.i54m.punisher.utils.UUIDFetcher;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class PlayerVote implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final ReputationManager reputationManager = ReputationManager.getINSTANCE();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVote(VotifierEvent event){
        Vote vote = event.getVote();
        String username = vote.getUsername();
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(username);
        if (player != null){
            reputationManager.addRep(player.getUniqueId(), plugin.getConfig().getDouble("Voting.amountOfRepToAdd", 0.1));
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Thanks for Voting, " + plugin.getConfig().getDouble("Voting.amountOfRepToAdd", 0.1) + " Reputation added!").color(ChatColor.GREEN).create());
        }else{
            UUIDFetcher uuidFetcher = new UUIDFetcher();
            uuidFetcher.fetch(username);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<UUID> future = executorService.submit(uuidFetcher);
            try {
                reputationManager.addRep(future.get(1, TimeUnit.SECONDS), plugin.getConfig().getDouble("Voting.amountOfRepToAdd", 0.1));
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
