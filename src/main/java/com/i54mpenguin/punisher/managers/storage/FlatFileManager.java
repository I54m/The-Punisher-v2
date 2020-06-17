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

    private ScheduledTask cacheTask = null;

    private int lastPunishmentId = 0;

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
        if (mainDataFile.length() <= 0) {
            mainDataConfig.set("lastPunishmentId", 0);
            saveMainDataConfig();
        }
        lastPunishmentId = mainDataConfig.getInt("lastPunishmentId");
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
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public void dumpNew() throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        TreeMap<Integer, Punishment> PunishmentCache = new TreeMap<>(this.PunishmentCache);//to avoid concurrent modification exception if we happen to clear cache while looping over it (unlikely but could still happen)
        ArrayList<Integer> exsistingIDs = new ArrayList<>();
        for (int i = 0; i < lastPunishmentId; i ++) {
            File file = new File(plugin.getDataFolder() + "/data/punishments/", i + ".yml");
            if (file.exists()) exsistingIDs.add(i);
        }
        PunishmentCache.values().removeIf((value) -> !exsistingIDs.contains(value.getId()));
        for (Punishment punishment : PunishmentCache.values()) {
                try {
                    updatePunishment(punishment);
                } catch (Exception e) {
                    throw new PunishmentsDatabaseException("Dumping Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), e);
                }
        }
    }

    @Override
    public void updatePunishment(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(plugin.getDataFolder() + "/data/punishments/", punishment.getId() + ".yml");
            if (!file.exists())
                file.createNewFile();
            Configuration config = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
            config.set("id", punishment.getId());
            config.set("type", punishment.getType().toString());
            config.set("reason", punishment.getReason());
            config.set("issueDate", punishment.getIssueDate());
            config.set("expiration", punishment.getExpiration());
            config.set("targetUUID", punishment.getTargetUUID().toString());
            config.set("targetName", punishment.getTargetName());
            config.set("punisherUUID", punishment.getPunisherUUID());
            config.set("message", punishment.getMessage());
            config.set("status", punishment.getStatus());
            config.set("removerUUID", punishment.getRemoverUUID());
            YamlConfiguration.getProvider(YamlConfiguration.class).save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Updating Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), e);
        }

    }

    @Override
    public void createHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public void incrementHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public void loadUser(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return 0;
        }
        return 0;
    }

    @Override
    public TreeMap<Integer, Punishment> getHistory(UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public ArrayList<UUID> getAlts(UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public TreeMap<Long, String> getIpHist(UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public void updateAlts(UUID uuid, String ip) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public void updateIpHist(UUID uuid, String ip) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    private void saveMainDataConfig() {
        try {
          YamlConfiguration.getProvider(YamlConfiguration.class).save(mainDataConfig, new File(plugin.getDataFolder() + "/data/", "mainData.yml"));
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}
