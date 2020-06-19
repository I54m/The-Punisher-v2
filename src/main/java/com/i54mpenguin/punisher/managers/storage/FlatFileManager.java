package com.i54mpenguin.punisher.managers.storage;

import com.i54mpenguin.punisher.exceptions.ManagerNotStartedException;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.WorkerManager;
import com.i54mpenguin.punisher.objects.ActivePunishments;
import com.i54mpenguin.punisher.objects.Punishment;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FlatFileManager implements StorageManager {

    @Getter
    private static final FlatFileManager INSTANCE = new FlatFileManager();
    private final File dataDir = new File(PLUGIN.getDataFolder() + "/data/");
    private final File punishmentsDir = new File(PLUGIN.getDataFolder() + "/data/punishments/");
    private final File historyDir = new File(PLUGIN.getDataFolder() + "/data/history/");
    private final File staffHistoryDir = new File(PLUGIN.getDataFolder() + "/data/staffhistory/");
    private final File iphistDir = new File(PLUGIN.getDataFolder() + "/data/iphist/");
    private final File altsDir = new File(PLUGIN.getDataFolder() + "/data/alts/");
    private Configuration mainDataConfig;

    private ScheduledTask cacheTask = null;

    private int lastPunishmentId = 0;

    private boolean locked = true;

    private FlatFileManager() {
    }

    public boolean isStarted() {
        return !locked;
    }

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
        File mainDataFile = new File(dataDir, "mainData.yml");
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
                ERROR_HANDLER.log(pde);
                ERROR_HANDLER.adminChatAlert(pde, PLUGIN.getProxy().getConsole());
            }
            cacheTask = PLUGIN.getProxy().getScheduler().schedule(PLUGIN, () -> {
                if (PLUGIN.getConfig().getBoolean("MySql.debugMode"))
                    PLUGIN.getLogger().info(PLUGIN.getPrefix() + ChatColor.GREEN + "Caching Punishments...");
                try {
                    WORKER_MANAGER.runWorker(new WorkerManager.Worker(this::resetCache));
                } catch (Exception e) {
                    try {
                        throw new PunishmentsDatabaseException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
                    } catch (PunishmentsDatabaseException pde) {
                        ERROR_HANDLER.log(pde);
                        ERROR_HANDLER.adminChatAlert(pde, PLUGIN.getProxy().getConsole());
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
        try {
            for (ProxiedPlayer player : PLUGIN.getProxy().getPlayers()) {
                loadUser(player.getUniqueId());
            }
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
        }

    }

    @Override
    public void dumpNew() throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        TreeMap<Integer, Punishment> PunishmentCache = new TreeMap<>(this.PUNISHMENT_CACHE);//to avoid concurrent modification exception if we happen to clear cache while looping over it (unlikely but could still happen)
        ArrayList<Integer> exsistingIDs = new ArrayList<>();
        for (int i = 0; i < lastPunishmentId; i++) {
            File file = new File(punishmentsDir, i + ".yml");
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
            File file = new File(punishmentsDir, punishment.getId() + ".yml");
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
        try {
            File file = new File(historyDir, uuid.toString() + ".yml");
            if (file.exists()) return;
            file.createNewFile();
            Configuration config = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
            for (String reason : PUNISHMENT_REASONS.keySet()) {
                config.set(reason, 0);
            }
            config.set("Punishments", new ArrayList<Integer>());
            YamlConfiguration.getProvider(YamlConfiguration.class).save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Creating History for: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(staffHistoryDir, uuid.toString() + ".yml");
            if (file.exists()) return;
            file.createNewFile();
            Configuration config = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
            for (String reason : PUNISHMENT_REASONS.keySet()) {
                config.set(reason, 0);
            }
            config.set("Punishments", new ArrayList<Integer>());
            YamlConfiguration.getProvider(YamlConfiguration.class).save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Creating Staff History for: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void incrementHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(historyDir, punishment.getTargetUUID().toString() + ".yml");
            if (!file.exists())
                createHistory(punishment.getTargetUUID());
            Configuration config = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
            int incremented = config.getInt(punishment.getReason(), 0) + 1;
            if (incremented > PUNISHMENT_REASONS.get(punishment.getReason()))
                incremented = PUNISHMENT_REASONS.get(punishment.getReason());
            config.set(punishment.getReason(), incremented);
            config.set("Punishments", config.getIntList("Punishments").add(punishment.getId()));
            YamlConfiguration.getProvider(YamlConfiguration.class).save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Incrementing Staff History for: " + punishment.getTargetUUID().toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(staffHistoryDir, punishment.getPunisherUUID().toString() + ".yml");
            if (!file.exists())
                createStaffHistory(punishment.getPunisherUUID());
            Configuration config = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
            config.set(punishment.getReason(), (config.getInt(punishment.getReason(), 0) + 1));
            config.set("Punishments", config.getIntList("Punishments").add(punishment.getId()));
            YamlConfiguration.getProvider(YamlConfiguration.class).save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Incrementing Staff History for: " + punishment.getPunisherUUID().toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void loadUser(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            loadPunishmentsFromFile(new File(historyDir, uuid.toString() + ".yml"));
            loadPunishmentsFromFile(new File(staffHistoryDir, uuid.toString() + ".yml"));
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Loading User: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    private void loadPunishmentsFromFile(File file) throws Exception {
        if (!file.exists()) return;
        Configuration config = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
        for (int punishmentIds : config.getIntList("Punishments")) {
            File punishmentFile = new File(punishmentsDir, punishmentIds + ".yml");
            Configuration punishmentsConfig = YamlConfiguration.getProvider(YamlConfiguration.class).load(punishmentFile);
            Punishment punishment = new Punishment(
                    punishmentsConfig.getInt("id"),
                    Punishment.Type.valueOf(punishmentsConfig.getString("type")),
                    punishmentsConfig.getString("reason"),
                    punishmentsConfig.getString("issueDate"),
                    punishmentsConfig.getLong("expiration"),
                    UUID.fromString(punishmentsConfig.getString("targetUUID")),
                    punishmentsConfig.getString("targetName"),
                    UUID.fromString(punishmentsConfig.getString("punisherUUID")),
                    punishmentsConfig.getString("message"),
                    Punishment.Status.valueOf(punishmentsConfig.getString("status")),
                    punishmentsConfig.getString("removerUUID") != null ? UUID.fromString(punishmentsConfig.getString("removerUUID")) : null);
            PUNISHMENT_CACHE.put(punishmentIds, punishment);
            if (punishment.isActive()) {
                UUID targetUUID = punishment.getTargetUUID();
                ActivePunishments punishments;
                if (ACTIVE_PUNISHMENT_CACHE.get(targetUUID) == null)
                    punishments = new ActivePunishments();
                else
                    punishments = ACTIVE_PUNISHMENT_CACHE.get(targetUUID);

                if (punishment.getType() == Punishment.Type.BAN && !punishments.banActive())
                    punishments.setBan(punishment);
                else if (punishment.getType() == Punishment.Type.MUTE && !punishments.muteActive())
                    punishments.setMute(punishment);

                ACTIVE_PUNISHMENT_CACHE.put(targetUUID, punishments);
            }
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
    public TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return null;
    }

    @Override
    public void updateAlts(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    @Override
    public void updateIpHist(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsDatabaseException {
        if (locked) {
            ErrorHandler.getINSTANCE().log(new ManagerNotStartedException(this.getClass()));
            return;
        }

    }

    private void saveMainDataConfig() {
        try {
            YamlConfiguration.getProvider(YamlConfiguration.class).save(mainDataConfig, new File(dataDir, "mainData.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
