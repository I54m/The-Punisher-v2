package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.utils.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ClearChatCommand extends Command {

    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    public ClearChatCommand() {
        super("clearchat", "punisher.clearchat", "cc", "chatclear");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        String prefix = Permissions.getPrefix(player.getUniqueId());
        if (strings.length <= 0) {
            workerManager.runWorker(new WorkerManager.Worker(() -> {
                for (ProxiedPlayer players : player.getServer().getInfo().getPlayers()) {
                    if (!players.hasPermission("punisher.clearchat.bypass")) {
                        for (int i = 0; i < 1000; i++) {
                            players.sendMessage(new TextComponent("\n"));
                        }

                    }
                    players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                    players.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', prefix)).append(" " + player.getName() + " has cleared chat!").color(ChatColor.RED).create());
                    players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                }
            }));
        } else {
            if (strings[0].equalsIgnoreCase("-anon")) {
                workerManager.runWorker(new WorkerManager.Worker(() -> {
                    for (ProxiedPlayer players : player.getServer().getInfo().getPlayers()) {
                        if (!players.hasPermission("punisher.clearchat.bypass")) {
                            for (int i = 0; i < 1000; i++) {
                                players.sendMessage(new TextComponent("\n"));
                            }
                        }
                        players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                        players.sendMessage(new ComponentBuilder("       Chat has been cleared!").color(ChatColor.RED).create());
                        players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                    }
                }));
                //anon clear on current server
            } else if (strings[0].equalsIgnoreCase("-all")) {
                workerManager.runWorker(new WorkerManager.Worker(() -> {
                    for (ProxiedPlayer players : plugin.getProxy().getPlayers()) {
                        if (!players.hasPermission("punisher.clearchat.bypass")) {
                            for (int i = 0; i < 1000; i++) {
                                players.sendMessage(new TextComponent("\n"));
                            }
                        }
                        players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                        players.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', prefix)).append(" " + player.getName() + " has cleared chat globally!").color(ChatColor.RED).create());
                        players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                    }
                }));
                //clear all servers not anon
            } else if (strings.length >= 2) {
                if ((strings[0].equalsIgnoreCase("-anon") && strings[1].equalsIgnoreCase("-all")) ||
                        (strings[0].equalsIgnoreCase("-all") && strings[1].equalsIgnoreCase("-anon"))) {
                    workerManager.runWorker(new WorkerManager.Worker(() -> {
                        for (ProxiedPlayer players : plugin.getProxy().getPlayers()) {
                            if (!players.hasPermission("punisher.clearchat.bypass")) {
                                for (int i = 0; i < 1000; i++) {
                                    players.sendMessage(new TextComponent("\n"));
                                }
                            }
                            players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                            players.sendMessage(new ComponentBuilder("       Chat has been cleared globally!").color(ChatColor.RED).create());
                            players.sendMessage(new ComponentBuilder("|---------------------------------------|").color(ChatColor.GRAY).strikethrough(true).create());
                        }
                    }));
                    //anon clear on all servers
                }
            } else {
                commandSender.sendMessage(new ComponentBuilder("/clearchat").color(ChatColor.RED).append(": clear the chat for all players (except staff)").color(ChatColor.WHITE).create());
            }
        }
    }
}
