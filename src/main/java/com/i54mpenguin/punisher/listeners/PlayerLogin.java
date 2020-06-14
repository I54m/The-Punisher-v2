package com.i54mpenguin.punisher.listeners;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.storage.DatabaseManager;
import com.i54mpenguin.punisher.managers.WorkerManager;
import com.i54mpenguin.punisher.utils.NameFetcher;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerLogin implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();

    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        PendingConnection connection = event.getConnection();
        String fetcheduuid = event.getConnection().getUniqueId().toString().replace("-", "");

        workerManager.runWorker(new WorkerManager.Worker(() -> {
            try {
                dbManager.loadUser(fetcheduuid);
            } catch (SQLException e) {
                try {
                    throw new PunishmentsDatabaseException("Loading user's punishments..", fetcheduuid, PlayerLogin.class.getName(), e);
                } catch (PunishmentsDatabaseException pde) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(pde);
                    errorHandler.loginError(event);
                }
            }
        }));
        workerManager.runWorker(new WorkerManager.Worker(() -> {
            try {
                //update ip and make sure there is one in the database
                String sqlip = "SELECT * FROM `altlist` WHERE UUID='" + fetcheduuid + "'";
                PreparedStatement stmtip = dbManager.connection.prepareStatement(sqlip);
                ResultSet resultsip = stmtip.executeQuery();
                if (resultsip.next()) {
                    if (!resultsip.getString("ip").equals(connection.getAddress().getAddress().getHostAddress())) {
                        String oldip = resultsip.getString("ip");
                        String sqlipadd = "UPDATE `altlist` SET `ip`='" + connection.getAddress().getAddress().getHostAddress() + "' WHERE `ip`='" + oldip + "' ;";
                        PreparedStatement stmtipadd = dbManager.connection.prepareStatement(sqlipadd);
                        stmtipadd.executeUpdate();
                        stmtipadd.close();
                    }
                } else {
                    String sql1 = "INSERT INTO `altlist` (`UUID`, `ip`) VALUES ('" + fetcheduuid + "', '" + connection.getAddress().getAddress().getHostAddress() + "');";
                    PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
                    stmt1.executeUpdate();
                    stmt1.close();
                }
                stmtip.close();
                resultsip.close();
            } catch (SQLException sqle) {
                try {
                    String targetName = NameFetcher.getName(fetcheduuid);
                    throw new PunishmentsDatabaseException("Updating ip in altlist", targetName, PlayerLogin.class.getName(), sqle);
                } catch (PunishmentsDatabaseException pde) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(pde);
                    errorHandler.loginError(event);
                }
            }

            try {
                //update ip hist
                String sql = "SELECT * FROM `iphist` WHERE UUID='" + fetcheduuid + "'";
                PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
                ResultSet results = stmt.executeQuery();
                if (results.next()) {
                    if (!results.getString("ip").equals(connection.getAddress().getAddress().getHostAddress())) {
                        String addip = "INSERT INTO `iphist` (`UUID`, `date`, `ip`) VALUES ('" + fetcheduuid + "', '" + System.currentTimeMillis() + "', '" + connection.getAddress().getAddress().getHostAddress() + "');";
                        PreparedStatement addipstmt = dbManager.connection.prepareStatement(addip);
                        addipstmt.executeUpdate();
                        addipstmt.close();
                    }
                } else {
                    String addip = "INSERT INTO `iphist` (`UUID`, `date`, `ip`) VALUES ('" + fetcheduuid + "', '" + System.currentTimeMillis() + "', '" + connection.getAddress().getAddress().getHostAddress() + "');";
                    PreparedStatement addipstmt = dbManager.connection.prepareStatement(addip);
                    addipstmt.executeUpdate();
                    addipstmt.close();
                }
            } catch (SQLException sqle) {
                try {
                    String targetName = NameFetcher.getName(fetcheduuid);
                    throw new PunishmentsDatabaseException("Updating ip in iphist", targetName, PlayerLogin.class.getName(), sqle);
                } catch (PunishmentsDatabaseException pde) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(pde);
                    errorHandler.loginError(event);
                }
            }
        }));
    }
}
