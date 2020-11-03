package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.fetchers.Status;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HistoryCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private UUID targetuuid;

    public HistoryCommand() {
        super("history", "punisher.history", "hist");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length == 0) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("View a player's punishment history").color(ChatColor.RED).append("\nUsage: /history <player name> [page no]").color(ChatColor.WHITE).create());
                return;
            }
            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
            Future<UUID> future = null;
            ExecutorService executorService = null;
            if (findTarget != null)
                targetuuid = findTarget.getUniqueId();
            else {
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
                        throw new DataFetchException("UUID Required for next step", strings[0], "UUID", this.getName(), e);
                    } catch (DataFetchException dfe) {
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
                try {
                    TreeMap<Integer, Punishment> history = plugin.getStorageManager().getHistory(targetuuid);
                    String targetname = NameFetcher.getName(targetuuid);
                    if (!history.isEmpty()) {
                        ExecutorService executorService1;
                        Future<BaseComponent[]> futurestatus;
                        Status statusClass = new Status();
                        statusClass.setTargetuuid(targetuuid);
                        executorService1 = Executors.newSingleThreadExecutor();
                        futurestatus = executorService1.submit(statusClass);
                        player.sendMessage(new ComponentBuilder("|-------------").color(ChatColor.GREEN).strikethrough(true).append("History for: " + targetname).color(ChatColor.RED).strikethrough(false)
                                .append("-------------|").color(ChatColor.GREEN).strikethrough(true).create());
                        int page = 1;
                        if (strings.length > 1) {
                            try {
                                page = Integer.parseInt(strings[1]);
                            } catch (NumberFormatException nfe) {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not a valid page number!").color(ChatColor.RED).create());
                                return;
                            }
                        }
                        ArrayList<BaseComponent[]> historymessage = new ArrayList<>();
                        for (Integer id : history.keySet()) {
                            Punishment punishment = history.get(id);
                            if (punishment.getStatus() != Punishment.Status.Created) {
                                BaseComponent[] type;
                                if (punishment.isBan())
                                    type = new ComponentBuilder("Banned").color(ChatColor.RED).create();
                                else if (punishment.isMute())
                                    type = new ComponentBuilder("Muted").color(ChatColor.GOLD).create();
                                else if (punishment.isKick())
                                    type = new ComponentBuilder("Kicked").color(ChatColor.YELLOW).create();
                                else if (punishment.isWarn())
                                    type = new ComponentBuilder("Warned").color(ChatColor.YELLOW).create();
                                else type = new ComponentBuilder(punishment.getType().toString()).create();
                                String reasonmessage;
                                if (punishment.getMessage() != null)
                                    reasonmessage = punishment.getMessage();
                                else
                                    reasonmessage = punishment.getReason();
                                String punishername = punishment.getPunisherUUID().equals(UUIDFetcher.getBLANK_UUID()) ? "CONSOLE" : NameFetcher.getName(punishment.getPunisherUUID());
                                HoverEvent hoverEvent = punishment.getHoverEvent();
                                BaseComponent[] message = new ComponentBuilder("#" + id).event(hoverEvent).append(" " + punishment.getIssueDate() + ": ").event(hoverEvent)
                                        .append(type).event(hoverEvent).append(" for " + reasonmessage + " by " + punishername).event(hoverEvent).create();
                                historymessage.add(message);
                            }
                        }
                        paginate(player, historymessage, page);
                        BaseComponent[] status;
                        try {
                            status = futurestatus.get(500, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            try {
                                throw new DataFetchException("Status was required for history check", targetname, "Punishment Status", this.getName(), e);
                            } catch (DataFetchException dfe) {
                                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                errorHandler.log(dfe);
                                errorHandler.alert(dfe, commandSender);
                            }
                            executorService1.shutdown();
                            return;
                        }
                        executorService1.shutdown();
                        player.sendMessage(status);
                    } else
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + " has not been punished yet!").color(ChatColor.RED).create());

                } catch (Exception e) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Unable to fetch player's history!").color(ChatColor.RED).create());
                    ErrorHandler.getINSTANCE().log(e);
                    return;
                }
            } else
                commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }

    public void paginate(ProxiedPlayer player, ArrayList<BaseComponent[]> list, int page) {
        int contentLinesPerPage = 5;
        int totalPageCount;
        int remainder = list.size() % contentLinesPerPage;
        if (remainder != 0)
            totalPageCount = (list.size() + (contentLinesPerPage - remainder)) / contentLinesPerPage;
        else
            totalPageCount = list.size() / contentLinesPerPage;

        if (page <= totalPageCount) {
            player.sendMessage(new ComponentBuilder("Page: " + page + "/" + totalPageCount).color(ChatColor.RED).create());
            page--;
            int i = (page * contentLinesPerPage) + 1;
            int start = i;
            for (BaseComponent[] message : list) {
                if (list.indexOf(message) + 1 >= i && !((i - start) >= contentLinesPerPage)) {
                    player.sendMessage(message);
                    i++;
                }
            }
        } else
            player.sendMessage(new ComponentBuilder("There are only " + totalPageCount + " pages!").color(ChatColor.RED).create());
    }
}
