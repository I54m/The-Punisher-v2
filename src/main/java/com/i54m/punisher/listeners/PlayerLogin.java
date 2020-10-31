package com.i54m.punisher.listeners;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.WorkerManager;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;

public class PlayerLogin implements Listener {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final ErrorHandler errorHandler = ErrorHandler.getINSTANCE();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(LoginEvent event) {
        PendingConnection connection = event.getConnection();
        UUID uuid = event.getConnection().getUniqueId();
        workerManager.runWorker(new WorkerManager.Worker(() -> {
            try {
                plugin.getStorageManager().updateAlts(uuid, connection.getSocketAddress().toString());
                plugin.getStorageManager().updateIpHist(uuid, connection.getSocketAddress().toString());
            } catch (PunishmentsStorageException pde) {
                errorHandler.log(pde);
                errorHandler.loginError(event);
            }
        }));


//                //update ip and make sure there is one in the database
//                String sqlip = "SELECT * FROM `altlist` WHERE UUID='" + fetcheduuid + "'";
//                PreparedStatement stmtip = dbManager.connection.prepareStatement(sqlip);
//                ResultSet resultsip = stmtip.executeQuery();
//                if (resultsip.next()) {
//                    //if their ip is not the old one then update all users with that ip to the new ip
//                    if (!resultsip.getString("ip").equals(connection.getAddress().getAddress().getHostAddress())) {
//                        String oldip = resultsip.getString("ip");
//                        String sqlipadd = "UPDATE `altlist` SET `ip`='" + connection.getAddress().getAddress().getHostAddress() + "' WHERE `ip`='" + oldip + "' ;";
//                        PreparedStatement stmtipadd = dbManager.connection.prepareStatement(sqlipadd);
//                        stmtipadd.executeUpdate();
//                        stmtipadd.close();
//                    }
//                } else {
//                    String sql1 = "INSERT INTO `altlist` (`UUID`, `ip`) VALUES ('" + fetcheduuid + "', '" + connection.getAddress().getAddress().getHostAddress() + "');";
//                    PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
//                    stmt1.executeUpdate();
//                    stmt1.close();
//                }
//                stmtip.close();
//                resultsip.close();
//            try {
//                //update ip hist
//                String sql = "SELECT * FROM `iphist` WHERE UUID='" + fetcheduuid + "'";
//                PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
//                ResultSet results = stmt.executeQuery();
//                if (results.next()) {
//                    if (!results.getString("ip").equals(connection.getAddress().getAddress().getHostAddress())) {
//                        String addip = "INSERT INTO `iphist` (`UUID`, `date`, `ip`) VALUES ('" + fetcheduuid + "', '" + System.currentTimeMillis() + "', '" + connection.getAddress().getAddress().getHostAddress() + "');";
//                        PreparedStatement addipstmt = dbManager.connection.prepareStatement(addip);
//                        addipstmt.executeUpdate();
//                        addipstmt.close();
//                    }
//                } else {
//                    String addip = "INSERT INTO `iphist` (`UUID`, `date`, `ip`) VALUES ('" + fetcheduuid + "', '" + System.currentTimeMillis() + "', '" + connection.getAddress().getAddress().getHostAddress() + "');";
//                    PreparedStatement addipstmt = dbManager.connection.prepareStatement(addip);
//                    addipstmt.executeUpdate();
//                    addipstmt.close();
//                }
//            } catch (SQLException sqle) {
//                try {
//                    String targetName = NameFetcher.getName(fetcheduuid);
//                    throw new PunishmentsStorageException("Updating ip in iphist", targetName, PlayerLogin.class.getName(), sqle);
//                } catch (PunishmentsStorageException pde) {
//                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
//                    errorHandler.log(pde);
//                    errorHandler.loginError(event);
//                }
//            }
    }
}
