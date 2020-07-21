package com.i54m.punisher.commands;

import com.google.gson.Gson;
import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.Permissions;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BanCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private long length;
    private String targetname;
    private UUID targetuuid;
    private final PunishmentManager punishMngr = PunishmentManager.getINSTANCE();

    public BanCommand() {
        super("ban", "punisher.ban", "tempban", "ipban", "banip");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length == 0) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Ban a player from the server").color(ChatColor.RED).append("\nUsage: /ban <player> [length<s|m|h|d|w|M|perm>] [reason]").color(ChatColor.WHITE).create());
            return;
        }
        if (targetname != null || targetuuid != null) {
            targetuuid = null;
            targetname = null;
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
        boolean duration = false;
        try {
            if (strings.length >= 2) {
                if (strings[1].toLowerCase().endsWith("perm"))
                    length = (long) 3.154e+12;
                else if (strings[1].endsWith("M"))
                    length = (long) 2.628e+9 * (long) Integer.parseInt(strings[1].replace("M", ""));
                else if (strings[1].toLowerCase().endsWith("w"))
                    length = (long) 6.048e+8 * (long) Integer.parseInt(strings[1].replace("w", ""));
                else if (strings[1].toLowerCase().endsWith("d"))
                    length = (long) 8.64e+7 * (long) Integer.parseInt(strings[1].replace("d", ""));
                else if (strings[1].toLowerCase().endsWith("h"))
                    length = (long) 3.6e+6 * (long) Integer.parseInt(strings[1].replace("h", ""));
                else if (strings[1].endsWith("m"))
                    length = 60000 * (long) Integer.parseInt(strings[1].replace("m", ""));
                else if (strings[1].toLowerCase().endsWith("s"))
                    length = 1000 * (long) Integer.parseInt(strings[1].replace("s", ""));

                if (strings[1].toLowerCase().endsWith("perm") || strings[1].toLowerCase().endsWith("w") || strings[1].toLowerCase().endsWith("d") ||
                        strings[1].toLowerCase().endsWith("h") || strings[1].toLowerCase().endsWith("m") || strings[1].toLowerCase().endsWith("s")) {
                    length += System.currentTimeMillis();
                    duration = true;
                } else {
                    duration = false;
                }
            }
        } catch (NumberFormatException e) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not a valid duration!").color(ChatColor.RED).create());
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Ban a player from the server").color(ChatColor.RED).append("\nUsage: /ban <player> [length<s|m|h|d|w|M|perm>] [reason]").color(ChatColor.WHITE).create());
            return;
        }
        StringBuilder reason = new StringBuilder();
        if (strings.length > 2 && duration) {
            for (int i = 2; i < strings.length; i++)
                reason.append(strings[i]).append(" ");
        } else if (!duration) {
            for (int i = 1; i < strings.length; i++)
                reason.append(strings[i]).append(" ");
            length = (long) 3.154e+12 + System.currentTimeMillis();
        } else if (reason.toString().isEmpty())
            reason.append("Manually Banned");
        String reasonString = reason.toString().replace("\"", "'");
        if (future != null && targetuuid == null) {
            Gson g = new Gson();// TODO: 22/04/2020 make sure bungee has a gson version
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
            player.sendMessage(new ComponentBuilder("That is not a player's name!").color(ChatColor.RED).create());
            return;
        }
        if (findTarget == null) {
            targetname = NameFetcher.getName(targetuuid);
            if (targetname == null) {
                targetname = strings[0];
            }
        } else {
            targetname = findTarget.getName();
        }
        try {
            if (!Permissions.higher(player, targetuuid)) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot punish that player!").color(ChatColor.RED).create());
                return;
            }
        } catch (Exception e) {
            try {
                throw new DataFecthException("User instance required for punishment level checking", player.getName(), "User Instance", Permissions.class.getName(), e);
            } catch (DataFecthException dfe) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(dfe);
                errorHandler.alert(e, player);
                return;
            }
        }
        try {
            Punishment ban = new Punishment(Punishment.Type.BAN, "CUSTOM", length, targetuuid, targetname, player.getUniqueId(), reasonString);
            punishMngr.issue(ban, player, true, true, true);
        } catch (SQLException e) {
            try {
                throw new PunishmentsDatabaseException("Issuing ban on a player ", targetname, this.getName(), e, "/ban", strings);
            } catch (PunishmentsDatabaseException pde) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(pde);
                errorHandler.alert(pde, commandSender);
            }
        }
    }
}