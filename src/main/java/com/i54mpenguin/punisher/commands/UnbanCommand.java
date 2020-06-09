package com.i54mpenguin.punisher.commands;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.DataFecthException;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.utils.NameFetcher;
import com.i54mpenguin.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class UnbanCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private String targetuuid;
    private final PunishmentManager punishMnger = PunishmentManager.getINSTANCE();

    public UnbanCommand() {
        super("unban", "punisher.unban", "pardon");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length == 0) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Unban a player").color(ChatColor.RED).append("\nUsage: /unban <player name>").color(ChatColor.WHITE).create());
                return;
            }
            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
            Future<String> future = null;
            ExecutorService executorService = null;
            if (findTarget != null) {
                targetuuid = findTarget.getUniqueId().toString().replace("-", "");
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
                    if (punishMnger.isBanned(targetuuid)) {
                        punishMnger.remove(punishMnger.getBan(targetuuid), player, true, false, true);
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Successfully unbanned " + targetname).color(ChatColor.GREEN).create());
                    } else {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + " is not currently banned!").color(ChatColor.RED).create());
                    }
                } catch (SQLException e) {
                    try {
                        throw new PunishmentsDatabaseException("Unbanning a player", targetname, this.getName(), e, "/unban", strings);
                    } catch (PunishmentsDatabaseException pde) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pde);
                        errorHandler.alert(pde, commandSender);
                    }
                }
            } else {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name!").color(ChatColor.RED).create());
            }
        } else {
            if (strings.length == 0) {
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Unban a player").color(ChatColor.RED).append("\nUsage: /unban <player name>").color(ChatColor.WHITE).create());
                return;
            }
            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
            Future<String> future = null;
            ExecutorService executorService = null;
            if (findTarget != null) {
                targetuuid = findTarget.getUniqueId().toString().replace("-", "");
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
                    if (punishMnger.isBanned(targetuuid)) {
                        punishMnger.remove(punishMnger.getBan(targetuuid), null, true, false, true);
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Successfully unbanned " + targetname).color(ChatColor.GREEN).create());
                    } else {
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + " is not currently banned!").color(ChatColor.RED).create());
                    }
                } catch (SQLException e) {
                    try {
                        throw new PunishmentsDatabaseException("Unbanning a player", targetname, this.getName(), e, "/unban", strings);
                    } catch (PunishmentsDatabaseException pde) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pde);
                        errorHandler.alert(pde, commandSender);
                    }
                }
            } else {
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name!").color(ChatColor.RED).create());
            }
        }
    }
}