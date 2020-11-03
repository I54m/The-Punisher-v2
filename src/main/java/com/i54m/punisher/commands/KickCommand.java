package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
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

public class KickCommand extends Command {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punishmentManager = PunishmentManager.getINSTANCE();

    public KickCommand() {
        super("kick", "punisher.kick");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length < 1) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Kick a player from the server").color(ChatColor.RED).append("\nUsage: /kick <player name> [custom meesage]").color(ChatColor.WHITE).create());
                return;
            }
            ProxiedPlayer target = ProxyServer.getInstance().getPlayer(strings[0]);
            if (target == null) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not an online player's name!").color(ChatColor.RED).create());
                return;
            }
            try {
                if (!Permissions.higher(player, target.getUniqueId())) {
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot punish that player!").color(ChatColor.RED).create());
                    return;
                }
            } catch (Exception e) {
                try {
                    throw new DataFetchException("User instance required for punishment level checking", player.getName(), "User Instance", Permissions.class.getName(), e);
                } catch (DataFetchException dfe) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(dfe);
                    errorHandler.alert(e, player);
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            if (strings.length == 1) {
                Punishment kick = new Punishment(
                        Punishment.Type.KICK,
                        "CUSTOM",
                        null,
                        target.getUniqueId(),
                        target.getName(),
                        player.getUniqueId(),
                        ChatColor.RED + "You have been Kicked from the server!" +
                                "\nYou were kicked for the reason: Manually Kicked!" +
                                "\nYou may reconnect at anytime, but make sure to read the /rules!");
                try {
                    punishmentManager.issue(kick, player, true, true, true);
                } catch (Exception e) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(e);
                    errorHandler.alert(e, commandSender);
                }
            } else {
                for (int i = 1; i < strings.length; i++)
                    sb.append(strings[i]).append(" ");
                Punishment kick = new Punishment(
                        Punishment.Type.KICK,
                        "CUSTOM",
                        null,
                        target.getUniqueId(),
                        target.getName(),
                        player.getUniqueId(),
                        ChatColor.translateAlternateColorCodes('&', sb.toString()));
                try {
                    punishmentManager.issue(kick, player, true, true, true);
                } catch (Exception e) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(e);
                    errorHandler.alert(e, commandSender);
                }
            }
        } else
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
    }
}