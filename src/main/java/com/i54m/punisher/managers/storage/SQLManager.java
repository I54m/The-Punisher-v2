package com.i54m.punisher.managers.storage;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.objects.ActivePunishments;
import com.i54m.punisher.objects.Punishment;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class SQLManager implements StorageManager {

    @Getter(AccessLevel.PUBLIC)
    private static final SQLManager INSTANCE = new SQLManager();

    private SQLManager() {}

    public String database;
    public Connection connection;
    private String host, username, password, extraArguments;
    private int port;
    private final HikariDataSource hikari = new HikariDataSource();
    private StorageType storageType;
    
    private ScheduledTask cacheTask = null;

    private int lastPunishmentId = 0;

    private boolean locked = true;

    @Override
    public boolean isStarted() {
        return !locked;
    }


    @Override
    public void start() {
        if (!locked) {
            ERROR_HANDLER.log(new Exception("SQL Manager Already started!"));
            return;
        }
        try {
            storageType = PLUGIN.getStorageType();
            setupStorage();
        } catch (Exception e) {
            ERROR_HANDLER.log(e);
            return;
        }
        locked = false;
    }


    @Override
    public void stop() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        locked = true;
        cacheTask.cancel();
    }
    
    private void openConnection() throws Exception {
        try {
            if (connection != null && !connection.isClosed() || hikari.isRunning())
                return;
            connection = hikari.getConnection();
        } catch (SQLException e) {
            PunisherPlugin.getLOGS().severe("Error Message: " + e.getMessage());
            StringBuilder stacktrace = new StringBuilder();
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                stacktrace.append(stackTraceElement.toString()).append("\n");
            }
            PunisherPlugin.getLOGS().severe("Stack Trace: " + stacktrace.toString());
            if (e.getCause() != null)
                PunisherPlugin.getLOGS().severe("Error Cause Message: " + e.getCause().getMessage());
            ProxyServer.getInstance().getLogger().severe(" ");
            ProxyServer.getInstance().getLogger().severe(PLUGIN.getPrefix() + ChatColor.RED + "An error was encountered and debug info was logged to log file!");
            ProxyServer.getInstance().getLogger().severe(PLUGIN.getPrefix() + ChatColor.RED + "Error Message: " + e.getMessage());
            if (e.getCause() != null)
                ProxyServer.getInstance().getLogger().severe(PLUGIN.getPrefix() + ChatColor.RED + "Error Cause Message: " + e.getCause().getMessage());
            ProxyServer.getInstance().getLogger().severe(" ");
            throw new Exception(storageType.toString() + " connection failed", e);
        }
    }
    
    @Override
    public void setupStorage() throws Exception {
        host = PLUGIN.getConfig().getString("MySQL.host", "localhost");
        database = PLUGIN.getConfig().getString("MySQL.database", "punisherdb");
        username = PLUGIN.getConfig().getString("MySQL.username", "punisher");
        password = PLUGIN.getConfig().getString("MySQL.password", "punisher");
        port = PLUGIN.getConfig().getInt("MySQL.port", 3306);
        extraArguments = PLUGIN.getConfig().getString("MySQL.extraArguments", "?useSSL=false");
        switch (storageType) {
            case MY_SQL: {
                hikari.addDataSourceProperty("serverName", host);
                hikari.addDataSourceProperty("port", port);
                hikari.setPassword(password);
                hikari.setUsername(username);
                hikari.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.extraArguments);
                break;
            }
            case SQLITE: {
                File databaseFile = new File(PLUGIN.getDataFolder() + "/data/", database + ".db");
                if (!databaseFile.exists())
                    databaseFile.createNewFile();
                hikari.setDriverClassName("org.sqlite.JDBC");
                hikari.setJdbcUrl("jdbc:sqlite:" + databaseFile);
                hikari.setConnectionTestQuery("SELECT 1");
            }
        }
        //todo to enable sqlite change this
        hikari.setPoolName("The-Punisher-" + PLUGIN.getStorageType().toString());
        hikari.setMaxLifetime(60000);
        hikari.setIdleTimeout(45000);
        hikari.setMaximumPoolSize(50);
        hikari.setMinimumIdle(10);
        openConnection();
        String createdb = "SELECT 1";
        if (storageType == StorageType.MY_SQL)
            createdb = "CREATE DATABASE IF NOT EXISTS " + database;
        String punishments = "CREATE TABLE IF NOT EXISTS `" + database + "`.`punishments` ( " +
                "`ID` INT NOT NULL , " +
                "`Type` VARCHAR(4) NOT NULL , " +
                "`Reason` VARCHAR(256) NOT NULL , " +
                "`Issue_Date` VARCHAR(20) NOT NULL , " +
                "`Expiration` BIGINT NULL DEFAULT NULL , " +
                "`Target_UUID` VARCHAR(32) NOT NULL , " +
                "`Target_Name` VARCHAR(16) NOT NULL , " +
                "`Punisher_UUID` VARCHAR(32) NOT NULL , " +
                "`Message` VARCHAR(512) NULL DEFAULT NULL , " +
                "`Status` VARCHAR(10) NOT NULL , " +
                "`Remover_UUID` VARCHAR(32) NULL DEFAULT NULL , " +
                "PRIMARY KEY (`ID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        // TODO: 21/07/2020 need to redo both the history tables to allow custom reasons https://i.imgur.com/K1ib881.png
        String history = "CREATE TABLE IF NOT EXISTS`" + database + "`.`history` ( " +
                "`Punishment_ID` INT NOT NULL , " +
                "`UUID` VARCHAR(32) NOT NULL , " +
                "PRIMARY KEY (`Punishment_ID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        String staff_history = "CREATE TABLE IF NOT EXISTS`" + database + "`.`staff_history` ( " +
                "`Punishment_ID` INT NOT NULL , " +
                "`UUID` VARCHAR(32) NOT NULL , " +
                "PRIMARY KEY (`Punishment_ID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        String alt_list = "CREATE TABLE IF NOT EXISTS`" + database + "`.`alt_list` ( " +
                "`UUID` VARCHAR(32) NOT NULL , " +
                "`ip` VARCHAR(32) NOT NULL , " +
                "PRIMARY KEY (`UUID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        String ip_history = "CREATE TABLE IF NOT EXISTS`" + database + "`.`ip_history` ( " +
                "`UUID` VARCHAR(32) NOT NULL , " +
                "`date` BIGINT NOT NULL , " +
                "`ip` VARCHAR(32) NOT NULL , " +
                "PRIMARY KEY (`UUID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        String use_db = "USE " + database;
        PreparedStatement stmt = connection.prepareStatement(createdb);
        PreparedStatement stmt1 = connection.prepareStatement(punishments);
        PreparedStatement stmt2 = connection.prepareStatement(history);
        PreparedStatement stmt3 = connection.prepareStatement(staff_history);
        PreparedStatement stmt4 = connection.prepareStatement(alt_list);
        PreparedStatement stmt5 = connection.prepareStatement(ip_history);
        PreparedStatement stmt6 = connection.prepareStatement(use_db);
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
    }

    @Override
    public void startCaching() {

    }

    @Override
    public void cache() throws PunishmentsDatabaseException {

    }

    @Override
    public void dumpNew() throws PunishmentsDatabaseException {

    }










    @Override
    public TreeMap<Integer, Punishment> getPunishmentCache() {
        return null;
    }

    @Override
    public Map<UUID, ActivePunishments> getActivePunishmentCache() {
        return null;
    }

    @Override
    public Map<String, Integer> getPunishmentReasons() {
        return null;
    }

    @Override
    public void clearCache() {

    }

    @Override
    public void resetCache() {

    }

    @Override
    public void NewPunishment(@NotNull Punishment punishment) {

    }

    @Override
    public void updatePunishment(@NotNull Punishment punishment) throws PunishmentsDatabaseException {

    }

    @Override
    public void createHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {

    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {

    }

    @Override
    public void incrementHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {

    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {

    }

    @Override
    public void loadUser(@NotNull UUID uuid) throws PunishmentsDatabaseException {

    }

    @Override
    public void updateAlts(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsDatabaseException {

    }

    @Override
    public void updateIpHist(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsDatabaseException {

    }

    @Override
    public Punishment getPunishmentFromId(int id) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsDatabaseException {
        return 0;
    }

    @Override
    public TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public int getNextID() {
        return 0;
    }
}
