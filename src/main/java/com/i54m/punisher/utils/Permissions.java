package com.i54m.punisher.utils;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.DataFetchException;
import com.i54m.punisher.handlers.ErrorHandler;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Permissions {

    private static LuckPerms LUCKPERMS_API;

    public static void init() {
//        try {
//            Class.forName("org.bukkit.Bukkit");
//            //bukkit
//            LUCKPERMS_API = PunisherBukkit.getInstance().luckPermsHook.getApi();
//        } catch (ClassNotFoundException cnfe){
        //bungeecord
        LUCKPERMS_API = PunisherPlugin.getInstance().getLuckPermsHook().getApi();
//        }
    }

    public static boolean higher(ProxiedPlayer player, UUID targetuuid) throws IllegalArgumentException {
        if (player.getUniqueId().equals(UUIDFetcher.getBLANK_UUID())) return true;
        else if (targetuuid.equals(UUIDFetcher.getBLANK_UUID())) return false;
        int playerlevel = getPermissionLvl(player);
        int targetlevel = getPermissionLvl(getUser(targetuuid));
        if (player.hasPermission("punisher.bypass") && playerlevel <= targetlevel)
            return true;
        return playerlevel > targetlevel;
    }

//    public static boolean higher(Player player, User user) throws IllegalArgumentException {
//        if (user == null) throw new IllegalArgumentException("User cannot be null!!");
//        ContextManager cm = LUCKPERMS_API.getContextManager();
//        QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
//        CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
//        int playerlevel = 0;
//        int targetlevel = 0;
//        for (int i = 0; i <= 3; i++){
//            if (player.hasPermission("punisher.punish.level." + i))
//                playerlevel = i;
//            if (permissionData.checkPermission("punisher.punish.level." + i).asBoolean())
//                targetlevel = i;
//        }
//        if (player.hasPermission("punisher.bypass") && playerlevel <= targetlevel)
//            return true;
//        return playerlevel > targetlevel;
//    }
//
//    public static int getPermissionLvl(Player player){
//        int playerlevel = 0;
//        for (int i = 0; i <= 3; i++) {
//            if (player.hasPermission("punisher.punish.level." + i))
//                playerlevel = i;
//        }
//        return playerlevel;
//    }

    public static int getPermissionLvl(ProxiedPlayer player) {
        int playerlevel = 0;
        for (int i = 0; i <= 3; i++) {
            if (player.hasPermission("punisher.punish.level." + i))
                playerlevel = i;
        }
        return playerlevel;
    }

    public static int getPermissionLvl(User user) {
        ContextManager cm = LUCKPERMS_API.getContextManager();
        QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
        CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
        int playerlevel = 0;
        for (int i = 0; i <= 3; i++) {
            if (permissionData.checkPermission("punisher.punish.level." + i).asBoolean())
                playerlevel = i;
        }
        return playerlevel;
    }

    public static String getPrefix(UUID targetUUID) {
        User user = LUCKPERMS_API.getUserManager().getUser(targetUUID);
        if (user == null) {
            UserFetcher userFetcher = new UserFetcher();
            userFetcher.setUuid(targetUUID);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<User> userFuture = executorService.submit(userFetcher);
            try {
                user = userFuture.get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(new DataFetchException(Permissions.class.getName(), "User Instance", NameFetcher.getName(targetUUID), e, "User prefix required for prefix request, to avoid issues the prefix was set to \"\""));
                return "";
            }
            executorService.shutdown();
        }
        return getPrefix(user);
    }


    public static String getPrefix(User user) {
        if (user != null) {
            ContextManager cm = LUCKPERMS_API.getContextManager();
            QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
            return user.getCachedData().getMetaData(queryOptions).getPrefix() == null ? "" : user.getCachedData().getMetaData(queryOptions).getPrefix();
        } else return "";
    }

    public static boolean higher(UUID player1UUID, UUID player2UUID) {
        if (player1UUID.equals(UUIDFetcher.getBLANK_UUID())) return true;
        else if (player2UUID.equals(UUIDFetcher.getBLANK_UUID())) return false;
        User user1 = getUser(player1UUID);
        ContextManager cm = LUCKPERMS_API.getContextManager();
        QueryOptions queryOptions1 = cm.getQueryOptions(user1).orElse(cm.getStaticQueryOptions());
        CachedPermissionData permissionData1 = user1.getCachedData().getPermissionData(queryOptions1);
        int player1level = getPermissionLvl(user1);
        int player2level = getPermissionLvl(getUser(player2UUID));
        if (permissionData1.checkPermission("punisher.bypass").asBoolean() && player1level <= player2level)
            return true;
        return player1level > player2level;
    }

    public static User getUser(UUID uuid) {
        User user = LUCKPERMS_API.getUserManager().getUser(uuid);
        if (user == null) {
            UserFetcher userFetcher = new UserFetcher();
            userFetcher.setUuid(uuid);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<User> userFuture = executorService.submit(userFetcher);
            try {
                user = userFuture.get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                DataFetchException dfe = new DataFetchException(Permissions.class.getName(), "User Instance", NameFetcher.getName(uuid), e, "User instance required for User fetching");
                errorHandler.log(dfe);
                errorHandler.alert(dfe, PunisherPlugin.getInstance().getProxy().getConsole());
                user = null;
            }
            executorService.shutdown();
            if (user == null) throw new IllegalArgumentException("User instance cannot be null!!");
        }
        return user;
    }
}
