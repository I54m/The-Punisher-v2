package com.i54m.punisher.managers.storage;

import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.managers.Manager;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.ActivePunishments;
import com.i54m.punisher.objects.Punishment;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface StorageManager extends Manager {

    PunishmentManager PUNISHMENT_MANAGER = PunishmentManager.getINSTANCE();
    WorkerManager WORKER_MANAGER = WorkerManager.getINSTANCE();

    TreeMap<Integer, Punishment> PUNISHMENT_CACHE = new TreeMap<>();
    default TreeMap<Integer, Punishment> getPunishmentCache() {
        return PUNISHMENT_CACHE;
    }
    Map<UUID, ActivePunishments> ACTIVE_PUNISHMENT_CACHE = new HashMap<>();
    default Map<UUID, ActivePunishments> getActivePunishmentCache() {
        return ACTIVE_PUNISHMENT_CACHE;
    }
    Map<String, Integer> PUNISHMENT_REASONS = new HashMap<>();
    default Map<String, Integer> getPunishmentReasons() {
        return PUNISHMENT_REASONS;
    }

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
    void setupStorage() throws Exception;
    void startCaching();
    void cache() throws PunishmentsDatabaseException;
    void dumpNew() throws PunishmentsDatabaseException;
    void NewPunishment(@NotNull Punishment punishment);
    void updatePunishment(@NotNull Punishment punishment) throws PunishmentsDatabaseException;
    void createHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    void createStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    void incrementHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException;
    void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException;
    void loadUser(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    void updateAlts(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsDatabaseException;
    void updateIpHist(@NotNull UUID uuid, @NotNull  String ip) throws PunishmentsDatabaseException;
    Punishment getPunishmentFromId(int id) throws PunishmentsDatabaseException;
    int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsDatabaseException;
    TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    ArrayList<UUID> getAlts(@NotNull String ip) throws PunishmentsDatabaseException;
    TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsDatabaseException;
    int getNextID();
}
