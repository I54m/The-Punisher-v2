package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.WorkerManager;
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

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MuteCommand extends Command {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punishMnger = PunishmentManager.getINSTANCE();
    private long length;
    private UUID targetuuid;
    private String targetname;

    public MuteCommand() {
        super("mute", "punisher.mute", "tempmute");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length == 0) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Mute a player from speaking").color(ChatColor.RED).append("\nUsage: /mute <player> [length<s|m|h|d|w|M|perm>] [reason]").color(ChatColor.WHITE).create());
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
        //mute player time reason
        //mute player reason
        //mute player time
        //mute player


        boolean duration = false;
        try {
            if (strings.length >= 2) {
                length = punishMnger.translateExpiration(strings[1]);
                duration = !(length <= 0);
            }
        } catch (NumberFormatException e) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not a valid duration!").color(ChatColor.RED).create());
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Mute a player from speaking").color(ChatColor.RED).append("\nUsage: /mute <player> [length<s|m|h|d|w|M|perm>] [reason]").color(ChatColor.WHITE).create());
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
            reason.append("Manually Muted");
        if (future != null && targetuuid == null) {
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
        if (targetuuid == null) {
            player.sendMessage(new ComponentBuilder("That is not a player's name!").color(ChatColor.RED).create());
            return;
        }
        WorkerManager.getINSTANCE().runWorker(new WorkerManager.Worker(() -> {
            try {
                plugin.getStorageManager().loadUser(targetuuid, true);
            } catch (Exception e) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(e);
                errorHandler.alert(e, commandSender);
            }
        }));
        targetname = NameFetcher.getName(targetuuid);
        if (targetname == null) {
            targetname = strings[0];
        }
        try {
            if (!Permissions.higher(player, targetuuid)) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot punish that player!").color(ChatColor.RED).create());
                return;
            }
        } catch (Exception e) {
            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
            DataFetchException dfe = new DataFetchException(Permissions.class.getName(), "User Instance", player.getName(), e, "User instance required for punishment level checking");
            errorHandler.log(dfe);
            errorHandler.alert(dfe, commandSender);
        }
        try {
            Punishment mute = new Punishment(Punishment.Type.MUTE, "MANUAL", length, targetuuid, targetname, player.getUniqueId(),null, reason.toString(), new Punishment.MetaData());
            punishMnger.issue(mute, player, true, true, true);
        } catch (Exception e) {
            PunishmentsStorageException pse = new PunishmentsStorageException("Issuing mute on a player", targetname, this.getName(), e, "/mute", strings);
            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
            errorHandler.log(pse);
            errorHandler.alert(pse, commandSender);
        }
    }
}
