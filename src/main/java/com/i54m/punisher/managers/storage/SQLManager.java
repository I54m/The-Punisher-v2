package com.i54m.punisher.managers.storage;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.UUIDFetcher;
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
    private int lastPunishmentId = 0, bansAmount = 0, mutesAmount = 0, warnsAmount = 0, kicksAmount = 0;
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
            ProxyServer.getInstance().getLogger().severe(ChatColor.RED + "An error was encountered and debug info was logged to log file!");
            ProxyServer.getInstance().getLogger().severe(ChatColor.RED + "Error Message: " + e.getMessage());
            if (e.getCause() != null)
                ProxyServer.getInstance().getLogger().severe(ChatColor.RED + "Error Cause Message: " + e.getCause().getMessage());
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
                "`Authorizer_UUID` VARCHAR(32) NULL DEFAULT NULL , " +
                "`MetaData` VARCHAR NULL DEFAULT NULL" +
                "PRIMARY KEY (`ID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        String alt_list = "CREATE TABLE IF NOT EXISTS`" + database + "`.`alt_list` ( " +
                "`UUID` VARCHAR(32) NOT NULL , " +
                "`IP` VARCHAR(32) NOT NULL , " +
                "PRIMARY KEY (`UUID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        String ip_history = "CREATE TABLE IF NOT EXISTS`" + database + "`.`ip_history` ( " +
                "`UUID` VARCHAR(32) NOT NULL , " +
                "`Date` BIGINT NOT NULL , " +
                "`IP` VARCHAR(32) NOT NULL , " +
                "PRIMARY KEY (`UUID`)) " +
                "ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
        String use_db = "USE " + database;// TODO: 1/11/2020 test if this works on sqlite
        if (storageType == StorageType.MY_SQL)
            use_db = "USE " + database;
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
        WORKER_MANAGER.runWorker(new WorkerManager.Worker(() -> {
            try {
                String sqlpunishment = "SELECT * FROM `punishments` WHERE `Type`='BAN';";
                PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
                ResultSet resultspunishment = stmtpunishment.executeQuery();
                if (resultspunishment.next()) {
                    resultspunishment.last();
                    bansAmount = resultspunishment.getRow();
                }
                resultspunishment.close();
                stmtpunishment.close();
                sqlpunishment = "SELECT * FROM `punishments` WHERE `Type`='MUTE';";
                stmtpunishment = connection.prepareStatement(sqlpunishment);
                resultspunishment = stmtpunishment.executeQuery();
                if (resultspunishment.next()) {
                    resultspunishment.last();
                    mutesAmount = resultspunishment.getRow();
                }
                resultspunishment.close();
                stmtpunishment.close();
                sqlpunishment = "SELECT * FROM `punishments` WHERE `Type`='WARN';";
                stmtpunishment = connection.prepareStatement(sqlpunishment);
                resultspunishment = stmtpunishment.executeQuery();
                if (resultspunishment.next()) {
                    resultspunishment.last();
                    warnsAmount = resultspunishment.getRow();
                }
                resultspunishment.close();
                stmtpunishment.close();
                sqlpunishment = "SELECT * FROM `punishments` WHERE `Type`='KICK';";
                stmtpunishment = connection.prepareStatement(sqlpunishment);
                resultspunishment = stmtpunishment.executeQuery();
                if (resultspunishment.next()) {
                    resultspunishment.last();
                    kicksAmount = resultspunishment.getRow();
                }
                resultspunishment.close();
                stmtpunishment.close();
            } catch (Exception e) {
                ERROR_HANDLER.log(new PunishmentsStorageException("Calculating punishment stats", "CONSOLE", this.getClass().getName(), e));
            }
        }));
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
                    NewPunishment(punishment);
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
        cachePunishment(punishment);
        punishment.verify();
        try {
            String sql1 = "INSERT INTO `punishments` (`ID`, `Type`, `Reason`, `Issue_Date`, `Expiration`, `Target_UUID`, `Target_Name`, `Punisher_UUID`, `Message`, `Status`, `Remover_UUID`)" +
                    " VALUES ('" + punishment.getId() + "', '" + punishment.getType().toString() + "', '" + punishment.getReason() + "', '" + punishment.getIssueDate() + "', '"
                    + punishment.getExpiration() + "', '" + punishment.getTargetUUID().toString() + "', '" + punishment.getTargetName() + "', '" + punishment.getPunisherUUID().toString() + "', '" + message
                    + "', '" + punishment.getStatus().toString() + "', '" + punishment.getRemoverUUID().toString() + "', '" + punishment.getAuthorizerUUID() + "', '" + punishment.getMetaData().serializeToJson() + "');";
            PreparedStatement stmt1 = connection.prepareStatement(sql1);
            stmt1.executeUpdate();
            stmt1.close();
        } catch (SQLException sqle) {
            PunishmentsStorageException pse = new PunishmentsStorageException("Adding Punishment: " + punishment.toString() + " to database.", "CONSOLE", this.getClass().getName(), sqle);
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
                    "`Type`='" + punishment.getType().toString() + "', " +
                    "`Reason`='" + punishment.getReason() + "', " +
                    "`Issue_Date`='" + punishment.getIssueDate() + "', " +
                    "`Expiration`='" + punishment.getExpiration() + "', " +
                    "`Target_UUID`='" + punishment.getTargetUUID().toString() + "', " +
                    "`Target_Name`='" + punishment.getTargetName() + "', " +
                    "`Punisher_UUID`='" + punishment.getPunisherUUID().toString() + "', " +
                    "`Message`='" + message + "', " +
                    "`Status`='" + punishment.getStatus().toString() + "', " +
                    "`Remover_UUID`='" + punishment.getRemoverUUID().toString() + "', " +
                    "`Authorizer_UUID`='" + punishment.getAuthorizerUUID() + "', " +
                    "`MetaData`='" + punishment.getMetaData().serializeToJson() + "' " +
                    "WHERE `ID`='" + punishment.getId() + "' ;";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            throw new PunishmentsStorageException("Updating Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void createHistory(@NotNull UUID uuid) {
        //No need as amount of offences is calculated from the punishments table when we fetch it.
    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) {
        //No need as amount of offences is calculated from the punishments table when we fetch it.
    }

    @Override
    public void incrementHistory(@NotNull Punishment punishment) {
        //No need as amount of offences is calculated from the punishments table when we fetch it.
    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) {
        //No need as amount of offences is calculated from the punishments table when we fetch it.
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
        String sqlpunishment;
        if (onlyLoadActive)
            sqlpunishment = "SELECT * FROM `punishments` WHERE `Target_UUID`='" + targetUUID + "' AND `Status`='Active';";
        else
            sqlpunishment = "SELECT * FROM `punishments` WHERE `Target_UUID`='" + targetUUID + "' OR `Punisher_UUID`='" + targetUUID + "';";
        PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
        ResultSet resultspunishment = stmtpunishment.executeQuery();
        while (resultspunishment.next()) {
            Punishment punishment = new Punishment(
                    resultspunishment.getInt("ID"),
                    Punishment.Type.valueOf(resultspunishment.getString("Type")),
                    resultspunishment.getString("Reason"),
                    resultspunishment.getString("Issue_Date"),
                    resultspunishment.getLong("Expiration"),
                    UUID.fromString(resultspunishment.getString("Target_UUID")),
                    resultspunishment.getString("Target_Name"),
                    UUID.fromString(resultspunishment.getString("Punisher_UUID")),
                    resultspunishment.getString("Message"),
                    Punishment.Status.valueOf(resultspunishment.getString("Status")),
                    UUID.fromString(resultspunishment.getString("Remover_UUID")),
                    UUID.fromString(resultspunishment.getString("Authorizer_UUID")),
                    Punishment.MetaData.deserializeFromJson(resultspunishment.getString("MetaData")));
            String message = punishment.getMessage();
            if (message.contains("%sinquo%"))
                message = message.replaceAll("%sinquo%", "'");
            if (message.contains("%dubquo%"))
                message = message.replaceAll("%dubquo%", "\"");
            if (message.contains("%bcktck%"))
                message = message.replaceAll("%bcktck%", "`");
            punishment.setMessage(message);
            cachePunishment(punishment);
        }
        resultspunishment.close();
        stmtpunishment.close();
    }

    @Override
    public Punishment getPunishmentFromId(int id) throws PunishmentsStorageException {
        try {
            if (PUNISHMENT_CACHE.containsKey(id)) return PUNISHMENT_CACHE.get(id);
            String sqlpunishment = "SELECT * FROM `punishments` WHERE `ID`='" + id + "';";
            PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
            ResultSet resultspunishment = stmtpunishment.executeQuery();
            Punishment punishment = new Punishment(
                    resultspunishment.getInt("ID"),
                    Punishment.Type.valueOf(resultspunishment.getString("Type")),
                    resultspunishment.getString("Reason"),
                    resultspunishment.getString("Issue_Date"),
                    resultspunishment.getLong("Expiration"),
                    UUID.fromString(resultspunishment.getString("Target_UUID")),
                    resultspunishment.getString("Target_Name"),
                    UUID.fromString(resultspunishment.getString("Punisher_UUID")),
                    resultspunishment.getString("Message"),
                    Punishment.Status.valueOf(resultspunishment.getString("Status")),
                    UUID.fromString(resultspunishment.getString("Remover_UUID")),
                    UUID.fromString(resultspunishment.getString("Authorizer_UUID")),
                    Punishment.MetaData.deserializeFromJson(resultspunishment.getString("MetaData")));
            String message = punishment.getMessage();
            if (message.contains("%sinquo%"))
                message = message.replaceAll("%sinquo%", "'");
            if (message.contains("%dubquo%"))
                message = message.replaceAll("%dubquo%", "\"");
            if (message.contains("%bcktck%"))
                message = message.replaceAll("%bcktck%", "`");
            punishment.setMessage(message);
            cachePunishment(punishment);
            resultspunishment.close();
            stmtpunishment.close();
            return punishment;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting punishment from id: " + id, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return 0;
        }
        if (!PUNISHMENT_REASONS.containsKey(reason))
            throw new IllegalArgumentException(reason + " is not a defined reason!!");
        try {
            String sqlpunishment = "SELECT * FROM `punishments` WHERE `Target_UUID`='" + targetUUID + "' AND `Reason`='" + reason + "';";
            PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
            ResultSet resultspunishment = stmtpunishment.executeQuery();
            int offences = 0;
            while (resultspunishment.next()) {
                Punishment punishment = new Punishment(
                        resultspunishment.getInt("ID"),
                        Punishment.Type.valueOf(resultspunishment.getString("Type")),
                        resultspunishment.getString("Reason"),
                        resultspunishment.getString("Issue_Date"),
                        resultspunishment.getLong("Expiration"),
                        UUID.fromString(resultspunishment.getString("Target_UUID")),
                        resultspunishment.getString("Target_Name"),
                        UUID.fromString(resultspunishment.getString("Punisher_UUID")),
                        resultspunishment.getString("Message"),
                        Punishment.Status.valueOf(resultspunishment.getString("Status")),
                        UUID.fromString(resultspunishment.getString("Remover_UUID")),
                        UUID.fromString(resultspunishment.getString("Authorizer_UUID")),
                        Punishment.MetaData.deserializeFromJson(resultspunishment.getString("MetaData")));
                String message = punishment.getMessage();
                if (message.contains("%sinquo%"))
                    message = message.replaceAll("%sinquo%", "'");
                if (message.contains("%dubquo%"))
                    message = message.replaceAll("%dubquo%", "\"");
                if (message.contains("%bcktck%"))
                    message = message.replaceAll("%bcktck%", "`");
                punishment.setMessage(message);
                cachePunishment(punishment);
                if (punishment.getMetaData().appliesToHistory())
                    offences++;
            }
            resultspunishment.close();
            stmtpunishment.close();
            return offences;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting offences on user: " + targetUUID, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Integer, Punishment> getHistory(@NotNull UUID targetUUID) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            ProxiedPlayer player = PLUGIN.getProxy().getPlayer(targetUUID);
            TreeMap<Integer, Punishment> history = new TreeMap<>();
            if (player != null && player.isConnected()) {
                for (Punishment punishment : PUNISHMENT_CACHE.values()) {
                    if (punishment.getTargetUUID().equals(targetUUID) && punishment.getMetaData().appliesToHistory())
                        history.put(punishment.getId(), punishment);
                }
            } else {
                String sqlpunishment = "SELECT * FROM `punishments` WHERE `Target_UUID`='" + targetUUID + "';";
                PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
                ResultSet resultspunishment = stmtpunishment.executeQuery();
                while (resultspunishment.next()) {
                    Punishment punishment = new Punishment(
                            resultspunishment.getInt("ID"),
                            Punishment.Type.valueOf(resultspunishment.getString("Type")),
                            resultspunishment.getString("Reason"),
                            resultspunishment.getString("Issue_Date"),
                            resultspunishment.getLong("Expiration"),
                            UUID.fromString(resultspunishment.getString("Target_UUID")),
                            resultspunishment.getString("Target_Name"),
                            UUID.fromString(resultspunishment.getString("Punisher_UUID")),
                            resultspunishment.getString("Message"),
                            Punishment.Status.valueOf(resultspunishment.getString("Status")),
                            UUID.fromString(resultspunishment.getString("Remover_UUID")),
                            UUID.fromString(resultspunishment.getString("Authorizer_UUID")),
                            Punishment.MetaData.deserializeFromJson(resultspunishment.getString("MetaData")));
                    String message = punishment.getMessage();
                    if (message.contains("%sinquo%"))
                        message = message.replaceAll("%sinquo%", "'");
                    if (message.contains("%dubquo%"))
                        message = message.replaceAll("%dubquo%", "\"");
                    if (message.contains("%bcktck%"))
                        message = message.replaceAll("%bcktck%", "`");
                    punishment.setMessage(message);
                    cachePunishment(punishment);
                    if (punishment.getMetaData().appliesToHistory())
                        history.put(punishment.getId(), punishment);
                }
                resultspunishment.close();
                stmtpunishment.close();
            }
            return history;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting history on user: " + targetUUID.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID targetUUID) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            ProxiedPlayer player = PLUGIN.getProxy().getPlayer(targetUUID);
            TreeMap<Integer, Punishment> history = new TreeMap<>();
            if (player != null && player.isConnected()) {
                for (Punishment punishment : PUNISHMENT_CACHE.values()) {
                    if (punishment.getTargetUUID().equals(targetUUID))
                        history.put(punishment.getId(), punishment);
                }
            } else {
                String sqlpunishment = "SELECT * FROM `punishments` WHERE `Punisher_UUID`='" + targetUUID + "';";
                PreparedStatement stmtpunishment = connection.prepareStatement(sqlpunishment);
                ResultSet resultspunishment = stmtpunishment.executeQuery();
                while (resultspunishment.next()) {
                    Punishment punishment = new Punishment(
                            resultspunishment.getInt("ID"),
                            Punishment.Type.valueOf(resultspunishment.getString("Type")),
                            resultspunishment.getString("Reason"),
                            resultspunishment.getString("Issue_Date"),
                            resultspunishment.getLong("Expiration"),
                            UUID.fromString(resultspunishment.getString("Target_UUID")),
                            resultspunishment.getString("Target_Name"),
                            UUID.fromString(resultspunishment.getString("Punisher_UUID")),
                            resultspunishment.getString("Message"),
                            Punishment.Status.valueOf(resultspunishment.getString("Status")),
                            UUID.fromString(resultspunishment.getString("Remover_UUID")),
                            UUID.fromString(resultspunishment.getString("Authorizer_UUID")),
                            Punishment.MetaData.deserializeFromJson(resultspunishment.getString("MetaData")));
                    String message = punishment.getMessage();
                    if (message.contains("%sinquo%"))
                        message = message.replaceAll("%sinquo%", "'");
                    if (message.contains("%dubquo%"))
                        message = message.replaceAll("%dubquo%", "\"");
                    if (message.contains("%bcktck%"))
                        message = message.replaceAll("%bcktck%", "`");
                    punishment.setMessage(message);
                    cachePunishment(punishment);
                    history.put(punishment.getId(), punishment);
                }
                resultspunishment.close();
                stmtpunishment.close();
            }
            return history;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting staff history on user: " + targetUUID.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            String sqlip = "SELECT * FROM `alt_list` WHERE UUID='" + uuid + "'";
            PreparedStatement stmtip = connection.prepareStatement(sqlip);
            ResultSet resultsip = stmtip.executeQuery();
            if (resultsip.next()) {
                String ip = resultsip.getString("IP");
                resultsip.close();
                stmtip.close();
                return getAlts(ip);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting alts on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull String ip) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            String sqlalts = "SELECT * FROM `alt_list` WHERE IP='" + ip + "'";
            PreparedStatement stmtalts = connection.prepareStatement(sqlalts);
            ResultSet resultsalts = stmtalts.executeQuery();
            ArrayList<String> alts = new ArrayList<>();
            while (resultsalts.next()) {
                alts.add(resultsalts.getString("UUID"));
            }
            resultsalts.close();
            stmtalts.close();
            return UUIDFetcher.convertStringArray(alts);
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting alts on ip: " + ip, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            String sqlip = "SELECT * FROM `ip_history` WHERE UUID='" + uuid + "'";
            PreparedStatement stmtip = connection.prepareStatement(sqlip);
            ResultSet resultsip = stmtip.executeQuery();
            TreeMap<Long, String> iphist = new TreeMap<>();
            while (resultsip.next()) {
                iphist.put(resultsip.getLong("Date"), resultsip.getString("IP"));
            }
            stmtip.close();
            resultsip.close();
            return iphist;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting iphist on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void updateAlts(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            //fetch ip
            String sqlip = "SELECT * FROM `alt_list` WHERE UUID='" + uuid + "'";
            PreparedStatement stmtip = connection.prepareStatement(sqlip);
            ResultSet resultsip = stmtip.executeQuery();
            if (resultsip.next()) { //player has an ip stored in database
                String oldip = resultsip.getString("IP");
                resultsip.close();
                stmtip.close();
                //check if ip has changed from stored ip
                if (oldip.equals(ip)) return;
                //update all records with oldip to the new ip (this includes the player we checked's record)
                String sqlipadd = "UPDATE `alt_list` SET `IP`='" + ip + "' WHERE `IP`='" + oldip + "' ;";
                PreparedStatement stmtipadd = connection.prepareStatement(sqlipadd);
                stmtipadd.executeUpdate();
                stmtipadd.close();
            } else {//no ip found in database, so we add them to the database with the new ip
                String sql1 = "INSERT INTO `alt_list` (`UUID`, `IP`) VALUES ('" + uuid + "', '" + ip + "');";
                PreparedStatement stmt1 = connection.prepareStatement(sql1);
                stmt1.executeUpdate();
                stmt1.close();
            }
        } catch (Exception e) {
            throw new PunishmentsStorageException("Updating alts on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void updateIpHist(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            //check for current ip
            String sql = "SELECT * FROM `ip_history` WHERE UUID='" + uuid + "'";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet results = stmt.executeQuery();
            if (results.next())//if current ip we check if it's changed
                if (results.getString("ip").equals(ip))
                    return;//if ip has not changed there is no need to update ip history
            //add new iphistory record
            String addip = "INSERT INTO `ip_history` (`UUID`, `Date`, `IP`) VALUES ('" + uuid + "', '" + System.currentTimeMillis() + "', '" + ip + "');";
            PreparedStatement addipstmt = connection.prepareStatement(addip);
            addipstmt.executeUpdate();
            addipstmt.close();
        } catch (Exception e) {
            throw new PunishmentsStorageException("Updating iphist on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    private void saveMainDataConfig() {
        try {
            YamlConfiguration.getProvider(YamlConfiguration.class).save(mainDataConfig, new File(PLUGIN.getDataFolder() + "/data/", "mainData.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    @Override
    public int getPunishmentsAmount() {
        return lastPunishmentId + 1;
    }

    @Override
    public int getBansAmount() {
        return bansAmount;
    }

    @Override
    public int getMutesAmount() {
        return mutesAmount;
    }

    @Override
    public int getWarnsAmount() {
        return warnsAmount;
    }

    @Override
    public int getKicksAmount() {
        return kicksAmount;
    }


}
