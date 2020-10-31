package com.i54m.punisher.managers.storage;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.ActivePunishments;
import com.i54m.punisher.objects.Punishment;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SQLManager implements StorageManager {

    @Getter(AccessLevel.PUBLIC)
    private static final SQLManager INSTANCE = new SQLManager();
    private final HikariDataSource hikari = new HikariDataSource();
    public String database;
    public Connection connection;
    private String host, username, password, extraArguments;
    private int port;
    private StorageType storageType;
    private Configuration mainDataConfig;
    private ScheduledTask cacheTask = null;
    private int lastPunishmentId = 0;
    private boolean locked = true;

    private SQLManager() {
    }

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void setupStorage() throws Exception {
        //create main data file
        File dataDir = new File(PLUGIN.getDataFolder() + "/data/");
        if (!dataDir.exists())
            dataDir.mkdir();
        File mainDataFile = new File(dataDir, "mainData.yml");
        if (!mainDataFile.exists())
            mainDataFile.createNewFile();
        mainDataConfig = YamlConfiguration.getProvider(YamlConfiguration.class).load(mainDataFile);
        if (mainDataFile.length() <= 0) {
            mainDataConfig.set("lastPunishmentId", 0);
            saveMainDataConfig();
        }
        lastPunishmentId = mainDataConfig.getInt("lastPunishmentId");
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
        PreparedStatement stmt2 = connection.prepareStatement(alt_list);
        PreparedStatement stmt3 = connection.prepareStatement(ip_history);
        PreparedStatement stmt4 = connection.prepareStatement(use_db);
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
    }

    @Override
    public void startCaching() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        if (cacheTask == null) {
            try {
                cache();
            } catch (PunishmentsStorageException pse) {
                ERROR_HANDLER.log(pse);
                ERROR_HANDLER.adminChatAlert(pse, PLUGIN.getProxy().getConsole());
            }
            cacheTask = PLUGIN.getProxy().getScheduler().schedule(PLUGIN, () -> {
                if (PLUGIN.getConfig().getBoolean("MySql.debugMode"))
                    PLUGIN.getLogger().info(ChatColor.GREEN + "Caching Punishments...");
                try {
                    WORKER_MANAGER.runWorker(new WorkerManager.Worker(this::resetCache));
                } catch (Exception e) {
                    try {
                        throw new PunishmentsStorageException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
                    } catch (PunishmentsStorageException pse) {
                        ERROR_HANDLER.log(pse);
                        ERROR_HANDLER.adminChatAlert(pse, PLUGIN.getProxy().getConsole());
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void cache() throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            for (ProxiedPlayer player : PLUGIN.getProxy().getPlayers()) {
                loadUser(player.getUniqueId(), false);
            }
        } catch (Exception e) {
            throw new PunishmentsStorageException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void dumpNew() throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            TreeMap<Integer, Punishment> PunishmentCache = new TreeMap<>(PUNISHMENT_CACHE);//to avoid concurrent modification exception if we happen to clear cache while looping over it (unlikely but could still happen)
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
                        throw new PunishmentsStorageException("Dumping Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), sqle);
                    }
            }
            resultspunishment.close();
            stmtpunishment.close();
        } catch (SQLException sqle) {
            throw new PunishmentsStorageException("Caching punishments", "CONSOLE", this.getClass().getName(), sqle);
        }
    }

    @Override
    public int getNextID() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return -1;
        }
        lastPunishmentId++;
        mainDataConfig.set("lastPunishmentId", lastPunishmentId);
        saveMainDataConfig();
        return lastPunishmentId;
    }

    @Override
    public void NewPunishment(@NotNull Punishment punishment) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        String message = punishment.getMessage();
        if (message.contains("%sinquo%"))
            message = message.replaceAll("%sinquo%", "'");
        if (message.contains("%dubquo%"))
            message = message.replaceAll("%dubquo%", "\"");
        if (message.contains("%bcktck%"))
            message = message.replaceAll("%bcktck%", "`");
        punishment.setMessage(message);
        if (PUNISHMENT_MANAGER.isLocked(punishment.getTargetUUID()))
            punishment.setStatus(Punishment.Status.Overridden);
        if (!PUNISHMENT_CACHE.containsKey(punishment.getId()))
            PUNISHMENT_CACHE.put(punishment.getId(), punishment);
        try {
            updatePunishment(punishment);
        } catch (PunishmentsStorageException pse) {
            ERROR_HANDLER.log(pse);
            ERROR_HANDLER.adminChatAlert(pse, PLUGIN.getProxy().getConsole());
        }
    }

    @Override
    public void updatePunishment(@NotNull Punishment punishment) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            String message = punishment.getMessage();
            if (message.contains("'"))
                message = message.replaceAll("'", "%sinquo%");
            if (message.contains("\""))
                message = message.replaceAll("\"", "%dubquo%");
            if (message.contains("`"))
                message = message.replaceAll("`", "%bcktck%");
            String sql = "UPDATE `punishments` SET " +
                    "`type`='" + punishment.getType().toString() + "', " +
                    "`reason`='" + punishment.getReason() + "', " +
                    "`issueDate`='" + punishment.getIssueDate() + "', " +
                    "`expiration`='" + punishment.getExpiration() + "', " +
                    "`targetUUID`='" + punishment.getTargetUUID() + "', " +
                    "`targetName`='" + punishment.getTargetName() + "', " +
                    "`punisherUUID`='" + punishment.getPunisherUUID() + "', " +
                    "`message`='" + message + "', " +
                    "`status`='" + punishment.getStatus().toString() + "', " +
                    "`removerUUID`='" + punishment.getRemoverUUID() + "' " +
                    "WHERE `id`='" + punishment.getId() + "' ;";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            throw new PunishmentsStorageException("Updating Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void createHistory(@NotNull UUID uuid) throws PunishmentsStorageException {
        //these do nothing as history is done by the punishments database.
    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) throws PunishmentsStorageException {

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
    public void incrementHistory(@NotNull Punishment punishment) throws PunishmentsStorageException {

    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsStorageException {

    }

    @Override
    public void loadUser(@NotNull UUID uuid, boolean onlyLoadActive) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            loadPunishmentsFromDatabase(uuid, false);
            for (UUID alts : getAlts(uuid)) {
                loadPunishmentsFromDatabase(alts, true);
            }
        } catch (Exception e) {
            throw new PunishmentsStorageException("Loading User: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    private void loadPunishmentsFromDatabase(UUID targetUUID, boolean onlyLoadActive) throws Exception {
        if (onlyLoadActive) {
            //get all punishments where targetUUID = targetUUID & Status = ACTIVE
        } else {
            //get all punishments where targetUUID = targetUUID or punisherUUID = targetUUID
        }
    }

    @Override
    public void updateAlts(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsStorageException {

    }

    @Override
    public void updateIpHist(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsStorageException {

    }

    @Override
    public Punishment getPunishmentFromId(int id) throws PunishmentsStorageException {
        return null;
    }

    @Override
    public int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsStorageException {
        return 0;
    }

    @Override
    public TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsStorageException {
        return null;
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsStorageException {
        return null;
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsStorageException {
        return null;
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull String ip) throws PunishmentsStorageException {
        return null;
    }

    @Override
    public TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsStorageException {
        return null;
    }


    private void saveMainDataConfig() {
        try {
            YamlConfiguration.getProvider(YamlConfiguration.class).save(mainDataConfig, new File(PLUGIN.getDataFolder() + "/data/", "mainData.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
