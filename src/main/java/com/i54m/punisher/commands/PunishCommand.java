package com.i54m.punisher.commands;

import com.i54m.punisher.objects.GUIS;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PunishCommand extends Command {

    public PunishCommand() {
        super("punish", "punisher.punish", "pun");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length >= 1) {
                // TODO: 17/11/2020 begin work on punish command
                /*
                fetch uuid
                then
                fetch rep & correct name
                then
                decide gui based on permission lvl
                then
                open correct gui
                 */
                GUIS.openMenu(GUIS.GUI.valueOf(strings[0].toUpperCase()), player, player.getUniqueId(), player.getName(), "(0/0)");
            }
        }
    }
}
