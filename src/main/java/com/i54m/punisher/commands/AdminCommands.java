package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.chats.AdminChat;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;
import java.util.Map;

public class AdminCommands extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punisher = PunishmentManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();

    private final Map<CommandSender, Long> confirmation = new HashMap<>();

    public AdminCommands() {
        super("punisher", "punisher.admin");
    }//todo redo the command arguments to be like /punisher reset <mutes|bans|history|shist|all>

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (strings.length != 0) {
            if (strings[0].equalsIgnoreCase("reseteverything") || (strings.length > 1 && strings[1].equalsIgnoreCase("reset"))) {
                if (!confirmation.containsKey(commandSender)) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This command will reset settings and values to their defaults!").color(ChatColor.RED).create());
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("THIS CANNOT BE UNDONE!!").bold(true).color(ChatColor.RED).create());
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("It is recommended you use caution with this command!").color(ChatColor.RED).create());
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("To confirm, please re-type the command within 30 seconds!").color(ChatColor.RED).create());
                    confirmation.put(commandSender, System.currentTimeMillis());
                    return;
                } else {
                    long time = confirmation.get(commandSender);
                    if (System.currentTimeMillis() - time > 30000) {
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This command will reset settings and values to their defaults!").color(ChatColor.RED).create());
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("THIS CANNOT BE UNDONE!!").bold(true).color(ChatColor.RED).create());
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("It is recommended you use caution with this command!").color(ChatColor.RED).create());
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("To confirm, please re-type the command within 30 seconds!").color(ChatColor.RED).create());
                        confirmation.put(commandSender, System.currentTimeMillis());
                        return;
                    } else confirmation.remove(commandSender);
                }
            }
            if (strings[0].equalsIgnoreCase("punishments")) {
                if (strings.length == 1) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punisher punishments <reset|delete>").color(ChatColor.RED).create());
                    return;
                }
                if (strings[1].equalsIgnoreCase("reset")) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Command not yet available!").color(ChatColor.RED).create());
                    return;
                } else if (strings[1].equalsIgnoreCase("delete")) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Command not yet available!").color(ChatColor.RED).create());
                    return;
                } else {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punisher punishments <reset|delete>").color(ChatColor.RED).create());
                }
            } else if (strings[0].equalsIgnoreCase("bans")) {
                if (strings.length == 1) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punisher bans reset").color(ChatColor.RED).create());
                    return;
                }
                if (strings[1].equalsIgnoreCase("reset")) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Command not yet available!").color(ChatColor.RED).create());
                    return;
                }
            } else if (strings[0].equalsIgnoreCase("mutes")) {
                if (strings.length == 1) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punisher mutes reset").color(ChatColor.RED).create());
                    return;
                }
                if (strings[1].equalsIgnoreCase("reset")) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Command not yet available!").color(ChatColor.RED).create());
                    return;
                }
            } else if (strings[0].equalsIgnoreCase("reload")) {
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Reloading plugin. Please wait....").color(ChatColor.RED).create());
                plugin.onDisable();
                plugin.onEnable();
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Plugin Reloaded!").color(ChatColor.RED).create());
                AdminChat.sendMessage(commandSender.getName() + " has reloaded the plugin!");
                workerManager.runWorker(new WorkerManager.Worker(this::reloadRecovery));
                PunisherPlugin.getLOGS().warning(commandSender.getName() + " reloaded the plugin ");
            } else if (strings[0].equalsIgnoreCase("help")) {
                plugin.getProxy().getPluginManager().dispatchCommand(commandSender, "punisherhelp");
            } else if (strings[0].equalsIgnoreCase("reseteverything")) {
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Command not yet available!").color(ChatColor.RED).create());
                return;
            } else if (strings[0].equalsIgnoreCase("version")) {
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Current Version: " + plugin.getDescription().getVersion()).color(ChatColor.GREEN).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This is the latest version!").color(ChatColor.GREEN).create());
            } else if (strings[0].equalsIgnoreCase("view-cache")){
                for (Integer punishmentID : plugin.getStorageManager().getPunishmentCache().descendingKeySet()) {
                    try {
                        Punishment punishment = plugin.getStorageManager().getPunishmentFromId(punishmentID);
                        commandSender.sendMessage(new ComponentBuilder("#" + punishmentID.toString()).color(ChatColor.RED).
                                event(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        new Text(new ComponentBuilder(punishment.toString().replace(",", "\n"))
                                                .color(ChatColor.RED).create()))).create());
                    } catch (Exception e) {
                        commandSender.sendMessage(new ComponentBuilder("An Error occurred on punishment: " + punishmentID.toString()).color(ChatColor.RED).create());
                    }
                }
                commandSender.sendMessage(new ComponentBuilder("This command is only intended for developers so that they can verify that punishments are caching correctly!!")
                        .bold(true).color(ChatColor.RED).create());
            } else if (strings[0].equalsIgnoreCase("list-staff")){
                for (ServerInfo server : plugin.getStaffServers()) {
                    commandSender.sendMessage(new ComponentBuilder(server.getName()).color(ChatColor.RED).create());
                    for (ProxiedPlayer players : plugin.getStaff(server)) {
                        try {
                            commandSender.sendMessage(new ComponentBuilder(Permissions.getPrefix(players.getUniqueId()) + " " + players.getName()).color(ChatColor.RED).create());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                commandSender.sendMessage(new ComponentBuilder("This command is only intended for developers so that they can verify that staff members are getting flagged correctly!!")
                        .bold(true).color(ChatColor.RED).create());
            } else {
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("|------------").strikethrough(true).append(plugin.getPrefix()).strikethrough(false).append("------------|").strikethrough(true).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher reseteverything - ").color(ChatColor.RED).append("Reset all stored data in the sql data base and on file.").color(ChatColor.WHITE).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher help - ").color(ChatColor.RED).append("Help command for the punisher.").color(ChatColor.WHITE).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher punishments - ").color(ChatColor.RED).append("Reset or delete punishments used for punishment calculation.").color(ChatColor.WHITE).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher bans reset - ").color(ChatColor.RED).append("Reset and delete all bans.").color(ChatColor.WHITE).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher mutes reset - ").color(ChatColor.RED).append("Reset and delete all mutes.").color(ChatColor.WHITE).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher reload - ").color(ChatColor.RED).append("Reload the config files.").color(ChatColor.WHITE).create());
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher version - ").color(ChatColor.RED).append("Get the current version").color(ChatColor.WHITE).create());
                if (plugin.getConfig().getBoolean("DiscordIntegration.Enabled"))
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/discord admin - ").color(ChatColor.RED).append("Admin commands for the discord integration").color(ChatColor.WHITE).create());
            }
        } else {
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("|------------").strikethrough(true).append(plugin.getPrefix()).strikethrough(false).append("------------|").strikethrough(true).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher reseteverything - ").color(ChatColor.RED).append("Reset all stored data in the sql data base and on file.").color(ChatColor.WHITE).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher help - ").color(ChatColor.RED).append("Help command for the punisher.").color(ChatColor.WHITE).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher punishments - ").color(ChatColor.RED).append("Reset or delete punishments used for punishment calculation.").color(ChatColor.WHITE).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher bans reset - ").color(ChatColor.RED).append("Reset and delete all bans.").color(ChatColor.WHITE).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher mutes reset - ").color(ChatColor.RED).append("Reset and delete all mutes.").color(ChatColor.WHITE).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher reload - ").color(ChatColor.RED).append("Reload the config files.").color(ChatColor.WHITE).create());
            commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/punisher version - ").color(ChatColor.RED).append("Get the current version").color(ChatColor.WHITE).create());
            if (plugin.getConfig().getBoolean("DiscordIntegration.Enabled"))
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("/discord admin - ").color(ChatColor.RED).append("Admin commands for the discord integration").color(ChatColor.WHITE).create());
        }
    }

    private void reloadRecovery() {
        plugin.getStorageManager().clearCache();
        for (ProxiedPlayer players : plugin.getProxy().getPlayers()) {
            if (punisher.isBanned(players.getUniqueId())) {
                Punishment ban = punisher.getBan(players.getUniqueId());
                String timeLeft = punisher.getTimeLeft(ban);
                String reason = ban.getMessage();
                if (ban.isPermanent()) {
                    String banMessage = plugin.getConfig().getString("PermBan Message").replace("%timeleft%", timeLeft).replace("%reason%", reason);
                    players.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                } else {
                    String banMessage = plugin.getConfig().getString("TempBan Message").replace("%timeleft%", timeLeft).replace("%reason%", reason);
                    players.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                }
            }
        }
    }
}