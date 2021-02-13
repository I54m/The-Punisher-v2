package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.storage.StorageManager;
import com.i54m.punisher.objects.Punishment;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PunishmentCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punishmentManager = PunishmentManager.getINSTANCE();
    private final StorageManager storageManager = plugin.getStorageManager();

    public PunishmentCommand() {
        super("punishment", "punisher.admin", "pun");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("The punishment command is currently a work in progress, Proceed with caution!").color(ChatColor.RED).create());
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length <= 2) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punishment revert <id>").color(ChatColor.RED).append(" - Revert a punishment and it's effects").create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punishment lookup ").color(ChatColor.RED).append(" - ").create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punishment lock <id>").color(ChatColor.RED).append(" - Lock a punishment from any actions").create());
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punishment unlock <id>").color(ChatColor.RED).append(" - Remove a Lock on a punishment").create());
// TODO: 13/02/2021 /punishment reissue <id>
                return;
            }
            switch (strings[0].toLowerCase()) {

                case "revert": {
                    int id;
                    try {
                        id = Integer.parseInt(strings[1]);
                        if (id < 1) throw new NumberFormatException("That is not a valid id");
                        if (id > storageManager.getLastID()) throw new NumberFormatException("That is not a valid id");
                    } catch (NumberFormatException nfe) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not a valid id!").color(ChatColor.RED).create());
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punishment revert <id>").color(ChatColor.RED).append(" - Revert a punishment and it's effects").create());
                        return;
                    }
                    try {
                        punishmentManager.revert(storageManager.getPunishmentFromId(id), player, true);
                    } catch (PunishmentsStorageException pse) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.alert(pse, commandSender);
                        errorHandler.log(pse);
                    }
                    return;
                }
                case "lookup": {
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This command is not yet implemented!").color(ChatColor.RED).create());
                    return;
                }
                case "lock": {
                    int id;
                    try {
                        id = Integer.parseInt(strings[1]);
                        if (id < 1) throw new NumberFormatException("That is not a valid id");
                        if (id > storageManager.getLastID()) throw new NumberFormatException("That is not a valid id");
                    } catch (NumberFormatException nfe) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not a valid id!").color(ChatColor.RED).create());
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punishment lock <id>").color(ChatColor.RED).append(" - Lock a punishment from any actions").create());
                        return;
                    }
                    try {
                        Punishment punishment = storageManager.getPunishmentFromId(id);
                        punishment.getMetaData().setLocked(true);
                        storageManager.updatePunishment(punishment);
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Locked punishment on " + punishment.getTargetName() + " with id " + strings[1]).color(ChatColor.GREEN).event(punishment.getHoverEvent()).create());
                    } catch (PunishmentsStorageException pse) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.alert(pse, commandSender);
                        errorHandler.log(pse);
                    }
                    return;
                }
                case "unlock": {
                    int id;
                    try {
                        id = Integer.parseInt(strings[1]);
                        if (id < 1) throw new NumberFormatException("That is not a valid id");
                        if (id > storageManager.getLastID()) throw new NumberFormatException("That is not a valid id");
                    } catch (NumberFormatException nfe) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[1] + " is not a valid id!").color(ChatColor.RED).create());
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punishment unlock <id>").color(ChatColor.RED).append(" - Remove a Lock on a punishment").create());
                        return;
                    }
                    try {
                        Punishment punishment = storageManager.getPunishmentFromId(id);
                        punishment.getMetaData().setLocked(false);
                        storageManager.updatePunishment(punishment);
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Unlocked punishment on " + punishment.getTargetName() + " with id " + strings[1]).color(ChatColor.GREEN).event(punishment.getHoverEvent()).create());
                    } catch (PunishmentsStorageException pse) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.alert(pse, commandSender);
                        errorHandler.log(pse);
                    }
                    return;
                }
            }


        } else
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
    }
}