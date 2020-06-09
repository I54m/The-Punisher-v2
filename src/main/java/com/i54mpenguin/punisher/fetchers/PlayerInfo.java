package com.i54mpenguin.punisher.fetchers;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.chats.StaffChat;
import com.i54mpenguin.punisher.exceptions.DataFecthException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.DatabaseManager;
import com.i54mpenguin.punisher.utils.NameFetcher;
import com.i54mpenguin.punisher.utils.Permissions;
import com.i54mpenguin.punisher.utils.UUIDFetcher;
import com.i54mpenguin.punisher.utils.UserFetcher;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ChatColor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class PlayerInfo implements Callable<Map<String, String>> {

    private String targetuuid;
    private String targetName;
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();

    public void setTargetuuid(String targetuuid) {
        this.targetuuid = targetuuid;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }


    @Override
    public Map<String, String> call() throws Exception {
        Map<String, String> info = new HashMap<>();
        info.put("uuid", UUIDFetcher.formatUUID(targetuuid).toString());
        User user = plugin.getLuckPermsHook().getApi().getUserManager().getUser(targetName);
        if (user == null) {
            UserFetcher userFetcher = new UserFetcher();
            userFetcher.setUuid(UUIDFetcher.formatUUID(targetuuid));
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<User> userFuture = executorService.submit(userFetcher);
            try {
                user = userFuture.get(500, TimeUnit.MILLISECONDS);
            }catch (Exception e){
                try {
                    throw new DataFecthException("User prefix required for chat message to avoid issues the prefix was set to \"\"", targetName, "User Instance", StaffChat.class.getName(), e);
                } catch (DataFecthException dfe) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(dfe);
                }
                user = null;
            }
            executorService.shutdown();
        }
        if (user != null) {
            ContextManager cm = plugin.getLuckPermsHook().getApi().getContextManager();
            QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
            info.put("prefix", Permissions.getPrefix(user));
            CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
            int punishLevel = -1;
            if (permissionData.checkPermission("punisher.punish.level.3").asBoolean())
                punishLevel = 3;
            else if (permissionData.checkPermission("punisher.punish.level.2").asBoolean())
                punishLevel = 2;
            else if (permissionData.checkPermission("punisher.punish.level.1").asBoolean())
                punishLevel = 1;
            else if (permissionData.checkPermission("punisher.punish.level.0").asBoolean())
                punishLevel = 0;
            if (punishLevel != -1)
                info.put("punishlevel", String.valueOf(punishLevel));
            else
                info.put("punishlevel", "N/A");
            if (permissionData.checkPermission("punisher.bypass").asBoolean())
                info.put("punishmentbypass", ChatColor.GREEN + "True");
            else
                info.put("punishmentbypass", ChatColor.RED + "False");
        }
        StringBuilder altslist = new StringBuilder();
        String sql = "SELECT * FROM `altlist` WHERE UUID='" + targetuuid + "'";
        PreparedStatement stmt = dbManager.connection.prepareStatement(sql);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            String ip = results.getString("ip");
            info.put("ip", ip);
            String sql1 = "SELECT * FROM `altlist` WHERE ip='" + ip + "'";
            PreparedStatement stmt1 = dbManager.connection.prepareStatement(sql1);
            ResultSet results1 = stmt1.executeQuery();
            while (results1.next()) {
                String concacc = NameFetcher.getName(results1.getString("uuid"));
                if (concacc != null && !concacc.equals(targetName)) {
                    altslist.append(concacc).append(" ");
                }
            }
            stmt1.close();
            results1.close();
        }else if (!results.next() && altslist.toString().isEmpty())
            altslist.append("no known alts");
        info.put("alts", altslist.toString());
//        info.put("firstjoin", PunisherPlugin.playerInfoConfig.getString(targetuuid + ".firstjoin"));
//        info.put("lastserver", PunisherPlugin.playerInfoConfig.getString(targetuuid + ".lastserver"));
//        info.put("reputation", ReputationManager.getRep(targetuuid));
//        long lastlogin = (System.currentTimeMillis() - PunisherPlugin.playerInfoConfig.getLong(targetuuid + ".lastlogin"));
//        long lastlogout = (System.currentTimeMillis() - PunisherPlugin.playerInfoConfig.getLong(targetuuid + ".lastlogout"));
//        String lastloginString, lastlogoutString;
//        int daysago = (int) (lastlogin / (1000 * 60 * 60 * 24));
//        int hoursago = (int) (lastlogin / (1000 * 60 * 60) % 24);
//        int minutesago = (int) (lastlogin / (1000 * 60) % 60);
//        int secondsago = (int) (lastlogin / 1000 % 60);
//        if (secondsago <= 0) secondsago = 1;
//        if (daysago >= 1) lastloginString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
//        else if (hoursago >= 1) lastloginString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
//        else if (minutesago >= 1) lastloginString = minutesago + "m " + secondsago + "s " + " ago";
//        else lastloginString = secondsago + "s " + " ago";
//        daysago = (int) (lastlogout / (1000 * 60 * 60 * 24));
//        hoursago = (int) (lastlogout / (1000 * 60 * 60) % 24);
//        minutesago = (int) (lastlogout / (1000 * 60) % 60);
//        secondsago = (int) (lastlogout / 1000 % 60);
//        if (secondsago <= 0) secondsago = 1;
//        if (daysago >= 1) lastlogoutString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
//        else if (hoursago >= 1) lastlogoutString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
//        else if (minutesago >= 1) lastlogoutString = minutesago + "m " + secondsago + "s " + " ago";
//        else lastlogoutString = secondsago + "s " + " ago";
//        info.put("lastlogin", lastloginString);
//        info.put("lastlogout", lastlogoutString);
        String sqlhist = "SELECT * FROM `history` WHERE UUID='" + targetuuid + "'";
        PreparedStatement stmthist = dbManager.connection.prepareStatement(sqlhist);
        ResultSet resultshist = stmthist.executeQuery();
        if (resultshist.next()) {
            int punishmentsReceived = resultshist.getInt("Minor_Chat_Offence")
                    + resultshist.getInt("Major_Chat_Offence")
                    + resultshist.getInt("DDoS_DoX_Threats")
                    + resultshist.getInt("Inappropriate_Link")
                    + resultshist.getInt("Scamming")
                    + resultshist.getInt("X_Raying")
                    + resultshist.getInt("AutoClicker")
                    + resultshist.getInt("Fly_Speed_Hacking")
                    + resultshist.getInt("Malicious_PvP_Hacks")
                    + resultshist.getInt("Disallowed_Mods")
                    + resultshist.getInt("Server_Advertisement")
                    + resultshist.getInt("Greifing")
                    + resultshist.getInt("Exploiting")
                    + resultshist.getInt("Tpa_Trapping")
                    + resultshist.getInt("Impersonation")
                    + resultshist.getInt("Manual_Punishments");
            info.put("punishmentsreceived", String.valueOf(punishmentsReceived));
        }else info.put("punishmentsreceived", String.valueOf(0));
        String sqlStaffhist = "SELECT * FROM `staffhistory` WHERE UUID='" + targetuuid + "'";
        PreparedStatement stmtStaffhist = dbManager.connection.prepareStatement(sqlStaffhist);
        ResultSet resultsStaffhist = stmtStaffhist.executeQuery();
        if (resultsStaffhist.next()) {
            int punishmentsGiven = resultsStaffhist.getInt("Minor_Chat_Offence")
                    + resultsStaffhist.getInt("Major_Chat_Offence")
                    + resultsStaffhist.getInt("DDoS_DoX_Threats")
                    + resultsStaffhist.getInt("Inappropriate_Link")
                    + resultsStaffhist.getInt("Scamming")
                    + resultsStaffhist.getInt("X_Raying")
                    + resultsStaffhist.getInt("AutoClicker")
                    + resultsStaffhist.getInt("Fly_Speed_Hacking")
                    + resultsStaffhist.getInt("Malicious_PvP_Hacks")
                    + resultsStaffhist.getInt("Disallowed_Mods")
                    + resultsStaffhist.getInt("Server_Advertisement")
                    + resultsStaffhist.getInt("Greifing")
                    + resultsStaffhist.getInt("Exploiting")
                    + resultsStaffhist.getInt("Tpa_Trapping")
                    + resultsStaffhist.getInt("Impersonation")
                    + resultsStaffhist.getInt("Manual_Punishments");
            info.put("punishmentsgiven", String.valueOf(punishmentsGiven));
        }else info.put("punishmentsgiven", String.valueOf(0));
        return info;
    }
}
