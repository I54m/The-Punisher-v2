package com.i54mpenguin.punisher.managers;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.fiftyfour.punisher.bungee.PunisherPlugin;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.objects.Punishment;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    private PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final PunishmentManager punishmentManager = PunishmentManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final ErrorHandler errorHandler = ErrorHandler.getINSTANCE();

    @Getter
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private DatabaseManager() {
    }

    TreeMap<Integer, Punishment> PunishmentCache = new TreeMap<>();
    Map<String, ArrayList<Integer>> ActivePunishmentCache = new HashMap<>();//although this can support more than 2 active punishments at once we only allow 1 mute and 1 ban at once
    ScheduledTask cacheTask;

    public String database;
    public Connection connection;
    private String host, username, password, extraArguments;
    private int port;
    private HikariDataSource hikari;

    public void start() throws Exception {
        //setup mysql connection
        if (plugin == null)
            plugin = PunisherPlugin.getInstance();
        plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "Establishing MYSQL connection...");
        host = PunisherPlugin.config.getString("MySQL.host", "localhost");
        database = PunisherPlugin.config.getString("MySQL.database", "punisherdb");
        username = PunisherPlugin.config.getString("MySQL.username", "punisher");
        password = PunisherPlugin.config.getString("MySQL.password", "punisher");
        port = PunisherPlugin.config.getInt("MySQL.port", 3306);
        extraArguments = PunisherPlugin.config.getString("MySQL.extraArguments", "?useSSL=false");
        hikari = new HikariDataSource();
        hikari.addDataSourceProperty("serverName", host);
        hikari.addDataSourceProperty("port", port);
        hikari.setPassword(password);
        hikari.setUsername(username);
        hikari.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.extraArguments);
        hikari.setPoolName("The-Punisher-DatabaseManager");
        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(10);
        //open connection
        openConnection();
        //setup mysql
        setupmysql();
        //start caching
        startCaching();
    }

    public void stop() {
        try {
            if (cacheTask != null)
                cacheTask.cancel();
            if (hikari != null && !hikari.isClosed()) {
                plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "Closing Storage....");
                hikari.close();
                connection = null;
                hikari = null;
                plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "Storage Closed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe(plugin.prefix + ChatColor.RED + "Error occurred on database manager shutdown!");
            e.printStackTrace();
        }
    }


    private void openConnection() throws Exception {
        try {
            if (connection != null && !connection.isClosed() || hikari == null)
                return;
            connection = hikari.getConnection();
            plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "MYSQL Connected to server: " + host + ":" + port + " with user: " + username + "!");
        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.prefix + ChatColor.RED + "MYSQL Connection failed!!! (SQLException)");
            PunisherPlugin.LOGS.severe("Error Message: " + e.getMessage());
            StringBuilder stacktrace = new StringBuilder();
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                stacktrace.append(stackTraceElement.toString()).append("\n");
            }
            PunisherPlugin.LOGS.severe("Stack Trace: " + stacktrace.toString());
            if (e.getCause() != null)
                PunisherPlugin.LOGS.severe("Error Cause Message: " + e.getCause().getMessage());
            ProxyServer.getInstance().getLogger().severe(" ");
            ProxyServer.getInstance().getLogger().severe(plugin.prefix + ChatColor.RED + "An error was encountered and debug info was logged to log file!");
            ProxyServer.getInstance().getLogger().severe(plugin.prefix + ChatColor.RED + "Error Message: " + e.getMessage());
            if (e.getCause() != null)
                ProxyServer.getInstance().getLogger().severe(plugin.prefix + ChatColor.RED + "Error Cause Message: " + e.getCause().getMessage());
            ProxyServer.getInstance().getLogger().severe(" ");
            throw new Exception("Mysql connection failed", e);
        }
    }

    public HikariDataSource getDataSource() {
        return hikari;
    }

    private void setupmysql() throws Exception {
        try {
            plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "Setting up MYSQL...");
            String createdb = "CREATE DATABASE IF NOT EXISTS " + database;
            String punishments = "CREATE TABLE IF NOT EXISTS `punisherdb`.`punishments` ( `id` INT NOT NULL AUTO_INCREMENT , `type` VARCHAR(4) NOT NULL , `reason` VARCHAR(20) NOT NULL ," +
                    " `issueDate` VARCHAR(20) NOT NULL , `expiration` BIGINT NULL DEFAULT NULL , `targetUUID` VARCHAR(32) NOT NULL , `targetName` VARCHAR(16) NOT NULL , " +
                    "`punisherUUID` VARCHAR(32) NOT NULL , `message` VARCHAR(512) NULL DEFAULT NULL , `status` VARCHAR(10) NOT NULL , `removerUUID` VARCHAR(32) NULL DEFAULT NULL ," +
                    " PRIMARY KEY (`id`)) ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
            String history = "CREATE TABLE IF NOT EXISTS`" + database + "`.`history` ( `UUID` VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ," +
                    " `Minor_Chat_Offence` TINYINT NOT NULL DEFAULT '0' , `Major_Chat_Offence` TINYINT NOT NULL DEFAULT '0' , `DDoS_DoX_Threats` TINYINT NOT NULL DEFAULT '0' ," +
                    " `Inappropriate_Link` TINYINT NOT NULL DEFAULT '0' , `Scamming` TINYINT NOT NULL DEFAULT '0' , `X_Raying` TINYINT NOT NULL DEFAULT '0' ," +
                    " `AutoClicker` TINYINT NOT NULL DEFAULT '0' , `Fly_Speed_Hacking` TINYINT NOT NULL DEFAULT '0' , `Malicious_PvP_Hacks` TINYINT NOT NULL DEFAULT '0' ," +
                    " `Disallowed_Mods` TINYINT NOT NULL DEFAULT '0' , `Server_Advertisement` TINYINT NOT NULL DEFAULT '0' , `Greifing` TINYINT NOT NULL DEFAULT '0' ," +
                    " `Exploiting` TINYINT NOT NULL DEFAULT '0' , `Tpa_Trapping` TINYINT NOT NULL DEFAULT '0' , `Impersonation` TINYINT NOT NULL DEFAULT '0' , `Manual_Punishments` TINYINT NOT NULL DEFAULT '0' ) ENGINE = InnoDB;";
            String staffhist = "CREATE TABLE IF NOT EXISTS`" + database + "`.`staffhistory` ( `UUID` VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ," +
                    " `Minor_Chat_Offence` TINYINT NOT NULL DEFAULT '0' , `Major_Chat_Offence` TINYINT NOT NULL DEFAULT '0' , `DDoS_DoX_Threats` TINYINT NOT NULL DEFAULT '0' ," +
                    " `Inappropriate_Link` TINYINT NOT NULL DEFAULT '0' , `Scamming` TINYINT NOT NULL DEFAULT '0' , `X_Raying` TINYINT NOT NULL DEFAULT '0' ," +
                    " `AutoClicker` TINYINT NOT NULL DEFAULT '0' , `Fly_Speed_Hacking` TINYINT NOT NULL DEFAULT '0' , `Malicious_PvP_Hacks` TINYINT NOT NULL DEFAULT '0' ," +
                    " `Disallowed_Mods` TINYINT NOT NULL DEFAULT '0' , `Server_Advertisement` TINYINT NOT NULL DEFAULT '0' , `Greifing` TINYINT NOT NULL DEFAULT '0' ," +
                    " `Exploiting` TINYINT NOT NULL DEFAULT '0' , `Tpa_Trapping` TINYINT NOT NULL DEFAULT '0' , `Impersonation` TINYINT NOT NULL DEFAULT '0' , `Manual_Punishments` TINYINT NOT NULL DEFAULT '0' ) ENGINE = InnoDB;";
            String alt = "CREATE TABLE IF NOT EXISTS`" + database + "`.`altlist` ( `UUID` VARCHAR(32) NOT NULL , `ip` VARCHAR(32) NOT NULL ) ENGINE = InnoDB;";
            String iphist = "CREATE TABLE IF NOT EXISTS`" + database + "`.`iphist` ( `UUID` VARCHAR(32) NOT NULL , `date` BIGINT NOT NULL , `ip` VARCHAR(32) NOT NULL ) ENGINE = InnoDB;";
            String usedb = "USE " + database;
            PreparedStatement stmt = connection.prepareStatement(createdb);
            PreparedStatement stmt1 = connection.prepareStatement(punishments);
            PreparedStatement stmt2 = connection.prepareStatement(history);
            PreparedStatement stmt3 = connection.prepareStatement(staffhist);
            PreparedStatement stmt4 = connection.prepareStatement(alt);
            PreparedStatement stmt5 = connection.prepareStatement(iphist);
            PreparedStatement stmt6 = connection.prepareStatement(usedb);
            stmt.executeUpdate();
            stmt.close();
            stmt1.executeUpdate();
            stmt1.close();
            stmt2.executeUpdate();
            stmt2.close();
            stmt3.executeUpdate();
            stmt3.close();
            stmt4.executeUpdate();
            stmt4.close();
            stmt5.executeUpdate();
            stmt5.close();
            stmt6.executeUpdate();
            stmt6.close();
            plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "MYSQL setup!");
            plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "Started Database Manager!");
        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.prefix + ChatColor.RED + "Could not Setup MYSQL!!");
            plugin.getLogger().severe(plugin.prefix + ChatColor.RED + "Could not Start Database Manager!!");
            PunisherPlugin.LOGS.severe("Error Message: " + e.getMessage());
            StringBuilder stacktrace = new StringBuilder();
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                stacktrace.append(stackTraceElement.toString()).append("\n");
            }
            PunisherPlugin.LOGS.severe("Stack Trace: " + stacktrace.toString());
            if (e.getCause() != null)
                PunisherPlugin.LOGS.severe("Error Cause Message: " + e.getCause().getMessage());
            ProxyServer.getInstance().getLogger().severe(" ");
            ProxyServer.getInstance().getLogger().severe(plugin.prefix + ChatColor.RED + "An error was encountered and debug info was logged to log file!");
            ProxyServer.getInstance().getLogger().severe(plugin.prefix + ChatColor.RED + "Error Message: " + e.getMessage());
            if (e.getCause() != null)
                ProxyServer.getInstance().getLogger().severe(plugin.prefix + ChatColor.RED + "Error Cause Message: " + e.getCause().getMessage());
            ProxyServer.getInstance().getLogger().severe(" ");
            throw new Exception("Failed to setup mysql!", e);
        }
    }

    public int getOffences(String targetuuid, Punishment.Reason reason) throws SQLException {
        String sql = "SELECT * FROM `history` WHERE UUID='" + targetuuid + "'";
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet results = stmt.executeQuery();
        int punishmentno;
        if (results.next())
            if (reason == Punishment.Reason.Custom) punishmentno = results.getInt("Manual_Punishments");
            else punishmentno = results.getInt(reason.toString());
        else punishmentno = 0;
        stmt.close();
        results.close();
        return punishmentno;
    }

    public void updatePunishment(@NotNull Punishment punishment) throws SQLException {
        String message = punishment.getMessage();
        if (message.contains("'"))
            message = message.replaceAll("'", "%sinquo%");
        if (message.contains("\""))
            message = message.replaceAll("\"", "%dubquo%");
        if (message.contains("`"))
            message = message.replaceAll("`", "%bcktck%");
        String sql = "UPDATE `punishments` SET `type`='" + punishment.getType().toString() + "', `reason`='" + punishment.getReason().toString() + "', `issueDate`='" + punishment.getIssueDate()
                + "', `expiration`='" + punishment.getExpiration() + "', `targetUUID`='" + punishment.getTargetUUID() + "', `targetName`='" + punishment.getTargetName() + "', `punisherUUID`='" + punishment.getPunisherUUID()
                + "', `message`='" + message + "', `status`='" + punishment.getStatus().toString() + "', `removerUUID`='" + punishment.getRemoverUUID() + "' WHERE `id`='" + punishment.getId() + "' ;";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.executeUpdate();
        stmt.close();
    }

    public void createHistory(String targetUUID) throws SQLException {
        String sql = "SELECT * FROM `history` WHERE UUID='" + targetUUID + "'";
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet results = stmt.executeQuery();
        if (!results.next()) {
            String sql1 = "INSERT INTO `history` (UUID) VALUES ('" + targetUUID + "');";
            PreparedStatement stmt1 = connection.prepareStatement(sql1);
            stmt1.executeUpdate();
            stmt1.close();
        }
        stmt.close();
        results.close();
    }

    public void createStaffHistory(String targetUUID) throws SQLException {
        String sql2 = "SELECT * FROM `staffhistory` WHERE UUID='" + targetUUID + "'";
        PreparedStatement stmt2 = connection.prepareStatement(sql2);
        ResultSet results2 = stmt2.executeQuery();
        if (!results2.next()) {
            String sql3 = "INSERT INTO `staffhistory` (UUID) VALUES ('" + targetUUID + "');";
            PreparedStatement stmt3 = connection.prepareStatement(sql3);
            stmt3.executeUpdate();
            stmt3.close();
        }
        stmt2.close();
        results2.close();
    }

    public void incrementHistory(@NotNull Punishment punishment) throws SQLException {
        String sql = "SELECT * FROM `history` WHERE UUID='" + punishment.getTargetUUID() + "'";
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet history = stmt.executeQuery();
        history.next();
        int current;
        if (punishment.isManual()) current = history.getInt("Manual_Punishments");
        else current = history.getInt(punishment.getReason().toString());
        stmt.close();
        history.close();
        current++;
        if (punishment.isManual()) {
            String sql4 = "UPDATE `history` SET `Manual_Punishments`='" + current + "' WHERE `UUID`='" + punishment.getTargetUUID() + "' ;";
            PreparedStatement stmt4 = connection.prepareStatement(sql4);
            stmt4.executeUpdate();
            stmt4.close();
        } else {
            String sql3 = "UPDATE `history` SET `" + punishment.getReason().toString() + "`='" + current + "' WHERE `UUID`='" + punishment.getTargetUUID() + "' ;";
            PreparedStatement stmt3 = connection.prepareStatement(sql3);
            stmt3.executeUpdate();
            stmt3.close();
        }
    }

    public void incrementStaffHistory(@NotNull Punishment punishment) throws SQLException {
        String sql2 = "SELECT * FROM `staffhistory` WHERE UUID='" + punishment.getPunisherUUID() + "'";
        PreparedStatement stmt2 = connection.prepareStatement(sql2);
        ResultSet staffHistory = stmt2.executeQuery();
        staffHistory.next();
        int Punishmentno;
        if (punishment.isManual()) Punishmentno = staffHistory.getInt("Manual_Punishments");
        else Punishmentno = staffHistory.getInt(punishment.getReason().toString());
        stmt2.close();
        staffHistory.close();
        Punishmentno++;
        if (punishment.isManual()) {
            String sql1 = "UPDATE `staffhistory` SET `Manual_Punishments`=" + Punishmentno + " WHERE UUID='" + punishment.getPunisherUUID() + "';";
            PreparedStatement stmt1 = connection.prepareStatement(sql1);
            stmt1.executeUpdate();
            stmt1.close();
        } else {
            String sql1 = "UPDATE `staffhistory` SET `" + punishment.getReason().toString() + "`=" + Punishmentno + " WHERE UUID='" + punishment.getPunisherUUID() + "';";
            PreparedStatement stmt1 = connection.prepareStatement(sql1);
            stmt1.executeUpdate();
            stmt1.close();
        }
    }

    public void loadUser(String targetuuid) throws SQLException {
        String sqlpunishment = "SELECT * FROM `punishments` WHERE targetUUID='" + targetuuid + "';";
        PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
        ResultSet resultspunishment = stmtpunishment.executeQuery();
        while (resultspunishment.next()) {
            Punishment punishment = new Punishment(resultspunishment.getInt("id"), Punishment.Type.valueOf(resultspunishment.getString("type")),
                    Punishment.Reason.valueOf(resultspunishment.getString("reason")), resultspunishment.getString("issueDate"), resultspunishment.getLong("expiration"),
                    resultspunishment.getString("targetUUID"), resultspunishment.getString("targetName"), resultspunishment.getString("punisherUUID"),
                    resultspunishment.getString("message"), Punishment.Status.valueOf(resultspunishment.getString("status")), resultspunishment.getString("removerUUID"));
            String message = punishment.getMessage();
            if (message.contains("%sinquo%"))
                message = message.replaceAll("%sinquo%", "'");
            if (message.contains("%dubquo%"))
                message = message.replaceAll("%dubquo%", "\"");
            if (message.contains("%bcktck%"))
                message = message.replaceAll("%bcktck%", "`");
            punishment.setMessage(message);
            if (!PunishmentCache.containsValue(punishment))
                PunishmentCache.put(resultspunishment.getInt("id"), punishment);
            if (punishment.isActive()) {
                if (!punishmentManager.hasActivePunishment(punishment.getTargetUUID())) {
                    ArrayList<Integer> activePunishments = new ArrayList<>();
                    activePunishments.add(punishment.getId());
                    ActivePunishmentCache.put(punishment.getTargetUUID(), activePunishments);
                } else
                    ActivePunishmentCache.get(punishment.getTargetUUID()).add(punishment.getId());
            }
            //if (punishment.isPending())
            //  PendingPunishments.put(punishment.getTargetUUID(), punishment.getId());
        }
        resultspunishment.close();
        stmtpunishment.close();
    }

    public void startCaching() {
        if (cacheTask == null) {
            try {
                cache();
            } catch (PunishmentsDatabaseException pde) {
                errorHandler.log(pde);
                errorHandler.adminChatAlert(pde, plugin.getProxy().getConsole());
            }
            cacheTask = plugin.getProxy().getScheduler().schedule(plugin, () -> {
                if (PunisherPlugin.config.getBoolean("MySql.debugMode"))
                    plugin.getLogger().info(plugin.prefix + ChatColor.GREEN + "Caching Punishments...");
                try {
                    workerManager.runWorker(new WorkerManager.Worker(this::resetCache));
                } catch (Exception e) {
                    try {
                        throw new PunishmentsDatabaseException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
                    } catch (PunishmentsDatabaseException pde) {
                        errorHandler.log(pde);
                        errorHandler.adminChatAlert(pde, plugin.getProxy().getConsole());
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    public void clearCache() {
        PunishmentCache.clear();
        ActivePunishmentCache.clear();
    }

//    public void clearAllCache() {
//        clearCache();
//        punishmentManager.PendingPunishments.clear();
//    }

    public void resetCache() {
        try {
            DumpNew();
            clearCache();
            cache();
        } catch (PunishmentsDatabaseException pde) {
            errorHandler.log(pde);
            errorHandler.adminChatAlert(pde, plugin.getProxy().getConsole());
        }
    }

    // TODO: 11/03/2020 make it so that we don't cache all punishments, just the ones for online players and then cache on join too
    private void cache() throws PunishmentsDatabaseException {
        try {
            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                loadUser(player.getUniqueId().toString().replace("-", ""));
            }
        } catch (SQLException sqle) {
            throw new PunishmentsDatabaseException("Caching punishments", "CONSOLE", this.getClass().getName(), sqle);
        }
    }

    void DumpNew() throws PunishmentsDatabaseException {
        try {
            TreeMap<Integer, Punishment> PunishmentCache = new TreeMap<>(this.PunishmentCache);//to avoid concurrent modification exception if we happen to clear cache while looping over it (unlikely but could still happen)
            String sqlpunishment = "SELECT * FROM `punishments`;";
            PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
            ResultSet resultspunishment = stmtpunishment.executeQuery();
            ArrayList<Integer> exsistingIDs = new ArrayList<>();
            while (resultspunishment.next()) {
                exsistingIDs.add(resultspunishment.getInt("id"));
            }
            for (Punishment punishment : PunishmentCache.values()) {
                if (!exsistingIDs.contains(punishment.getId()))
                    try {
                        String message = punishment.getMessage();
                        if (message.contains("'"))
                            message = message.replaceAll("'", "%sinquo%");
                        if (message.contains("\""))
                            message = message.replaceAll("\"", "%dubquo%");
                        if (message.contains("`"))
                            message = message.replaceAll("`", "%bcktck%");
                        String sql1 = "INSERT INTO `punishments` (`id`, `type`, `reason`, `issueDate`, `expiration`, `targetUUID`, `targetName`, `punisherUUID`, `message`, `status`, `removerUUID`)" +
                                " VALUES ('" + punishment.getId() + "', '" + punishment.getType().toString() + "', '" + punishment.getReason().toString() + "', '" + punishment.getIssueDate() + "', '"
                                + punishment.getExpiration() + "', '" + punishment.getTargetUUID() + "', '" + punishment.getTargetName() + "', '" + punishment.getPunisherUUID() + "', '" + message
                                + "', '" + punishment.getStatus().toString() + "', '" + punishment.getRemoverUUID() + "');";
                        PreparedStatement stmt1 = connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                    } catch (SQLException sqle) {
                        throw new PunishmentsDatabaseException("Dumping Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), sqle);
                    }
            }
            resultspunishment.close();
            stmtpunishment.close();
        } catch (SQLException sqle) {
            throw new PunishmentsDatabaseException("Caching punishments", "CONSOLE", this.getClass().getName(), sqle);
        }
    }
}
