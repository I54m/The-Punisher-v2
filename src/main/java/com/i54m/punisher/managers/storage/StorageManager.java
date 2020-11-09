package com.i54m.punisher.managers.storage;

import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.managers.Manager;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.ActivePunishments;
import com.i54m.punisher.objects.Punishment;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface StorageManager extends Manager {// TODO: 9/11/2020 Add Javadocs

    PunishmentManager PUNISHMENT_MANAGER = PunishmentManager.getINSTANCE();
    WorkerManager WORKER_MANAGER = WorkerManager.getINSTANCE();

    TreeMap<Integer, Punishment> PUNISHMENT_CACHE = new TreeMap<>();
    Map<UUID, ActivePunishments> ACTIVE_PUNISHMENT_CACHE = new HashMap<>();
    Map<String, Integer> PUNISHMENT_REASONS = new HashMap<>();// TODO: 9/11/2020 string = reason, int = max number of offences, all defined in punishments.yml

    default TreeMap<Integer, Punishment> getPunishmentCache() {
        return PUNISHMENT_CACHE;
    }

    default Map<UUID, ActivePunishments> getActivePunishmentCache() {
        return ACTIVE_PUNISHMENT_CACHE;
    }

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
        } catch (PunishmentsStorageException pse) {
            ERROR_HANDLER.log(pse);
            ERROR_HANDLER.adminChatAlert(pse, PLUGIN.getProxy().getConsole());
        }
    }

    default void cachePunishment(Punishment punishment) {
        if (!PUNISHMENT_CACHE.containsValue(punishment))
            PUNISHMENT_CACHE.put(punishment.getId(), punishment);
        if (punishment.isActive()) {
            ActivePunishments punishments;
            if (ACTIVE_PUNISHMENT_CACHE.get(punishment.getTargetUUID()) == null)
                punishments = new ActivePunishments();
            else
                punishments = ACTIVE_PUNISHMENT_CACHE.get(punishment.getTargetUUID());

            if (punishment.isBan() && !punishments.banActive())
                punishments.setBan(punishment);
            else if (punishment.isBan() && punishments.banActive())
                punishments.setBan(PUNISHMENT_MANAGER.override(punishment, punishments.getBan()));
            if (punishment.isMute() && !punishments.muteActive())
                punishments.setMute(punishment);
            else if (punishment.isMute() && punishments.muteActive())
                punishments.setMute(PUNISHMENT_MANAGER.override(punishment, punishments.getMute()));
            ACTIVE_PUNISHMENT_CACHE.put(punishment.getTargetUUID(), punishments);
        }
    }

    default void importPunishmentReasons() {
        PUNISHMENT_REASONS.clear();//clear incase this is called on reload, so that we don't end up with duplicate reasons
        // TODO: 9/11/2020 import punishment reasons here
    }

    void setupStorage() throws Exception;

    void startCaching();

    void cache() throws PunishmentsStorageException;

    void dumpNew() throws PunishmentsStorageException;

    void NewPunishment(@NotNull Punishment punishment);

    void updatePunishment(@NotNull Punishment punishment) throws PunishmentsStorageException;

    void createHistory(@NotNull UUID uuid) throws PunishmentsStorageException;

    void createStaffHistory(@NotNull UUID uuid) throws PunishmentsStorageException;

    void incrementHistory(@NotNull Punishment punishment) throws PunishmentsStorageException;

    void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsStorageException;

    void loadUser(@NotNull UUID uuid, boolean onlyLoadActive) throws PunishmentsStorageException;

    void updateAlts(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsStorageException;

    void updateIpHist(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsStorageException;

    Punishment getPunishmentFromId(int id) throws PunishmentsStorageException;

    int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsStorageException;

    TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsStorageException;// TODO: 9/11/2020 implement metadata applies to history

    TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsStorageException;

    ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsStorageException;

    ArrayList<UUID> getAlts(@NotNull String ip) throws PunishmentsStorageException;

    TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsStorageException;

    int getNextID();
}
