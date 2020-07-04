package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.storage.DatabaseManager;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class StaffhistoryCommand extends Command {
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();
    private String targetuuid;

    public StaffhistoryCommand() {
        super("staffhistory", "punisher.staffhistory", "shist");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length == 0) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("View a staff member's punishment history").color(ChatColor.RED).append("\nUsage: /staffhistory <player name> [-c|-r]").color(ChatColor.WHITE).create());
                return;
            }
            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
            Future<String> future = null;
            ExecutorService executorService = null;
            if (findTarget != null) {
                targetuuid = findTarget.getUniqueId().toString().replace("-", "");
            } else {
                UUIDFetcher uuidFetcher = new UUIDFetcher();
                uuidFetcher.fetch(strings[0]);
                executorService = Executors.newSingleThreadExecutor();
                future = executorService.submit(uuidFetcher);
            }
            if (future != null) {
                try {
                    targetuuid = future.get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    try {
                        throw new DataFecthException("UUID Required for next step", strings[0], "UUID", this.getName(), e);
                    } catch (DataFecthException dfe) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(dfe);
                        errorHandler.alert(dfe, commandSender);
                    }
                    executorService.shutdown();
                    return;
                }
                executorService.shutdown();
            }
            if (targetuuid != null) {
                String targetname = NameFetcher.getName(targetuuid);
                if (targetname == null) {
                    targetname = strings[0];
                }
                if (strings.length == 2 && (strings[1].contains("-c") || strings[1].contains("-r"))) {
                    try {
                        String sql = "DELETE FROM `staffhistory` WHERE `UUID`='" + targetuuid + "' ;";
                        PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
                        stmt.executeUpdate();
                        stmt.close();
                        String sql1 = "INSERT INTO `staffhistory` (UUID) VALUES ('" + targetuuid + "');";
                        PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("All staff history for: " + targetname + " has been cleared!").color(ChatColor.GREEN).create());
                    } catch (SQLException e) {
                        try {
                            throw new PunishmentsDatabaseException("Checking Staff history", targetname, this.getName(), e, "/staffhistory", strings);
                        } catch (PunishmentsDatabaseException pde) {
                            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                            errorHandler.log(pde);
                            errorHandler.alert(pde, commandSender);
                            return;
                        }
                    }
                }
                try {
                    String sql = "SELECT * FROM `staffhistory` WHERE UUID='" + targetuuid + "'";
                    PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
                    ResultSet results = stmt.executeQuery();
                    if (results.next()) {
                        player.sendMessage(new ComponentBuilder("|-------------").color(ChatColor.GREEN).strikethrough(true).append("StaffHistory for: " + targetname).color(ChatColor.RED).strikethrough(false)
                                .append("-------------|").color(ChatColor.GREEN).strikethrough(true).create());
                        if (results.getInt("Minor_Chat_Offence") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Minor Chat Offence: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Minor_Chat_Offence"))).color(ChatColor.RED).create());
                        if (results.getInt("Major_Chat_Offence") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Major Chat Offence: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Major_Chat_Offence"))).color(ChatColor.RED).create());
                        if (results.getInt("DDoS_DoX_Threats") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for DDoS/DoX Threats: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("DDoS_DoX_Threats"))).color(ChatColor.RED).create());
                        if (results.getInt("Inappropriate_Link") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Inappropriate Link: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Inappropriate_Link"))).color(ChatColor.RED).create());
                        if (results.getInt("Scamming") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Scamming: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Scamming"))).color(ChatColor.RED).create());
                        if (results.getInt("X_Raying") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for X-Raying: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("X_Raying"))).color(ChatColor.RED).create());
                        if (results.getInt("AutoClicker") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for AutoClicker(Non PvP): ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("AutoClicker"))).color(ChatColor.RED).create());
                        if (results.getInt("Fly_Speed_Hacking") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Fly/Speed Hacking: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Fly_Speed_Hacking"))).color(ChatColor.RED).create());
                        if (results.getInt("Malicious_PvP_Hacks") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Malicious PvP Hacks: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Malicious_PvP_Hacks"))).color(ChatColor.RED).create());
                        if (results.getInt("Disallowed_Mods") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Disallowed Mods: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Disallowed_Mods"))).color(ChatColor.RED).create());
                        if (results.getInt("Server_Advertisement") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Server_Advertisement: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Server_Advertisement"))).color(ChatColor.RED).create());
                        if (results.getInt("Greifing") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Greifing: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Greifing"))).color(ChatColor.RED).create());
                        if (results.getInt("Exploiting") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Exploiting: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Exploiting"))).color(ChatColor.RED).create());
                        if (results.getInt("Tpa_Trapping") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for TPA-Trapping: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Tpa_Trapping"))).color(ChatColor.RED).create());
                        if (results.getInt("Impersonation") != 0)
                            player.sendMessage(new ComponentBuilder("Punished for Player_Impersonation: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Impersonation"))).color(ChatColor.RED).create());
                        if (results.getInt("Manual_Punishments") != 0)
                            player.sendMessage(new ComponentBuilder("Punished Manually: ").color(ChatColor.GREEN).append(String.valueOf(results.getInt("Manual_Punishments"))).color(ChatColor.RED).create());
                    } else {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + " has not punished anyone yet!").color(ChatColor.RED).create());
                    }
                    stmt.close();
                    results.close();
                } catch (SQLException e) {
                    try {
                        throw new PunishmentsDatabaseException("Checking Staff history", targetname, this.getName(), e, "/staffhistory", strings);
                    } catch (PunishmentsDatabaseException pde) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pde);
                        errorHandler.alert(pde, commandSender);
                    }
                }
            } else {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name").color(ChatColor.RED).create());
            }
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }
}