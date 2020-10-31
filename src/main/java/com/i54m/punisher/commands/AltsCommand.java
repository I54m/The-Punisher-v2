package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AltsCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
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
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Command not yet available!").color(ChatColor.RED).create());
                        return;
                    } else if (strings[0].equalsIgnoreCase("get")) {
                        ExecutorService executorService1;
                        Future<BaseComponent[]> futurestatus;
                        Status statusClass = new Status();
                        statusClass.setTargetuuid(targetuuid);
                        executorService1 = Executors.newSingleThreadExecutor();
                        futurestatus = executorService1.submit(statusClass);
                        ArrayList<UUID> altslist = plugin.getStorageManager().getAlts(targetuuid);
                        if (altslist.isEmpty()) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player has no connected accounts!").color(ChatColor.RED).create());
                            return;
                        }
                        String ip = plugin.getStorageManager().getIpHist(targetuuid).firstEntry().getValue();
                        StringBuilder alts = new StringBuilder();
                        for (UUID alt : altslist) {
                            String concacc = NameFetcher.getName(alt);
                            if (concacc != null && !concacc.equals(targetname))
                                alts.append(ChatColor.RED).append(concacc).append(" ");
                        }
                        if (player.hasPermission("punisher.alts.ip")) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Accounts connected to " + targetname + " on the ip " + ip + ": ").color(ChatColor.RED).create());
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Accounts connected to " + targetname + ": ").color(ChatColor.RED).create());
                        }
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(alts.toString()).color(ChatColor.RED).create());
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
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Check a players alt or reset their stored ip").color(ChatColor.RED).append("\nUsage: /alts <reset|get> <player>").color(ChatColor.WHITE).create());
                    }
                } else {
                    if (strings[1].contains(".") && player.hasPermission("punisher.alts.ip")) {
                        ExecutorService executorService1;
                        Future<BaseComponent[]> futurestatus;
                        Status statusClass = new Status();
                        statusClass.setTargetuuid(targetuuid);
                        executorService1 = Executors.newSingleThreadExecutor();
                        futurestatus = executorService1.submit(statusClass);
                        ArrayList<UUID> altslist = plugin.getStorageManager().getAlts(strings[1]);
                        if (altslist.isEmpty()) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player has no connected accounts!").color(ChatColor.RED).create());
                            return;
                        }
                        targetname = NameFetcher.getName(altslist.get(0));
                        String ip = plugin.getStorageManager().getIpHist(targetuuid).firstEntry().getValue();
                        StringBuilder alts = new StringBuilder();
                        for (UUID alt : altslist) {
                            String concacc = NameFetcher.getName(alt);
                            if (concacc != null && !concacc.equals(targetname))
                                alts.append(ChatColor.RED).append(concacc).append(" ");
                        }
                        if (altslist.toString().isEmpty()) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player has no connected accounts!").color(ChatColor.RED).create());
                            return;
                        }
                        if (player.hasPermission("punisher.alts.ip")) {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Accounts connected to " + targetname + " on the ip " + ip + ": ").color(ChatColor.RED).create());
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Accounts connected to " + targetname + ": ").color(ChatColor.RED).create());
                        }
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(alts.toString()).color(ChatColor.RED).create());
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
                    } else if (!player.hasPermission("punisher.alts.ip")) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You do not have permission to do that!").color(ChatColor.RED).create());
                    } else {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not an ip or a player's name! (ips must contain the dots and not have a port number)").color(ChatColor.RED).create());
                    }
                }
            } else {
                commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            }
        } catch (Exception e) {
            try {
                throw new PunishmentsStorageException("Alts command (/alts" + strings[0] + strings[1] + ")", targetname, this.getName(), e);
            } catch (PunishmentsStorageException pde) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(pde);
                errorHandler.alert(pde, commandSender);
            }
        }
    }
}
