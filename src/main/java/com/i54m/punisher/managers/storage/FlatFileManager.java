package com.i54m.punisher.managers.storage;

import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.PunishmentsDatabaseException;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.ActivePunishments;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.UUIDFetcher;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
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
    private final ConfigurationProvider yamlProvider = YamlConfiguration.getProvider(YamlConfiguration.class);
    private Configuration mainDataConfig;

    private ScheduledTask cacheTask = null;

    private int lastPunishmentId = 0;

    private boolean locked = true;

    private FlatFileManager() {
    }

    @Override
    public boolean isStarted() {
        return !locked;
    }

    @Override
    public void start() {
        if (!locked) {
            ERROR_HANDLER.log(new Exception("Flat File Manager Already started!"));
            return;
        }
        try {
            setupStorage();
        } catch (Exception e) {
            ERROR_HANDLER.log(e);
            return;
        }
        locked = false;
    }

    @Override
    public void stop() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        locked = true;
        cacheTask.cancel();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void setupStorage() throws Exception {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
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
        mainDataConfig = yamlProvider.load(mainDataFile);
        if (mainDataFile.length() <= 0) {
            mainDataConfig.set("lastPunishmentId", 0);
            saveMainDataConfig();
        }
        lastPunishmentId = mainDataConfig.getInt("lastPunishmentId");
    }

    @Override
    public void startCaching() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        if (cacheTask == null) {
            try {
                cache();
            } catch (PunishmentsDatabaseException pde) {
                ERROR_HANDLER.log(pde);
                ERROR_HANDLER.adminChatAlert(pde, PLUGIN.getProxy().getConsole());
            }
            cacheTask = PLUGIN.getProxy().getScheduler().schedule(PLUGIN, () -> {
                if (PLUGIN.getConfig().getBoolean("MySql.debugMode"))
                    PLUGIN.getLogger().info(ChatColor.GREEN + "Caching Punishments...");
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
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
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
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
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
    public int getNextID() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return -1;
        }
        lastPunishmentId++;
        mainDataConfig.set("lastPunishmentId", lastPunishmentId);
        saveMainDataConfig();
        return lastPunishmentId;
    }

    @Override
    public void NewPunishment(@NotNull Punishment punishment) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        String message = punishment.getMessage();
        if (message.contains("%sinquo%"))
            message = message.replaceAll("%sinquo%", "'");
        if (message.contains("%dubquo%"))
            message = message.replaceAll("%dubquo%", "\"");
        if (message.contains("%bcktck%"))
            message = message.replaceAll("%bcktck%", "`");
        punishment.setMessage(message);
        if (PUNISHMENT_MANAGER.isLocked(punishment.getTargetUUID()))
            punishment.setStatus(Punishment.Status.Overridden);
        if (!PUNISHMENT_CACHE.containsKey(punishment.getId()))
            PUNISHMENT_CACHE.put(punishment.getId(), punishment);
        try {
            updatePunishment(punishment);
        } catch (PunishmentsDatabaseException pde) {
            ERROR_HANDLER.log(pde);
            ERROR_HANDLER.adminChatAlert(pde, PLUGIN.getProxy().getConsole());
        }
    }

    @Override
    public void updatePunishment(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(punishmentsDir, punishment.getId() + ".yml");
            if (!file.exists())
                file.createNewFile();
            Configuration config = yamlProvider.load(file);
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
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Updating Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), e);
        }

    }

    @Override
    public void createHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(historyDir, uuid.toString() + ".yml");
            if (file.exists()) return;
            file.createNewFile();
            Configuration config = yamlProvider.load(file);
            for (String reason : PUNISHMENT_REASONS.keySet()) {
                config.set(reason, 0);
            }
            config.set("Punishments", new ArrayList<Integer>());
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Creating History for: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(staffHistoryDir, uuid.toString() + ".yml");
            if (file.exists()) return;
            file.createNewFile();
            Configuration config = yamlProvider.load(file);
            for (String reason : PUNISHMENT_REASONS.keySet()) {
                config.set(reason, 0);
            }
            config.set("Punishments", new ArrayList<Integer>());
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Creating Staff History for: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void incrementHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(historyDir, punishment.getTargetUUID().toString() + ".yml");
            if (!file.exists())
                createHistory(punishment.getTargetUUID());
            Configuration config = yamlProvider.load(file);
            int incremented = config.getInt(punishment.getReason(), 0) + 1;
            if (incremented > PUNISHMENT_REASONS.get(punishment.getReason()))
                incremented = PUNISHMENT_REASONS.get(punishment.getReason());
            config.set(punishment.getReason(), incremented);
            config.set("Punishments", config.getIntList("Punishments").add(punishment.getId()));
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Incrementing Staff History for: " + punishment.getTargetUUID().toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(staffHistoryDir, punishment.getPunisherUUID().toString() + ".yml");
            if (!file.exists())
                createStaffHistory(punishment.getPunisherUUID());
            Configuration config = yamlProvider.load(file);
            config.set(punishment.getReason(), (config.getInt(punishment.getReason(), 0) + 1));
            config.set("Punishments", config.getIntList("Punishments").add(punishment.getId()));
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Incrementing Staff History for: " + punishment.getPunisherUUID().toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void loadUser(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            loadPunishmentsFromFile(new File(historyDir, uuid.toString() + ".yml"), false);
            loadPunishmentsFromFile(new File(staffHistoryDir, uuid.toString() + ".yml"), false);
            for (UUID alts : getAlts(uuid)) {
                loadPunishmentsFromFile(new File(historyDir, alts.toString() + ".yml"), true);
            }
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Loading User: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    private void loadPunishmentsFromFile(File file, boolean onlyLoadActive) throws Exception {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        if (!file.exists()) return;
        Configuration config = yamlProvider.load(file);
        for (int punishmentIds : config.getIntList("Punishments")) {
            Punishment punishment = getPunishmentFromId(punishmentIds);
            if (!onlyLoadActive)
                PUNISHMENT_CACHE.put(punishmentIds, punishment);
            if (punishment.isActive()) {
                UUID targetUUID = punishment.getTargetUUID();
                ActivePunishments punishments;
                if (ACTIVE_PUNISHMENT_CACHE.get(targetUUID) == null)
                    punishments = new ActivePunishments();
                else
                    punishments = ACTIVE_PUNISHMENT_CACHE.get(targetUUID);

                if (punishment.isBan() && !punishments.banActive())
                    punishments.setBan(punishment);
                else if (punishment.isBan() && punishments.banActive())
                    punishments.setBan(PUNISHMENT_MANAGER.override(punishment, punishments.getBan()));
                if (punishment.isMute() && !punishments.muteActive())
                    punishments.setMute(punishment);
                else if (punishment.isMute() && punishments.muteActive())
                    punishments.setMute(PUNISHMENT_MANAGER.override(punishment, punishments.getMute()));
                ACTIVE_PUNISHMENT_CACHE.put(targetUUID, punishments);
            }
        }
    }

    @Override
    public Punishment getPunishmentFromId(int id) throws PunishmentsDatabaseException {
        try {
            if (PUNISHMENT_CACHE.containsKey(id)) return PUNISHMENT_CACHE.get(id);
            File punishmentFile = new File(punishmentsDir, id + ".yml");
            Configuration punishmentsConfig = yamlProvider.load(punishmentFile);
            return new Punishment(
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
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Getting punishment from id: " + id, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return 0;
        }
        if (!PUNISHMENT_REASONS.containsKey(reason)) throw new IllegalArgumentException(reason + " is not a defined reason!!");
        try {
            File file = new File(historyDir, targetUUID.toString() + ".yml");
            if (!file.exists())
                createHistory(targetUUID);
            Configuration config = yamlProvider.load(file);
            return config.getInt(reason, 0);
        } catch (Exception e){
            throw new PunishmentsDatabaseException("Getting offences on user: " + targetUUID, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(historyDir, uuid.toString() + ".yml");
            Configuration config = yamlProvider.load(file);
            TreeMap<Integer, Punishment> punishments = new TreeMap<>();
            for (int id : config.getIntList("Punishments")){
                punishments.put(id, getPunishmentFromId(id));
            }
            return punishments;
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Getting history on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(staffHistoryDir, uuid.toString() + ".yml");
            Configuration config = yamlProvider.load(file);
            TreeMap<Integer, Punishment> punishments = new TreeMap<>();
            for (int id : config.getIntList("Punishments")){
                punishments.put(id, getPunishmentFromId(id));
            }
            return punishments;
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Getting staff history on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(altsDir, uuid.toString() + ".yml");
            if (!file.exists()) return new ArrayList<>();
            Configuration config = yamlProvider.load(file);
            String ip = config.getString("ip");
            file = new File(altsDir, ip + ".yml");
            config = yamlProvider.load(file);
            ArrayList<String> alts = new ArrayList<>(config.getStringList("alts"));
            return UUIDFetcher.convertStringArray(alts);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Getting alts on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(iphistDir, uuid.toString() + ".yml");
            if (!file.exists()) return new TreeMap<>();
            Configuration config = yamlProvider.load(file).getSection("iphist");
            TreeMap<Long, String> iphist = new TreeMap<>();
            for (String key : config.getKeys()){
                iphist.put(Long.parseLong(key), config.getString(key));
            }
            return iphist;
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Getting iphist on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    private final ArrayList<String> updatingIps = new ArrayList<>();
    @Override
    public void updateAlts(@NotNull final UUID uuid, @NotNull final String ip) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            //get old ip
            File file = new File(altsDir, uuid.toString() + ".yml");
            Configuration config = yamlProvider.load(file);

            //if they have an old ip then remove them from being associated with that ip and update alts for all users associated with that ip if not already updating that ip
            String oldip = config.getString("ip");
            if (oldip != null) {
                File oldipFile = new File(altsDir, oldip + ".yml");
                Configuration oldipConfig = yamlProvider.load(oldipFile);
                final ArrayList<String> oldalts = new ArrayList<>(oldipConfig.getStringList("alts"));
                oldalts.remove(uuid.toString());
                oldipConfig.set("alts", oldalts);
                yamlProvider.save(oldipConfig, oldipFile);
                if (!updatingIps.contains(ip)) {
                    updatingIps.add(ip);
                    WORKER_MANAGER.runWorker(new WorkerManager.Worker(() -> {
                        for (UUID oldalt : UUIDFetcher.convertStringArray(oldalts)) {
                            try {
                                updateAlts(oldalt, ip);
                            } catch (PunishmentsDatabaseException pde) {
                                ERROR_HANDLER.log(pde);
                            }
                        }
                        updatingIps.remove(ip);
                    }));
                }
            }

            //set new ip in their data file
            if (!file.exists()) file.createNewFile();
            config = yamlProvider.load(file);
            config.set("ip", ip);
            yamlProvider.save(config, file);

            //add them to the list of accounts associated with the new ip
            file = new File(altsDir, ip + ".yml");
            if (!file.exists()) file.createNewFile();
            config = yamlProvider.load(file);
            ArrayList<String> alts = new ArrayList<>(config.getStringList("alts"));
            alts.add(uuid.toString());
            config.set("alts", alts);
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Updating alts on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void updateIpHist(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsDatabaseException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            File file = new File(iphistDir, uuid.toString() + ".yml");
            if (!file.exists()) file.createNewFile();
            Configuration config = yamlProvider.load(file);
            config.set("iphist." + System.currentTimeMillis(), ip);
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsDatabaseException("Updating iphist on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    private void saveMainDataConfig() {
        try {
            yamlProvider.save(mainDataConfig, new File(dataDir, "mainData.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
