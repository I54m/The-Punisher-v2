package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.ReputationManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.GUIS;
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

public class PunishCommand extends Command {

    private final PunisherPlugin PLUGIN = PunisherPlugin.getInstance();
    private final ReputationManager REPUTATION_MANAGER = ReputationManager.getINSTANCE();

    public PunishCommand() {
        super("punish", "punisher.punish", "pun");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length == 0) {
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Please provide a player's name!").color(ChatColor.RED).create());
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Use automatic punishment calculation to punish a player").color(ChatColor.RED).append("\nUsage: /punish <player>").color(ChatColor.WHITE).create());
                return;
            }
            UUID targetUUID = null;
            String targetName;
            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
            Future<UUID> future = null;
            ExecutorService executorService = null;
            if (findTarget != null) {
                targetUUID = findTarget.getUniqueId();
            } else {
                UUIDFetcher uuidFetcher = new UUIDFetcher();
                uuidFetcher.fetch(strings[0]);
                executorService = Executors.newSingleThreadExecutor();
                future = executorService.submit(uuidFetcher);
            }
            if (future != null) {
                try {
                    targetUUID = future.get(1, TimeUnit.SECONDS);
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
            if (targetUUID == null) {
                player.sendMessage(new ComponentBuilder("That is not a player's name!").color(ChatColor.RED).create());
                return;
            }
            final UUID finalTargetUUID = targetUUID;
            WorkerManager.getINSTANCE().runWorker(new WorkerManager.Worker(() -> {
                try {
                    PLUGIN.getStorageManager().loadUser(finalTargetUUID, true);
                } catch (Exception e) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(e);
                    errorHandler.alert(e, commandSender);
                }
            }));
            String reputation = REPUTATION_MANAGER.getFormattedRep(targetUUID);

            if (findTarget == null) {
                targetName = NameFetcher.getName(targetUUID);
                if (targetName == null) {
                    targetName = strings[0];
                }
            } else {
                targetName = findTarget.getName();
            }
            GUIS.GUI gui = GUIS.GUI.LEVEL_ZERO;
            try {
                if (!Permissions.higher(player, targetUUID)) {
                    player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot punish that player!").color(ChatColor.RED).create());
                    return;
                }
                // TODO: 17/01/2021 add this to other punishment classes
                if (PunishmentManager.getINSTANCE().isLocked(targetUUID))
                    player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Punishing " + targetName + " is currently locked! Please try again later! (perhaps they already have a pending punishment?)").color(ChatColor.RED).create());
                switch (Permissions.getPermissionLvl(player)) {
                    default: {
                        break;
                    }
                    case (0): {
                        gui = GUIS.GUI.LEVEL_ZERO;
                        break;
                    }
                    case (1): {
                        gui = GUIS.GUI.LEVEL_ONE;
                        break;
                    }
                    case (2): {
                        gui = GUIS.GUI.LEVEL_TWO;
                        break;
                    }
                    case (3): {
                        gui = GUIS.GUI.LEVEL_THREE;
                        break;
                    }
                }
            } catch (Exception e) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                DataFetchException dfe = new DataFetchException(Permissions.class.getName(), "User Instance", player.getName(), e, "User instance required for punishment level checking");
                errorHandler.log(dfe);
                errorHandler.alert(dfe, commandSender);
            }
            GUIS.openMenu(gui, player, targetUUID, targetName, reputation);
        } else commandSender.sendMessage(new TextComponent("You must be a player in order to use this command!"));
    }
}
