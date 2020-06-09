package com.i54mpenguin.punisher.commands;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.DataFecthException;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.objects.Punishment;
import com.i54mpenguin.punisher.utils.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;

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
            player.sendMessage(new ComponentBuilder("That is not a player's name!").color(ChatColor.RED).create());
            return;
        }
        String targetuuid = target.getUniqueId().toString().replace("-", "");
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
            Punishment warn = new Punishment(Punishment.Type.WARN, Punishment.Reason.Custom, null, targetuuid, target.getName(), player.getUniqueId().toString().replace("-", ""), sb.toString());
            punishMnger.issue(warn, player, true, true, true);
        } catch (SQLException e) {
            try {
                throw new PunishmentsDatabaseException("Issuing warn on a player", target.getName(), this.getName(), e, "/warn", strings);
            } catch (PunishmentsDatabaseException pde) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(pde);
                errorHandler.alert(pde, commandSender);
            }
        }
    }
}