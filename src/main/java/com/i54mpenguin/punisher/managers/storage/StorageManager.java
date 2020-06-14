package com.i54mpenguin.punisher.managers.storage;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.managers.WorkerManager;
import com.i54mpenguin.punisher.objects.Punishment;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface StorageManager {

    PunisherPlugin plugin = PunisherPlugin.getInstance();
    PunishmentManager punishmentManager = PunishmentManager.getINSTANCE();
    WorkerManager workerManager = WorkerManager.getINSTANCE();
    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
    TreeMap<Integer, Punishment> PunishmentCache = new TreeMap<>();
    Map<String, ArrayList<Integer>> ActivePunishmentCache = new HashMap<>();//although this can support more than 2 active punishments at once we only allow 1 mute and 1 ban at once

    void start() throws Exception;
    void stop();

    void setupStorage() throws Exception;
    void startCaching();
    default void clearCache() {
        ActivePunishmentCache.clear();
        PunishmentCache.clear();
    }

    default void resetCache() {
        try {
            dumpNew();
            clearCache();
            cache();
        } catch (PunishmentsDatabaseException pde) {
            errorHandler.log(pde);
            errorHandler.adminChatAlert(pde, plugin.getProxy().getConsole());
        }
    }
    void cache() throws PunishmentsDatabaseException;
    void dumpNew() throws PunishmentsDatabaseException;
    void updatePunishment(@NotNull Punishment punishment) throws PunishmentsDatabaseException;

    void createHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    void createStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;

    void incrementHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException;
    void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException;

    void loadUser(@NotNull UUID uuid) throws PunishmentsDatabaseException;

    int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsDatabaseException;
    TreeMap<Integer, Punishment> getHistory(UUID uuid) throws PunishmentsDatabaseException;
    TreeMap<Integer, Punishment> getStaffHistory(UUID uuid) throws PunishmentsDatabaseException;
    ArrayList<UUID> getAlts(UUID uuid) throws PunishmentsDatabaseException;
    TreeMap<Long, String> getIpHist(UUID uuid) throws PunishmentsDatabaseException;

    void updateAlts(UUID uuid, String ip) throws PunishmentsDatabaseException;
    void updateIpHist(UUID uuid, String ip) throws PunishmentsDatabaseException;

}
