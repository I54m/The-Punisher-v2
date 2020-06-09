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

import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ReputationCommand extends Command {

    private String uuid;
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    public ReputationCommand() {
        super("reputation", "punisher.reputation", "rep");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length <= 2) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Add/Minus/Set a player's reputation score").color(ChatColor.RED).append("\nUsage: /reputation <player> <add|minus|set> <amount>").color(ChatColor.WHITE).create());
            return;
        }
        ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
        Future<String> future = null;
        ExecutorService executorService = null;
        if (findTarget != null){
            uuid = findTarget.getUniqueId().toString().replace("-", "");
        }else {
            UUIDFetcher uuidFetcher = new UUIDFetcher();
            uuidFetcher.fetch(strings[0]);
            executorService = Executors.newSingleThreadExecutor();
            future = executorService.submit(uuidFetcher);
        }
        if (future != null) {
            try {
                uuid = future.get(1, TimeUnit.SECONDS);
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
        if (uuid == null) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name!").color(ChatColor.RED).create());
            return;
        }
        String name = NameFetcher.getName(uuid);
        if (name == null){
            name = strings[0];
        }
        if (strings[1].equalsIgnoreCase("add")) {
            try {
                double amount = Double.parseDouble(strings[2]);
                ReputationManager.addRep(name, uuid, amount);
                String currentRep = ReputationManager.getRep(uuid);
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Added: " + new DecimalFormat("##.##").format(amount) + " to: " + name + "'s reputation").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("New Reputation: " + new DecimalFormat("##.##").format(currentRep)).color(ChatColor.RED).create());
            }catch (NumberFormatException e){
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a valid amount!").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Add/Minus/Set a player's reputation score").color(ChatColor.RED).append("\nUsage: /reputation <player> <add|minus|set> <amount>").color(ChatColor.WHITE).create());
            }
        } else if (strings[1].equalsIgnoreCase("minus")) {
            try {
                double amount = Double.parseDouble(strings[2]);
                ReputationManager.minusRep(name, uuid, amount);
                String currentRep = ReputationManager.getRep(uuid);
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Removed: " + new DecimalFormat("##.##").format(amount) + " from: " + name + "'s reputation").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("New Reputation: " + new DecimalFormat("##.##").format(currentRep)).color(ChatColor.RED).create());
            }catch (NumberFormatException e){
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a valid amount!").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Add/Minus/Set a player's reputation score").color(ChatColor.RED).append("\nUsage: /reputation <player> <add|minus|set> <amount>").color(ChatColor.WHITE).create());
            }
        } else if (strings[1].equalsIgnoreCase("set")) {
            try {
                double amount = Double.parseDouble(strings[2]);
                ReputationManager.setRep(name, uuid, amount);
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Set: " + name + "'s reputation to: " + new DecimalFormat("##.##").format(amount)).color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("New Reputation: " + new DecimalFormat("##.##").format(amount)).color(ChatColor.RED).create());
            } catch (NumberFormatException e) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a valid amount!").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Add/Minus/Set a player's reputation score").color(ChatColor.RED).append("\nUsage: /reputation <player> <add|minus|set> <amount>").color(ChatColor.WHITE).create());
            }
        } else {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Add/Minus/Set a player's reputation score").color(ChatColor.RED).append("\nUsage: /reputation <player> <add|minus|set> <amount>").color(ChatColor.WHITE).create());
        }
    }

}
