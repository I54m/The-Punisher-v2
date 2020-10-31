package com.i54m.punisher.managers.storage;

import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.managers.WorkerManager;
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
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FlatFileManager implements StorageManager {

    @Getter
    private static final FlatFileManager INSTANCE = new FlatFileManager();
    private final ConfigurationProvider yamlProvider = YamlConfiguration.getProvider(YamlConfiguration.class);
    private final ArrayList<String> updatingIps = new ArrayList<>();
    private File dataDir = null;
    private File punishmentsDir = null;
    private File historyDir = null;
    private File staffHistoryDir = null;
    private File iphistDir = null;
    private File altsDir = null;
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
        dataDir = new File(PLUGIN.getDataFolder() + "/data/");
        punishmentsDir = new File(PLUGIN.getDataFolder() + "/data/punishments/");
        historyDir = new File(PLUGIN.getDataFolder() + "/data/history/");
        staffHistoryDir = new File(PLUGIN.getDataFolder() + "/data/staffhistory/");
        iphistDir = new File(PLUGIN.getDataFolder() + "/data/iphist/");
        altsDir = new File(PLUGIN.getDataFolder() + "/data/alts/");
        try {
            setupStorage();
        } catch (Exception e) {
            ERROR_HANDLER.log(e);
            return;
        }
        PUNISHMENT_REASONS.put("CUSTOM", 1);
        locked = false;
        PLUGIN.getLogger().info(ChatColor.GREEN + "Started Flat File Manager!");
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
        //create main directories
        if (!dataDir.exists())
            dataDir.mkdir();
        if (!punishmentsDir.exists())
            punishmentsDir.mkdir();
        if (!historyDir.exists())
            historyDir.mkdir();
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
            } catch (PunishmentsStorageException pse) {
                ERROR_HANDLER.log(pse);
                ERROR_HANDLER.adminChatAlert(pse, PLUGIN.getProxy().getConsole());
            }
            cacheTask = PLUGIN.getProxy().getScheduler().schedule(PLUGIN, () -> {
                if (PLUGIN.getConfig().getBoolean("MySql.debugMode"))
                    PLUGIN.getLogger().info(ChatColor.GREEN + "Caching Punishments...");
                try {
                    WORKER_MANAGER.runWorker(new WorkerManager.Worker(this::resetCache));
                } catch (Exception e) {
                    try {
                        throw new PunishmentsStorageException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
                    } catch (PunishmentsStorageException pse) {
                        ERROR_HANDLER.log(pse);
                        ERROR_HANDLER.adminChatAlert(pse, PLUGIN.getProxy().getConsole());
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void cache() throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            for (ProxiedPlayer player : PLUGIN.getProxy().getPlayers()) {
                loadUser(player.getUniqueId(), false);
            }
        } catch (Exception e) {
            throw new PunishmentsStorageException("Caching punishments", "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void dumpNew() throws PunishmentsStorageException {
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
                throw new PunishmentsStorageException("Dumping Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), e);
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
        punishment.verify();
        try {
            updatePunishment(punishment);
        } catch (PunishmentsStorageException pse) {
            ERROR_HANDLER.log(pse);
            ERROR_HANDLER.adminChatAlert(pse, PLUGIN.getProxy().getConsole());
        }
    }

    @Override
    public void updatePunishment(@NotNull Punishment punishment) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        File file = null;
        Configuration config = null;
        try {
            file = new File(punishmentsDir, punishment.getId() + ".yml");
            if (!file.exists())
                file.createNewFile();
            config = yamlProvider.load(file);
            config.set("id", punishment.getId());
            config.set("type", punishment.getType().toString());
            config.set("reason", punishment.getReason());
            config.set("issueDate", punishment.getIssueDate());
            config.set("expiration", punishment.getExpiration());
            config.set("targetUUID", punishment.getTargetUUID().toString());
            config.set("targetName", punishment.getTargetName());
            config.set("punisherUUID", punishment.getPunisherUUID().toString());
            config.set("message", punishment.getMessage());
            config.set("status", punishment.getStatus().toString());
            config.set("removerUUID", punishment.getRemoverUUID() == null ? "N/A" : punishment.getRemoverUUID().toString());
            yamlProvider.save(config, file);
        } catch (Exception e) {
            try {
                yamlProvider.save(config, file);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            throw new PunishmentsStorageException("Updating Punishment: " + punishment.toString(), "CONSOLE", this.getClass().getName(), e);
        }

    }

    @Override
    public void createHistory(@NotNull UUID uuid) throws PunishmentsStorageException {
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
            throw new PunishmentsStorageException("Creating History for: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void createStaffHistory(@NotNull UUID uuid) throws PunishmentsStorageException {
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
            throw new PunishmentsStorageException("Creating Staff History for: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void incrementHistory(@NotNull Punishment punishment) throws PunishmentsStorageException {
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
            List<Integer> punishments = config.getIntList("Punishments");
            punishments.add(punishment.getId());
            config.set("Punishments", punishments);
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsStorageException("Incrementing Staff History for: " + punishment.getTargetUUID().toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void incrementStaffHistory(@NotNull Punishment punishment) throws PunishmentsStorageException {
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
            List<Integer> punishments = config.getIntList("Punishments");
            punishments.add(punishment.getId());
            config.set("Punishments", punishments);
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsStorageException("Incrementing Staff History for: " + punishment.getPunisherUUID().toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void loadUser(@NotNull UUID uuid, boolean onlyLoadActive) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        if (getActivePunishmentCache().containsKey(uuid)) return;
        try {
            loadPunishmentsFromFile(new File(historyDir, uuid.toString() + ".yml"), onlyLoadActive);
            loadPunishmentsFromFile(new File(staffHistoryDir, uuid.toString() + ".yml"), onlyLoadActive);
            for (UUID alts : getAlts(uuid)) {
                loadPunishmentsFromFile(new File(historyDir, alts.toString() + ".yml"), true);//always only load active punishments for alts as this could fill the cache very fast
            }
        } catch (Exception e) {
            throw new PunishmentsStorageException("Loading User: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
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
            if (onlyLoadActive && !punishment.isActive()) continue;
            cachePunishment(punishment);
        }
    }

    @Override
    public Punishment getPunishmentFromId(int id) throws PunishmentsStorageException {
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
                    punishmentsConfig.getString("removerUUID").equals("N/A") ? null : UUID.fromString(punishmentsConfig.getString("removerUUID")));
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting punishment from id: " + id, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public int getOffences(@NotNull UUID targetUUID, @NotNull String reason) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return 0;
        }
        if (!PUNISHMENT_REASONS.containsKey(reason))
            throw new IllegalArgumentException(reason + " is not a defined reason!!");
        try {
            File file = new File(historyDir, targetUUID.toString() + ".yml");
            if (!file.exists())
                createHistory(targetUUID);
            Configuration config = yamlProvider.load(file);
            return config.getInt(reason, 0);
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting offences on user: " + targetUUID, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Integer, Punishment> getHistory(@NotNull UUID uuid) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(historyDir, uuid.toString() + ".yml");
            if (!file.exists()) return new TreeMap<>();
            Configuration config = yamlProvider.load(file);
            TreeMap<Integer, Punishment> punishments = new TreeMap<>();
            for (int id : config.getIntList("Punishments")) {
                punishments.put(id, getPunishmentFromId(id));
            }
            return punishments;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting history on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Integer, Punishment> getStaffHistory(@NotNull UUID uuid) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(staffHistoryDir, uuid.toString() + ".yml");
            if (!file.exists()) return new TreeMap<>();
            Configuration config = yamlProvider.load(file);
            TreeMap<Integer, Punishment> punishments = new TreeMap<>();
            for (int id : config.getIntList("Punishments")) {
                punishments.put(id, getPunishmentFromId(id));
            }
            return punishments;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting staff history on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull UUID uuid) throws PunishmentsStorageException {
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
            if (!file.exists()) return new ArrayList<>();
            config = yamlProvider.load(file);
            ArrayList<String> alts = new ArrayList<>(config.getStringList("alts"));
            return UUIDFetcher.convertStringArray(alts);
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting alts on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public ArrayList<UUID> getAlts(@NotNull String ip) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(altsDir, ip + ".yml");
            if (!file.exists()) return new ArrayList<>();
            Configuration config = yamlProvider.load(file);
            ArrayList<String> alts = new ArrayList<>(config.getStringList("alts"));
            return UUIDFetcher.convertStringArray(alts);
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting alts on ip: " + ip, "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public TreeMap<Long, String> getIpHist(@NotNull UUID uuid) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            File file = new File(iphistDir, uuid.toString() + ".yml");
            if (!file.exists()) return new TreeMap<>();
            Configuration config = yamlProvider.load(file).getSection("iphist");
            TreeMap<Long, String> iphist = new TreeMap<>();
            for (String key : config.getKeys()) {
                iphist.put(Long.parseLong(key), config.getString(key));
            }
            return iphist;
        } catch (Exception e) {
            throw new PunishmentsStorageException("Getting iphist on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void updateAlts(@NotNull final UUID uuid, @NotNull String ip) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            if (ip.contains("/")) ip = ip.replace("/", "");
            if (ip.contains(":")) ip = ip.substring(0, ip.indexOf(":"));
            //get file
            File file = new File(altsDir, uuid.toString() + ".yml");
            if (!file.exists()) file.createNewFile();
            Configuration config = yamlProvider.load(file);
            String oldip = null;
            if (config.contains("ip"))
                //get old ip
                oldip = config.getString("ip");
            //set new ip
            config.set("ip", ip);
            yamlProvider.save(config, file);

            //add them to the list of accounts associated with the ip
            file = new File(altsDir, ip + ".yml");
            if (!file.exists()) file.createNewFile();
            config = yamlProvider.load(file);
            ArrayList<String> alts = new ArrayList<>(config.getStringList("alts"));
            if (!alts.contains(uuid.toString())) {
                alts.add(uuid.toString());
                config.set("alts", alts);
                yamlProvider.save(config, file);
            }

            //check if ip has changed
            if (oldip == null || ip.equals(oldip)) return;
            //if they have an old ip then remove them from being associated with that ip and update alts for all users associated with that ip if not already updating that ip
            File oldipFile = new File(altsDir, oldip + ".yml");
            if (!oldipFile.exists()) oldipFile.createNewFile();
            Configuration oldipConfig = yamlProvider.load(oldipFile);
            final ArrayList<String> oldalts = new ArrayList<>(oldipConfig.getStringList("alts"));
            oldalts.remove(uuid.toString());
            oldipConfig.set("alts", oldalts);
            yamlProvider.save(oldipConfig, oldipFile);
            //update other linked accounts
            if (!updatingIps.contains(ip)) {
                final String finalIp = ip;
                updatingIps.add(finalIp);
                WORKER_MANAGER.runWorker(new WorkerManager.Worker(() -> {
                    for (UUID oldalt : UUIDFetcher.convertStringArray(oldalts)) {
                        try {
                            updateAlts(oldalt, finalIp);
                        } catch (PunishmentsStorageException pse) {
                            ERROR_HANDLER.log(pse);
                        }
                    }
                    updatingIps.remove(finalIp);
                }));
            }
        } catch (Exception e) {
            throw new PunishmentsStorageException("Updating alts on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
        }
    }

    @Override
    public void updateIpHist(@NotNull UUID uuid, @NotNull String ip) throws PunishmentsStorageException {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        if (ip.contains("/")) ip = ip.replace("/", "");
        if (ip.contains(":")) ip = ip.substring(0, ip.indexOf(":"));
        try {
            File file = new File(iphistDir, uuid.toString() + ".yml");
            if (!file.exists()) file.createNewFile();
            Configuration config = yamlProvider.load(file);
            config.set("iphist." + System.currentTimeMillis(), ip);
            yamlProvider.save(config, file);
        } catch (Exception e) {
            throw new PunishmentsStorageException("Updating iphist on user: " + uuid.toString(), "CONSOLE", this.getClass().getName(), e);
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
