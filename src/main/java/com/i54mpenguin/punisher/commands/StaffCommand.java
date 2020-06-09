package com.i54mpenguin.punisher.commands;

import com.i54mpenguin.punisher.PunisherPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class StaffCommand extends Command {
    public StaffCommand() {
        super("staff", null, "onlinestaff", "staffonline");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    // TODO: 2/03/2020 test this to make sure that it works correctly
    @Override
    public void execute(CommandSender commandSender, String[] strings) {
            
            commandSender.sendMessage(new ComponentBuilder("|------------- ").color(ChatColor.GREEN).strikethrough(true).append("Online Staff Members").color(ChatColor.RED).strikethrough(false)
                    .append(" -------------|").color(ChatColor.GREEN).strikethrough(true).create());
//            for (ServerInfo servers : plugin.staff.keySet()) {
//                commandSender.sendMessage(new ComponentBuilder(servers.getName()).color(ChatColor.RED).create());
//                for (ProxiedPlayer player : plugin.staff.get(servers)) {
//                    UUID uuid = player.getUniqueId();
//                    String prefixText = ChatColor.translateAlternateColorCodes('&', Permissions.getPrefix(uuid));
//                    if (!PunisherPlugin.staffHideConfig.contains(player.getUniqueId().toString()))
//                        commandSender.sendMessage(new ComponentBuilder(prefixText + " ").append(player.getName()).color(ChatColor.RED).create());
//                    else
//                        if (commandSender.hasPermission("punisher.staff"))
//                            commandSender.sendMessage(new ComponentBuilder(prefixText + " ").strikethrough(true).append(player.getName()).strikethrough(true).color(ChatColor.RED)
//                                .append(" - HIDDEN").color(ChatColor.GRAY).strikethrough(false).create());
//                }
//            }
        commandSender.sendMessage(new ComponentBuilder("|---------------------------------------------|").color(ChatColor.GREEN).strikethrough(true).create());
    }
}
