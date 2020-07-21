package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.chats.AdminChat;
import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.listeners.ServerConnect;
import com.i54m.punisher.managers.storage.DatabaseManager;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.UpdateChecker;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AdminCommands extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punisher = PunishmentManager.getINSTANCE();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();

    private Map<CommandSender, Long> confirmation = new HashMap<>();

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
                    AdminChat.sendMessage("\n", false);
                    plugin.saveDefaultPunishments();
                    PunisherPlugin.getLOGS().warning(commandSender.getName() + " reset punishments");
                    AdminChat.sendMessage(commandSender.getName() + " has reset all punishments", true);
                    AdminChat.sendMessage("This means automatic punishments will use default values!", true);
                    try {
                        plugin.loadConfigs();
                    } catch (Exception e) {
                        commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Unable to reload config files, try restarting your server and try again.").color(ChatColor.RED).create());
                        AdminChat.sendMessage("Plugin failed to reload config! Try restarting the server and try again!", true);
                    }
                    AdminChat.sendMessage("\n", false);
                } else if (strings[1].equalsIgnoreCase("delete")) {
                    plugin.getPunishments().set("Minor_Chat_Offence", null);
                    plugin.getPunishments().set("Major_Chat_Offence", null);
                    plugin.getPunishments().set("DDoS/DoX_Threats", null);
                    plugin.getPunishments().set("Inappropriate_Link", null);
                    plugin.getPunishments().set("Impersonating_Staff", null);
                    plugin.getPunishments().set("X-Raying", null);
                    plugin.getPunishments().set("AutoClicker(Non_PvP)", null);
                    plugin.getPunishments().set("Fly/Speed_Hacking", null);
                    plugin.getPunishments().set("Malicious_PvP_Hacks", null);
                    plugin.getPunishments().set("Other_Hacks", null);
                    plugin.getPunishments().set("Server_Advertisement", null);
                    plugin.getPunishments().set("Exploiting", null);
                    plugin.getPunishments().set("TPA-Trapping", null);
                    plugin.getPunishments().set("Other_Major_Offence", null);
                    plugin.getPunishments().set("Other_Minor_Offence", null);
                    plugin.getPunishments().set("Player_Impersonation", null);
                    plugin.saveConfig();
                    AdminChat.sendMessages(true, commandSender.getName() + " has REMOVED all automatic punishment values!", "Automatic punishments will not longer work!", "To reset punishments to their defaults do: ", "/punisher punishments reset", " ");
                    PunisherPlugin.getLOGS().warning(commandSender.getName() + " deleted punishments");
                } else {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punisher punishments <reset|delete>").color(ChatColor.RED).create());
                }
            } else if (strings[0].equalsIgnoreCase("bans")) {
                if (strings.length == 1) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punisher bans reset").color(ChatColor.RED).create());
                    return;
                }
                if (strings[1].equalsIgnoreCase("reset")) {
                    try {
                        String sql1 = "TRUNCATE `bans`";
                        PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                        dbManager.resetCache();
                    } catch (SQLException e) {
                        try {
                            throw new PunishmentsDatabaseException("Resetting bans", null, this.getName(), e);
                        } catch (PunishmentsDatabaseException pde) {
                            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                            errorHandler.log(pde);
                            errorHandler.alert(pde, commandSender);
                            return;
                        }
                    }
                    AdminChat.sendMessage("\n", false);
                    AdminChat.sendMessage(commandSender.getName() + " has reset all bans!", true);
                    AdminChat.sendMessage("\n", false);
                    PunisherPlugin.getLOGS().warning(commandSender.getName() + " reset bans");
                }
            } else if (strings[0].equalsIgnoreCase("mutes")) {
                if (strings.length == 1) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Usage: /punisher mutes reset").color(ChatColor.RED).create());
                    return;
                }
                if (strings[1].equalsIgnoreCase("reset")) {
                    try {
                        String sql1 = "TRUNCATE `mutes`";
                        PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                        dbManager.resetCache();
                    } catch (SQLException e) {
                        try {
                            throw new PunishmentsDatabaseException("Resetting mutes", null, this.getName(), e);
                        } catch (PunishmentsDatabaseException pde) {
                            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                            errorHandler.log(pde);
                            errorHandler.alert(pde, commandSender);
                            return;
                        }
                    }
                    AdminChat.sendMessage("\n", false);
                    AdminChat.sendMessage(commandSender.getName() + " has reset all mutes!", true);
                    AdminChat.sendMessage("\n", false);
                    PunisherPlugin.getLOGS().warning(commandSender.getName() + " reset mutes");
                }
            } else if (strings[0].equalsIgnoreCase("reload")) {
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Reloading plugin. Please wait....").color(ChatColor.RED).create());
                plugin.onDisable();
                plugin.onEnable();
                commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Plugin Reloaded!").color(ChatColor.RED).create());
                AdminChat.sendMessage(commandSender.getName() + " has reloaded the plugin!", true);
                workerManager.runWorker(new WorkerManager.Worker(this::reloadRecovery));
                PunisherPlugin.getLOGS().warning(commandSender.getName() + " reloaded the plugin ");
            } else if (strings[0].equalsIgnoreCase("help")) {
                plugin.getProxy().getPluginManager().dispatchCommand(commandSender, "punisherhelp");
            } else if (strings[0].equalsIgnoreCase("reseteverything")) {
                AdminChat.sendMessage(commandSender.getName() + " has initiated a full reset of everything, please wait....", true);
                //todo add a reset abort feature
                PunisherPlugin.getLOGS().warning(commandSender.getName() + " has initiated a full reset of everything!");
                AdminChat.sendMessage("Plugin command and event listeners will be unloaded during this process!", true);
                plugin.getProxy().getPluginManager().unregisterCommands(plugin);
                plugin.getProxy().getPluginManager().unregisterListeners(plugin);
                try {
                    String sql1 = "DROP DATABASE `" + dbManager.database + "`";
                    PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                    stmt1.executeUpdate();
                    stmt1.close();
                } catch (SQLException e) {
                    try {
                        throw new PunishmentsDatabaseException("Resetting entire punishment database", null, this.getName(), e);
                    } catch (PunishmentsDatabaseException pde) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pde);
                        errorHandler.alert(pde, commandSender);
                        AdminChat.sendMessages(true, "An error occurred during the reset process, nothing was reset!", "Reloading plugin, please wait...");
                        PunisherPlugin.getLOGS().warning("An error occurred during the reset process, nothing was reset!");
                        plugin.onDisable();
                        plugin.onEnable();
                        AdminChat.sendMessage("Plugin reloaded!", true);
                        return;
                    }
                }
                AdminChat.sendMessages(false, "Reset all mutes!", "Reset all bans!", "Reset all stored ips!", "Reset all player history!", "Reset all staff history!");
                plugin.onDisable();
                dbManager.clearCache();
                AdminChat.sendMessage("Cleared all cached data!", false);
                plugin.saveDefaultPunishments();
                AdminChat.sendMessage("Reset all punishments!", false);
                AdminChat.sendMessage("This means automatic punishments will use default values!", false);
//                PunisherPlugin.reputationFile.delete();
//                AdminChat.sendMessage("Reset all reputation scores!", false);
//                PunisherPlugin.playerInfoFile.delete();
//                AdminChat.sendMessage("Reset all stored playerinfo!", false);
//                if (plugin.getConfig().getBoolean("DiscordIntegration.Enabled")) {
//                    PunisherPlugin.discordIntegrationFile.delete();
//                    DiscordMain.verifiedUsers.clear();
//                    DiscordMain.userCodes.clear();
//                    AdminChat.sendMessage("Reset all linked discord users!", false);
//                }
                AdminChat.sendMessages(false, " ", "Reloading plugin please wait....", " ");
                plugin.onEnable();
                AdminChat.sendMessages(false, "Reloaded the plugin!", "Warning, plugin is in an out of the box state and will need to be configured properly!", "Collecting required info on online players in order to solve errors..");
                ServerConnect.lastJoinId = 0;
                workerManager.runWorker(new WorkerManager.Worker(() -> {
                    for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
//                        PunisherPlugin.reputationConfig.set(all.getUniqueId().toString().replace("-", ""), 5.0);
//                        PunisherPlugin.playerInfoConfig.set(all.getUniqueId().toString().replace("-", "") + ".lastlogin", System.currentTimeMillis());
//                        PunisherPlugin.playerInfoConfig.set(all.getUniqueId().toString().replace("-", "") + ".lastserver", all.getServer().getInfo().getName());
//                        PunisherPlugin.playerInfoConfig.set("lastjoinid", (ServerConnect.lastJoinId + 1));
//                        PunisherPlugin.playerInfoConfig.set(String.valueOf((ServerConnect.lastJoinId + 1)), all.getUniqueId().toString().replace("-", ""));
//                        ServerConnect.lastJoinId++;
//                        PunisherPlugin.saveInfo();
                        String fetcheduuid = all.getUniqueId().toString().replace("-", "");
                        String targetName = all.getName();
                        try {
                            //update ip and make sure there is one in the database
                            String sqlip = "SELECT * FROM `altlist` WHERE UUID='" + fetcheduuid + "'";
                            PreparedStatement stmtip = dbManager.connection.prepareStatement(sqlip);
                            ResultSet resultsip = stmtip.executeQuery();
                            if (resultsip.next()) {
                                if (!resultsip.getString("ip").equals(all.getAddress().getHostString())) {
                                    String oldip = resultsip.getString("ip");
                                    String sqlipadd = "UPDATE `altlist` SET `ip`='" + all.getAddress().getAddress().getHostAddress() + "' WHERE `ip`='" + oldip + "' ;";
                                    PreparedStatement stmtipadd = dbManager.connection.prepareStatement(sqlipadd);
                                    stmtipadd.executeUpdate();
                                    stmtipadd.close();
                                }
                            } else {
                                String sql1 = "INSERT INTO `altlist` (`UUID`, `ip`) VALUES ('" + fetcheduuid + "', '" + all.getAddress().getAddress().getHostAddress() + "');";
                                PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                                stmt1.executeUpdate();
                                stmt1.close();
                            }
                            stmtip.close();
                            resultsip.close();
                        } catch (SQLException sqle) {
                            try {
                                throw new PunishmentsDatabaseException("Updating ip in altlist", targetName, this.getName(), sqle);
                            } catch (PunishmentsDatabaseException pde) {
                                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                errorHandler.log(pde);
                                errorHandler.adminChatAlert(sqle, all);
                            }
                        }
                        try {
                            //update ip hist
                            String sql = "SELECT * FROM `iphist` WHERE UUID='" + fetcheduuid + "'";
                            PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
                            ResultSet results = stmt.executeQuery();
                            if (results.next()) {
                                if (!results.getString("ip").equals(all.getAddress().getHostString())) {
                                    String addip = "INSERT INTO `iphist` (`UUID`, `date`, `ip`) VALUES ('" + fetcheduuid + "', '" + System.currentTimeMillis() + "', '" + all.getAddress().getAddress().getHostAddress() + "');";
                                    PreparedStatement addipstmt = dbManager.connection.prepareStatement(addip);
                                    addipstmt.executeUpdate();
                                    addipstmt.close();
                                }
                            } else {
                                String addip = "INSERT INTO `iphist` (`UUID`, `date`, `ip`) VALUES ('" + fetcheduuid + "', '" + System.currentTimeMillis() + "', '" + all.getAddress().getAddress().getHostAddress() + "');";
                                PreparedStatement addipstmt = dbManager.connection.prepareStatement(addip);
                                addipstmt.executeUpdate();
                                addipstmt.close();
                            }
                        } catch (SQLException sqle) {
                            try {
                                throw new PunishmentsDatabaseException("Updating ip in iphist", targetName, this.getName(), sqle);
                            } catch (PunishmentsDatabaseException pde) {
                                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                errorHandler.log(pde);
                                errorHandler.adminChatAlert(sqle, all);
                            }
                        }
                    }
                }));
                AdminChat.sendMessages(false, "Required info collected!", " ", "Plugin successfully reset!", " ");
            } else if (strings[0].equalsIgnoreCase("version")) {
                if (!PunisherPlugin.isUpdate()) {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Current Bungeecord Version: " + plugin.getDescription().getVersion()).color(ChatColor.GREEN).create());
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This is the latest version!").color(ChatColor.GREEN).create());
                } else {
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Current Bungeecord Version: " + plugin.getDescription().getVersion()).color(ChatColor.RED).create());
                    commandSender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Latest version: " + UpdateChecker.getCurrentVersion()).color(ChatColor.RED).create());
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
        dbManager.resetCache();
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