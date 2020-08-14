package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.managers.PlayerDataManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

public class StaffHideCommand extends Command {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PlayerDataManager playerDataManager = PlayerDataManager.getINSTANCE();

    public StaffHideCommand() {
        super("staffhide", "punisher.staff.hide", "sh");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length >= 1) {
                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(strings[0]);
                if (target == null || !target.isConnected()) {
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That player is not online!").color(ChatColor.RED).create());
                    return;
                }
                Configuration playerData = playerDataManager.getPlayerData(target, false);
                if (player.hasPermission("punisher.staff.hide.others")) {
                    if (!playerData.getBoolean("staffHide")) {
                        playerData.set("staffHide", true);
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(target.getName() + " is now hidden on the staff list!").color(ChatColor.GREEN).create());
                    } else {
                        playerData.set("staffHide", false);
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(target.getName() + " is no longer hidden on the staff list!").color(ChatColor.GREEN).create());
                    }
                } else if (player.equals(target)) {
                    if (!playerData.getBoolean("staffHide")) {
                        playerData.set("staffHide", true);
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are now hidden on the staff list!").color(ChatColor.GREEN).create());
                    } else {
                        playerData.set("staffHide", false);
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are no longer hidden on the staff list!").color(ChatColor.GREEN).create());
                    }
                } else {
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You don't have permission to staffhide others!").color(ChatColor.RED).create());
                    return;
                }
                playerDataManager.savePlayerData(target.getUniqueId());
            } else {
                Configuration playerData = playerDataManager.getPlayerData(player, false);
                if (!playerData.getBoolean("staffHide")) {
                    playerData.set("staffHide", true);
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are now hidden on the staff list!").color(ChatColor.GREEN).create());
                } else {
                    playerData.set("staffHide", false);
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are no longer hidden on the staff list!").color(ChatColor.GREEN).create());
                }
                playerDataManager.savePlayerData(player.getUniqueId());
            }
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }
}
