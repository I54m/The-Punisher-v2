package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.objects.GUIS;
import com.i54m.punisher.objects.Punishment;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class AuthorizePunishmentCommand extends Command {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    public AuthorizePunishmentCommand() {
        super("authorizepunishment", "punisher.staff", "authpunishment", "authpun", "punauth", "pauth");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new ComponentBuilder("You must be a player to use this command!").color(ChatColor.RED).create());
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (!player.hasPermission("punisher.authorizer"))
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are not allowed to authorize punishments! Please talk to an admin+ if you believe this to be a mistake!").color(ChatColor.RED).create());
        else {
            if (strings.length <= 0) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Please provide a punishment id!\nUsage: /authorizepunishment <punishment id>").color(ChatColor.RED).create());
                return;
            }
            int id = -1;
            try {
                id = Integer.parseInt(strings[0]);
                if (id < 1) throw new NumberFormatException();
            } catch (NumberFormatException nfe) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(strings[0] + " is not a valid number for a punishment id!\nUsage: /authorizepunishment <punishment id>").color(ChatColor.RED).create());
                return;
            }
            try {
                Punishment punishment = plugin.getStorageManager().getPunishmentFromId(id);
                if (punishment.getStatus() != Punishment.Status.Awaiting_Authorization) {
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That punishment is not currently awaiting authorization! (maybe you mistyped the id?)").color(ChatColor.RED).create());
                    return;
                }
                GUIS.openAuthorizationGUI(punishment, player);
            } catch (PunishmentsStorageException pse) {
                ErrorHandler.getINSTANCE().log(pse);
                ErrorHandler.getINSTANCE().alert(pse, player);
            }
        }
    }
}
