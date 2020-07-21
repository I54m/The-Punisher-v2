package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.fetchers.Status;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.storage.DatabaseManager;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AltsCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();
    private UUID targetuuid;

    public AltsCommand() {
        super("alts", "punisher.alts", "alt", "altsearch");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        String targetname = "NOT SET";
        try {
            if (commandSender instanceof ProxiedPlayer) {
                ProxiedPlayer player = (ProxiedPlayer) commandSender;
                if (strings.length < 2) {
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Check a players alt or reset their stored ip").color(ChatColor.RED).append("\nUsage: /alts <reset|get> <player>").color(ChatColor.WHITE).create());
                    return;
                }
                if (!strings[1].contains(".")) {
                    ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[1]);
                    Future<UUID> future = null;
                    ExecutorService executorService = null;
                    if (findTarget != null) {
                        targetuuid = findTarget.getUniqueId();
                    } else {
                        UUIDFetcher uuidFetcher = new UUIDFetcher();
                        uuidFetcher.fetch(strings[1]);
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
                    if (targetuuid == null) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name!").color(ChatColor.RED).create());
                        return;
                    }
                    targetname = NameFetcher.getName(targetuuid);
                    if (targetname == null) {
                        targetname = strings[1];
                    }
                    if (strings[0].equalsIgnoreCase("reset") && player.hasPermission("punisher.alts.reset")) {
                        String sql = "SELECT * FROM `altlist` WHERE UUID='" + targetuuid + "'";
                        PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
                        ResultSet results = stmt.executeQuery();
                        if (results.next()) {
                            String sql1 = "DELETE FROM `altlist` WHERE `UUID`='" + targetuuid + "' ;";
                            PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                            stmt1.executeUpdate();
                            stmt1.close();
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + "'s stored ip address has been reset!").color(ChatColor.RED).create());
                            PunisherPlugin.getLOGS().info(player.getName() + " reset " + targetname + "'s stored ip address");
                            ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetuuid);
                            if (target != null) {
                                String sql2 = "INSERT INTO `altlist` (`UUID`, `ip`) VALUES ('" + targetuuid + "', '" + target.getAddress().getHostString() + "');";
                                PreparedStatement stmt2 = dbManager.connection.prepareStatement(sql2);
                                stmt2.executeUpdate();
                                stmt2.close();
                            }
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player has no stored ip address!").color(ChatColor.RED).create());
                        }
                        stmt.close();
                        results.close();
                    } else if (strings[0].equalsIgnoreCase("get")) {
                        ExecutorService executorService1;
                        Future<BaseComponent[]> futurestatus;
                        Status statusClass = new Status();
                        statusClass.setTargetuuid(targetuuid);
                        executorService1 = Executors.newSingleThreadExecutor();
                        futurestatus = executorService1.submit(statusClass);
                        StringBuilder altslist = new StringBuilder();
                        String sql = "SELECT * FROM `altlist` WHERE UUID='" + targetuuid + "'";
                        PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
                        ResultSet results = stmt.executeQuery();
                        if (results.next()) {
                            String ip = results.getString("ip");
                            String sql1 = "SELECT * FROM `altlist` WHERE ip='" + ip + "'";
                            PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                            ResultSet results1 = stmt1.executeQuery();
                            while (results1.next()) {
                                String concacc = NameFetcher.getName(results1.getString("uuid"));
                                if (concacc != null && !concacc.equals(targetname)) {
                                    altslist.append(ChatColor.RED).append(concacc).append(" ");
                                }
                            }
                            stmt1.close();
                            results1.close();
                            if (altslist.toString().isEmpty()) {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player has no connected accounts!").color(ChatColor.RED).create());
                                return;
                            }
                            if (player.hasPermission("punisher.alts.ip")) {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Accounts connected to " + targetname + " on the ip " + ip + ": ").color(ChatColor.RED).create());
                            } else {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Accounts connected to " + targetname + ": ").color(ChatColor.RED).create());
                            }
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(altslist.toString()).color(ChatColor.RED).create());
                            BaseComponent[] status;
                            try {
                                status = futurestatus.get(500, TimeUnit.MILLISECONDS);
                            } catch (Exception e) {
                                try {
                                    throw new DataFecthException("Status was required for alts check", targetname, "Punishment Status", this.getName(), e);
                                } catch (DataFecthException dfe) {
                                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                    errorHandler.log(dfe);
                                    errorHandler.alert(dfe, commandSender);
                                }
                                executorService1.shutdown();
                                return;
                            }
                            executorService1.shutdown();
                            player.sendMessage(status);
                            PunisherPlugin.getLOGS().info(player.getName() + " looked at " + targetname + "'s connected accounts, at the time of logging they were: " + altslist.toString());
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player has no connected accounts!").color(ChatColor.RED).create());
                        }
                        stmt.close();
                        results.close();
                    } else {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Check a players alt or reset their stored ip").color(ChatColor.RED).append("\nUsage: /alts <reset|get> <player>").color(ChatColor.WHITE).create());
                    }
                } else {
                    if (strings[1].contains(".") && player.hasPermission("punisher.alts.ip")) {
                        String ip = strings[1];
                        StringBuilder altslist = new StringBuilder();
                        String sql = "SELECT * FROM `altlist` WHERE ip='" + ip + "'";
                        PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
                        ResultSet results = stmt.executeQuery();
                        while (results.next()) {
                            String concacc = NameFetcher.getName(results.getString("uuid"));
                            altslist.append(ChatColor.RED).append(concacc).append(" ");
                        }
                        stmt.close();
                        results.close();
                        if (altslist.toString().isEmpty()) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That ip is not stored in our database!").color(ChatColor.RED).create());
                            return;
                        }
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Accounts connected to the ip: " + ip + ": ").color(ChatColor.RED).create());
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(altslist.toString()).color(ChatColor.RED).create());
                        PunisherPlugin.getLOGS().info(player.getName() + " Looked at accounts connected to ip: " + ip + " Accounts connected at time of logging: " + altslist.toString());
                    } else if (!player.hasPermission("punisher.alts.ip")) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You do not have permission to do that!").color(ChatColor.RED).create());
                    } else {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not an ip or a player's name! (ips must contain the dots and not have a port number)").color(ChatColor.RED).create());
                    }
                }
            } else {
                commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            }
        } catch (SQLException e) {
            try {
                throw new PunishmentsDatabaseException("Alts command (/alts" + strings[0] + strings[1] + ")", targetname, this.getName(), e);
            } catch (PunishmentsDatabaseException pde) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(pde);
                errorHandler.alert(pde, commandSender);
            }
        }
    }
}
