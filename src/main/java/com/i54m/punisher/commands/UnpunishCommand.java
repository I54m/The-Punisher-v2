package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class UnpunishCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private UUID targetuuid;
    private final PunishmentManager punishmentManager = PunishmentManager.getINSTANCE();

    public UnpunishCommand() {
        super("unpunish", "punisher.unpunish", "unpun");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Unpunish command is currently not functioning properly!").color(ChatColor.RED).create());
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length <= 1) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Remove a punishment from a player's history (CaSe SeNsItIvE!!)").color(ChatColor.RED).append("\nUsage: /unpunish <player name> <reason>").color(ChatColor.WHITE).create());
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
            StringBuilder reason = new StringBuilder();
            for (int i = 1; i < strings.length; i++) {
                reason.append(strings[i]);
                if (i + 1 < strings.length) reason.append(" ");
            }
            String reasonString = reason.toString().replace("-", "_").replace(" ", "_").replace("/", "_");
            if (reasonString.toLowerCase().contains("manual")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Manual Punishments may not be removed from history!").color(ChatColor.RED).create());
                return;
            }
            ArrayList<String> reasonslist = new ArrayList<>();
            StringBuilder reasons = new StringBuilder();
//            for (Punishment.Reason reason1 : Punishment.Reason.values()) {
//                if (!reason1.toString().contains("Manual")) {
//                    reasons.append(reason1.toString()).append(" ");
//                }
//                reasonslist.add(reason1.toString());
//            }
            if (!reasonslist.contains(reasonString)) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a punishment reason!").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Reasons are as follows (Case Sensitive):").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(reasons.toString()).color(ChatColor.RED).create());
                return;
            }
            if (future != null) {
                try {
                    targetuuid = future.get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    try {
                        throw new DataFecthException("UUID Required for next step", strings[0], "UUID", this.getName(), e);
                    } catch (DataFecthException dfe) {
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
                try {
//                    Punishment.Reason Reason = Punishment.Reason.valueOf(reasonString);
                    //todo NEED to find out type & issuedate third or maybe just get them to give an id?
                    punishmentManager.remove(punishmentManager.punishmentLookup(null, Punishment.Type.ALL, "Reason", "test", (long) 123456789, targetuuid), player, true, true, true);
                } catch (SQLException e) {
                    try {
                        throw new PunishmentsDatabaseException("Unpunishing a player", targetname, this.getName(), e, "/unpunish", strings);
                    } catch (PunishmentsDatabaseException pde) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pde);
                        errorHandler.alert(pde, commandSender);
                    }
                }
            } else {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name").color(ChatColor.RED).create());
            }
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }
}