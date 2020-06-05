package com.i54mpenguin.punisher.commands;

import com.i54mpenguin.protocol.items.ItemStack;
import com.i54mpenguin.protocol.items.ItemType;
import com.i54mpenguin.punisher.objects.gui.ConfirmationGUI;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelOne;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelThree;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelTwo;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelZero;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PunishCommand extends Command {

    public PunishCommand() {
        super("punish", "punisher.punish", "pun");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer){
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length >=1){
                switch (strings[0].toLowerCase()){
                    case "levelzero": {
                        LevelZero.open(player, player.getUniqueId().toString(), player.getName(), "(0/0)");
                        player.getPendingConnection().getVersion();
                        return;
                    }
                    case "levelone": {
                        LevelOne.open(player, player.getUniqueId().toString(), player.getName(), "(0/0)");
                        return;
                    }
                    case "leveltwo": {
                        LevelTwo.open(player, player.getUniqueId().toString(), player.getName(),"(0/0)");
                        return;
                    }
                    case "confirmation": {
                        ConfirmationGUI.open(player, player.getUniqueId().toString(), player.getName(), new ItemStack(ItemType.NO_DATA), 0);
                        return;
                    }
                    case "levelthree": {
                        LevelThree.open(player, player.getUniqueId().toString(), player.getName(), "(0/0)");
                    }
                }
            }
        }
    }
}
