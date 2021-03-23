package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.managers.PlayerDataManager;
import com.i54m.punisher.utils.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class StaffCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PlayerDataManager playerDataManager = PlayerDataManager.getINSTANCE();
    public StaffCommand() {
        super("staff", null, "onlinestaff", "staffonline");
    }

    // TODO: 2/03/2020 test this to make sure that it works correctly  and do this idea https://i.i54m.com/8xplgAaN.png
    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        boolean staff = false;
        commandSender.sendMessage(new ComponentBuilder("|-------------").color(ChatColor.GREEN).strikethrough(true).append(" Online Staff Members ").color(ChatColor.RED).strikethrough(false)
                .append("-------------|").color(ChatColor.GREEN).strikethrough(true).create());
        for (ServerInfo servers : plugin.getStaffServers()) {
            if (plugin.getStaff(servers).isEmpty()) continue;
            commandSender.sendMessage(new ComponentBuilder("|-----").strikethrough(true).color(ChatColor.RED).append( "[" + servers.getName() + "]").strikethrough(false).color(ChatColor.RED).append("-----|").strikethrough(true).color(ChatColor.RED).create());
            for (ProxiedPlayer player : plugin.getStaff(servers)) {
                staff = true;
                UUID uuid = player.getUniqueId();
                String prefixText = ChatColor.translateAlternateColorCodes('&', Permissions.getPrefix(uuid));
                if (!playerDataManager.getPlayerData(uuid, true).getBoolean("staffHide"))
                    commandSender.sendMessage(new ComponentBuilder("- " + prefixText + " ").append(player.getName()).color(ChatColor.RED).create());
                else if (commandSender.hasPermission("punisher.staff"))
                    commandSender.sendMessage(new ComponentBuilder("- " + prefixText + " ").strikethrough(true).append(player.getName()).strikethrough(true).color(ChatColor.RED)
                            .append(" - HIDDEN").color(ChatColor.GRAY).strikethrough(false).create());
            }
        }
        if (!staff)
            commandSender.sendMessage(new ComponentBuilder("No Staff Online!").color(ChatColor.RED).create());
        commandSender.sendMessage(new ComponentBuilder("|---------------------------------------------|").color(ChatColor.GREEN).strikethrough(true).create());
    }
}
