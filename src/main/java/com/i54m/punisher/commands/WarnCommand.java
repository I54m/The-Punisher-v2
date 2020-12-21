package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class WarnCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punishMnger = PunishmentManager.getINSTANCE();

    public WarnCommand() {
        super("warn", "punisher.warn");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length == 0) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Warn a player for breaking the rules").color(ChatColor.RED).append("\nUsage: /warn <player> [reason]").color(ChatColor.WHITE).create());
            return;
        }
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(strings[0]);
        if (target == null) {
            player.sendMessage(new ComponentBuilder("That is not an online player's name!").color(ChatColor.RED).create());
            return;
        }
        UUID targetuuid = target.getUniqueId();
        StringBuilder sb = new StringBuilder();
        if (strings.length == 1) {
            sb.append("Manually Warned");
        } else {
            for (int i = 1; i < strings.length; i++)
                sb.append(strings[i]).append(" ");
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
            Punishment warn = new Punishment(Punishment.Type.WARN, "MANUAL", null, targetuuid, target.getName(), player.getUniqueId(), null, sb.toString(), new Punishment.MetaData());
            punishMnger.issue(warn, player, true, true, true);
        } catch (Exception e) {
            PunishmentsStorageException pse = new PunishmentsStorageException("Issuing warn on a player", target.getName(), this.getName(), e, "/warn", strings);
            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
            errorHandler.log(pse);
            errorHandler.alert(pse, commandSender);
        }
    }
}