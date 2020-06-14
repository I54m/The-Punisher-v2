package com.i54mpenguin.punisher.managers.storage;

import com.i54mpenguin.punisher.objects.Punishment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;

public interface StorageManager {

    void start() throws Exception;
    void stop();

    void setupStorage() throws Exception;
    void startCaching();
    void clearCache();
    void resetCache();
    void cache() throws Exception;
    void dumpNew() throws Exception;
    void updatePunishment(@NotNull Punishment punishment) throws Exception;

    void createHistory(@NotNull UUID uuid) throws Exception;
    void createStaffHistory(@NotNull UUID uuid) throws Exception;

    void incrementHistory(@NotNull Punishment punishment) throws Exception;
    void incrementStaffHistory(@NotNull Punishment punishment) throws Exception;

    void loadUser(@NotNull UUID uuid) throws Exception;

    int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws Exception;
    TreeMap<Integer, Punishment> getHistory(UUID uuid) throws Exception;
    TreeMap<Integer, Punishment> getStaffHistory(UUID uuid) throws Exception;
    ArrayList<UUID> getAlts(UUID uuid) throws Exception;
    TreeMap<Long, String> getIpHist(UUID uuid) throws Exception;

    void updateAlts(UUID uuid, String ip) throws Exception;
    void updateIpHist(UUID uuid, String ip) throws Exception;

}
