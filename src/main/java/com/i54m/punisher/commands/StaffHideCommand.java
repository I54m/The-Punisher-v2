package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class StaffHideCommand extends Command {
    public StaffHideCommand() {
        super("staffhide", "punisher.staff.hide", "sh");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer){
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length >= 1) {
                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(strings[0]);
                if (target == null){
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player is not online!").color(ChatColor.RED).create());
                    return;
                }
//                if (player.hasPermission("punisher.staff.hide.others")) {
//                    if (!PunisherPlugin.staffHideConfig.contains(target.getUniqueId().toString())) {
//                        PunisherPlugin.staffHideConfig.set(target.getUniqueId().toString(), "hidden");
//                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are now hidden on the staff list!").color(ChatColor.GREEN).create());
//                    } else {
//                        PunisherPlugin.staffHideConfig.set(target.getUniqueId().toString(), null);
//                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are no longer hidden on the staff list!").color(ChatColor.GREEN).create());
//                    }
//                } else if (player.equals(target)){
//                    if (!PunisherPlugin.staffHideConfig.contains(player.getUniqueId().toString())) {
//                        PunisherPlugin.staffHideConfig.set(player.getUniqueId().toString(), "hidden");
//                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are now hidden on the staff list!").color(ChatColor.GREEN).create());
//                    }else {
//                        PunisherPlugin.staffHideConfig.set(player.getUniqueId().toString(), null);
//                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are no longer hidden on the staff list!").color(ChatColor.GREEN).create());
//                    }
//                }else{
//                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You don't have permission to staffhide others!").color(ChatColor.RED).create());
//                    return;
//                }

//            }else{
//                if (!PunisherPlugin.staffHideConfig.contains(player.getUniqueId().toString())) {
//                    PunisherPlugin.staffHideConfig.set(player.getUniqueId().toString(), "hidden");
//                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are now hidden on the staff list!").color(ChatColor.GREEN).create());
//                }else {
//                    PunisherPlugin.staffHideConfig.set(player.getUniqueId().toString(), null);
//                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are no longer hidden on the staff list!").color(ChatColor.GREEN).create());
//                }
            }
//            PunisherPlugin.saveStaffHide();
        }else{
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }
}
