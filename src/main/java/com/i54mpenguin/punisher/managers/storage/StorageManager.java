package com.i54mpenguin.punisher.managers.storage;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.managers.WorkerManager;
import com.i54mpenguin.punisher.objects.ActivePunishments;
import com.i54mpenguin.punisher.objects.Punishment;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface StorageManager {

    PunisherPlugin PLUGIN = PunisherPlugin.getInstance();
    PunishmentManager PUNISHMENT_MANAGER = PunishmentManager.getINSTANCE();
    WorkerManager WORKER_MANAGER = WorkerManager.getINSTANCE();
    ErrorHandler ERROR_HANDLER = ErrorHandler.getINSTANCE();
    TreeMap<Integer, Punishment> PUNISHMENT_CACHE = new TreeMap<>();
    Map<UUID, ActivePunishments> ACTIVE_PUNISHMENT_CACHE = new HashMap<>();
    Map<String, Integer> PUNISHMENT_REASONS = new HashMap<>();

    void start() throws Exception;
    void stop();

    void setupStorage() throws Exception;

    void startCaching();
    default void clearCache() {
        ACTIVE_PUNISHMENT_CACHE.clear();
        PUNISHMENT_CACHE.clear();
    }
    default void resetCache() {
        try {
            dumpNew();
            clearCache();
            cache();
        } catch (PunishmentsDatabaseException pde) {
            ERROR_HANDLER.log(pde);
            ERROR_HANDLER.adminChatAlert(pde, PLUGIN.getProxy().getConsole());
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
    TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsDatabaseException;

    void updateAlts(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsDatabaseException;
    void updateIpHist(@NotNull UUID uuid, @NotNull  String ip) throws PunishmentsDatabaseException;

}
