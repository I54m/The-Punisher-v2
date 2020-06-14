package com.i54mpenguin.punisher.managers.storage;

import com.i54mpenguin.punisher.exceptions.ManagerNotStartedException;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.WorkerManager;
import com.i54mpenguin.punisher.objects.Punishment;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FlatFileManager implements StorageManager {

    @Getter
    private static final  FlatFileManager INSTANCE = new FlatFileManager();

    private Configuration mainDataConfig;

    ScheduledTask cacheTask = null;

    private boolean locked = true;

    public boolean isStarted() {
        return !locked;
    }

    private FlatFileManager() {}

    @Override
    public void start() throws Exception {
        locked = false;
        setupStorage();
    }

    @Override
    public void stop() {
        locked = true;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void setupStorage() throws Exception {
        try {
            if (locked)
                throw new ManagerNotStartedException(this.getClass());
        } catch (ManagerNotStartedException mnse) {
            ErrorHandler.getINSTANCE().log(mnse);
            return;
        }
        //create main directories
        File dataDir = new File(plugin.getDataFolder(), "/data/");
        File punishmentsDir = new File(plugin.getDataFolder() + "/data/", "punishments/");
        File historyDir = new File(plugin.getDataFolder() + "/data/", "history/");
        File staffHistoryDir = new File(plugin.getDataFolder() + "/data/", "staffhistory/");
        File iphistDir = new File(plugin.getDataFolder() + "/data/", "iphist/");
        File altsDir = new File(plugin.getDataFolder() + "/data/", "alts/");
        if (!dataDir.exists())
            dataDir.mkdir();
        if (!punishmentsDir.exists())
            punishmentsDir.mkdir();
        if (!historyDir.exists())
            historyDir.exists();
        if (!staffHistoryDir.exists())
            staffHistoryDir.mkdir();
        if (!iphistDir.exists())
            iphistDir.mkdir();
        if (!altsDir.exists())
            altsDir.mkdir();
        //create main data file
        File mainDataFile = new File(plugin.getDataFolder() + "/data/", "mainData.yml");
        if (!mainDataFile.exists())
            mainDataFile.createNewFile();
        mainDataConfig = YamlConfiguration.getProvider(YamlConfiguration.class).load(mainDataFile);
        mainDataConfig.set("lastPunishmentId", 0);
        saveMainDataConfig();
    }

    @Override
    public void startCaching() {
        if (cacheTask == null) {
            try {
                cache();
            } catch (PunishmentsDatabaseException pde) {
                errorHandler.log(pde);
                errorHandler.adminChatAlert(pde, plugin.getProxy().getConsole());
            }
            cacheTask = plugin.getProxy().getScheduler().schedule(plugin, () -> {
                if (plugin.getConfig().getBoolean("MySql.debugMode"))
                    plugin.getLogger().info(plugin.getPrefix() + ChatColor.GREEN + "Caching Punishments...");
                try {
                    workerManager.runWorker(new WorkerManager.Worker(this::resetCache));
                } catch (Exception e) {
                    try {
                        throw new PunishmentsDatabaseException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
                    } catch (PunishmentsDatabaseException pde) {
                        errorHandler.log(pde);
                        errorHandler.adminChatAlert(pde, plugin.getProxy().getConsole());
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }


    @Override
    public void cache() throws PunishmentsDatabaseException {

    }

    @Override
    public void dumpNew() throws PunishmentsDatabaseException {

    }

    @Override
    public void updatePunishment(@NotNull Punishment punishment) throws PunishmentsDatabaseException {

    }

    @Override
    public void createHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {

    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {

    }

    @Override
    public void incrementHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {

    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {

    }

    @Override
    public void loadUser(@NotNull UUID uuid) throws PunishmentsDatabaseException {

    }

    @Override
    public int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsDatabaseException {
        return 0;
    }

    @Override
    public TreeMap<Integer, Punishment> getHistory(UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public ArrayList<UUID> getAlts(UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public TreeMap<Long, String> getIpHist(UUID uuid) throws PunishmentsDatabaseException {
        return null;
    }

    @Override
    public void updateAlts(UUID uuid, String ip) throws PunishmentsDatabaseException {

    }

    @Override
    public void updateIpHist(UUID uuid, String ip) throws PunishmentsDatabaseException {

    }

    private void saveMainDataConfig() {
        try{
          YamlConfiguration.getProvider(YamlConfiguration.class).save(mainDataConfig, new File(plugin.getDataFolder() + "/data/", "mainData.yml"));
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}
