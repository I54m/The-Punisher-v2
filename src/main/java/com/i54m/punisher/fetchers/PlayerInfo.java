package com.i54m.punisher.fetchers;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PlayerDataManager;
import com.i54m.punisher.managers.ReputationManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.managers.storage.StorageManager;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.Permissions;
import com.i54m.punisher.utils.UserFetcher;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class PlayerInfo implements Callable<Map<String, String>> {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final StorageManager storageManager = plugin.getStorageManager();
    private final ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final PlayerDataManager playerDataManager = PlayerDataManager.getINSTANCE();
    private final ReputationManager reputationManager = ReputationManager.getINSTANCE();
    private UUID targetuuid;
    private String targetName;
    private Configuration playerData;

    public void setTargetuuid(UUID targetuuid) {
        this.targetuuid = targetuuid;
        workerManager.runWorker(new WorkerManager.Worker(() -> {
            if (!playerDataManager.isPlayerDataLoaded(targetuuid))
                playerData = playerDataManager.getPlayerData(targetuuid, false);
        }));
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }


    @Override
    public Map<String, String> call() throws Exception {
        Map<String, String> info = new HashMap<>();
        info.put("uuid", targetuuid.toString());
        User user = plugin.getLuckPermsHook().getApi().getUserManager().getUser(targetName);
        if (user == null) {
            UserFetcher userFetcher = new UserFetcher();
            userFetcher.setUuid(targetuuid);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<User> userFuture = executorService.submit(userFetcher);
            try {
                user = userFuture.get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                errorHandler.log(new DataFetchException(this.getClass().getName(), "User Instance", targetName, e, "User prefix required for player info fetch, all player permission and prefix data will not be fetched!"));
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
        ArrayList<UUID> alts = storageManager.getAlts(targetuuid);
        if (alts.isEmpty())
            altslist.append("no known alts");
        else
            for (UUID alt : alts) {
                altslist.append(NameFetcher.getName(alt)).append(" ");
            }
        info.put("alts", altslist.toString());
        info.put("reputation", reputationManager.getRep(targetuuid));
        if (playerData != null) {
            info.put("firstjoin", playerData.getString("firstjoin"));
            info.put("lastserver", playerData.getString("lastserver"));
            long lastlogin = (System.currentTimeMillis() - playerData.getLong("lastlogin"));
            long lastlogout = (System.currentTimeMillis() - playerData.getLong("lastlogout"));
            String lastloginString, lastlogoutString;
            int daysago = (int) (lastlogin / (1000 * 60 * 60 * 24));
            int hoursago = (int) (lastlogin / (1000 * 60 * 60) % 24);
            int minutesago = (int) (lastlogin / (1000 * 60) % 60);
            int secondsago = (int) (lastlogin / 1000 % 60);
            if (secondsago <= 0) secondsago = 1;
            if (daysago >= 1)
                lastloginString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (hoursago >= 1) lastloginString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (minutesago >= 1) lastloginString = minutesago + "m " + secondsago + "s " + " ago";
            else lastloginString = secondsago + "s " + " ago";
            daysago = (int) (lastlogout / (1000 * 60 * 60 * 24));
            hoursago = (int) (lastlogout / (1000 * 60 * 60) % 24);
            minutesago = (int) (lastlogout / (1000 * 60) % 60);
            secondsago = (int) (lastlogout / 1000 % 60);
            if (secondsago <= 0) secondsago = 1;
            if (daysago >= 1)
                lastlogoutString = daysago + "d " + hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (hoursago >= 1) lastlogoutString = hoursago + "h " + minutesago + "m " + secondsago + "s " + " ago";
            else if (minutesago >= 1) lastlogoutString = minutesago + "m " + secondsago + "s " + " ago";
            else lastlogoutString = secondsago + "s " + " ago";
            info.put("lastlogin", lastloginString);
            info.put("lastlogout", lastlogoutString);
        }
        info.put("punishmentsreceived", String.valueOf(storageManager.getHistory(targetuuid).keySet().size()));
        info.put("punishmentsgiven", String.valueOf(storageManager.getStaffHistory(targetuuid).keySet().size()));
        return info;
    }
}
