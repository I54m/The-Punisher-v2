package com.i54m.punisher.managers;

import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.utils.UUIDFetcher;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The PlayerDataManager handles all player data files and caches the required ones
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@NoArgsConstructor
public class PlayerDataManager implements Listener, Manager {

    /**
     * Private manager instance with a getter method.
     */
    @Getter
    private static final PlayerDataManager INSTANCE = new PlayerDataManager();
    /**
     * Cache to keep all loaded player data configuration files.
     */
    private final Map<UUID, Configuration> playerDataCache = new HashMap<>();
    /**
     * The Main Data File to store joinids -> uuid conversion and other things needed globally.
     */
    private File mainDataFile = null;
    /**
     * Whether the manager is started or not,
     * true if the manager should be locked and no operations allowed, else false.
     */
    private boolean locked = true;
    /**
     * The join id of the last person that joined the server for the first time.
     */
    @Getter(AccessLevel.PUBLIC)
    private int lastJoinId = 0;
    /**
     * The Main Data Config to store joinids -> uuid conversion and other things needed globally.
     */
    @Getter(AccessLevel.PUBLIC)
    private Configuration mainDataConfig;

    /**
     * Whether the manager is started or not.
     *
     * @return true if the manager should be locked and no operations allowed, else false.
     */
    @Override
    public boolean isStarted() {
        return !locked;
    }

    /**
     * Used to start the PlayerDataManager and register it's listeners.
     */
    @Override
    public void start() {
        if (!locked) {
            ERROR_HANDLER.log(new Exception("Player Data Manager Already started!"));
            return;
        }
        mainDataFile = new File(PLUGIN.getDataFolder() + "/playerdata/", "MainData.yml");
        try {
            boolean newFile = false;
            if (!mainDataFile.exists()) {
                mainDataFile.createNewFile();
                newFile = true;
            }
            mainDataConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(mainDataFile);
            if (newFile) mainDataConfig.set("lastJoinID", 0);
            lastJoinId = mainDataConfig.getInt("lastJoinID", 0);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        PLUGIN.getProxy().getPluginManager().registerListener(PLUGIN, this);
        locked = false;
        PLUGIN.getLogger().info(ChatColor.GREEN + "Started Player Data Manager!");
    }

    /**
     * Used to stop the PlayerDataManager and register it's listeners.
     */
    @Override
    public void stop() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        saveMainDataConfig();
        PLUGIN.getProxy().getPluginManager().unregisterListener(this);
        locked = true;
    }

    /**
     * Used to save the main data config to file.
     */
    private void saveMainDataConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(mainDataConfig, mainDataFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * This is the event listener to load playerdata when a player joins, it uses {@link WorkerManager}
     * to run it on a separate thread so that the player's login isn't slowed down.
     *
     * @param e the LoginEvent.
     * @see WorkerManager
     * @see com.i54m.punisher.managers.WorkerManager.Worker
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(@NotNull final LoginEvent e) {
        WorkerManager.getINSTANCE().runWorker(new WorkerManager.Worker(() -> {
            UUIDFetcher uuidFetcher = new UUIDFetcher();
            uuidFetcher.fetch(e.getConnection().getName());
            try {
                UUID uuid = uuidFetcher.call();
                loadPlayerData(uuid);
            } catch (Exception pde) {
                ERROR_HANDLER.log(pde);
                ERROR_HANDLER.loginError(e);
            }
        }));
    }

    /**
     * This is the event listener to set last login details, it uses {@link WorkerManager}
     * to run it on a separate thread so that the player's login isn't slowed down.
     *
     * @param e the ServerConnectEvent
     * @see WorkerManager
     * @see com.i54m.punisher.managers.WorkerManager.Worker
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(@NotNull final ServerConnectEvent e){
        WorkerManager.getINSTANCE().runWorker(new WorkerManager.Worker(() -> {
            ProxiedPlayer player = e.getPlayer();
            Configuration playerData = getPlayerData(player, true);
            if (e.getReason().equals(ServerConnectEvent.Reason.JOIN_PROXY)) {
                playerData.set("lastLogin", System.currentTimeMillis());
                if (e.getTarget() != null)
                    playerData.set("lastServer", e.getTarget().getName());
                savePlayerData(player.getUniqueId());
            } else if (e.getTarget() != null) {
                if (player.hasPermission("punisher.staff"))
                    PLUGIN.addStaff(e.getTarget(), player);
                playerData.set("lastServer", e.getTarget().getName());
                savePlayerData(player.getUniqueId());
            }
        }));
    }

    /**
     * This is the event listener to remove playerdata from the cache when they leave and set the last logout details,
     * it uses {@link WorkerManager} to run it on a separate thread so that the player's disconnect isn't slowed down.
     *
     * @param e the DisconnectEvent.
     * @see WorkerManager
     * @see com.i54m.punisher.managers.WorkerManager.Worker
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogout(@NotNull final PlayerDisconnectEvent e) {
        WorkerManager.getINSTANCE().runWorker(new WorkerManager.Worker(() -> {
            if (e.getPlayer().hasPermission("punisher.staff"))
                PLUGIN.removeStaff(e.getPlayer());
            UUID uuid = e.getPlayer().getUniqueId();
            getPlayerData(uuid, true).set("lastLogout", System.currentTimeMillis());
            savePlayerData(uuid);
            playerDataCache.remove(uuid);
        }));
    }

    /**
     * Fetch the player's data configuration file.
     *
     * @param player the player to fetch the data for
     * @return a configuration file of the stored data
     */
    public Configuration getPlayerData(@NotNull ProxiedPlayer player, boolean create) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return getPlayerData(player.getUniqueId(), create);
    }

    /**
     * Fetch the player's data configuration file by their join id.
     *
     * @param joinID the join id of player to fetch the data for
     * @return a configuration file of the stored data
     */
    public Configuration getPlayerData(@NotNull Integer joinID) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        return getPlayerData(UUID.fromString(mainDataConfig.getString(String.valueOf(joinID))), false);
    }

    /**
     * Fetch the player's data configuration file from their uuid.
     * If the player's data is not already loaded we will load it into the cache and return it
     *
     * @param uuid the uuid of the player to fetch the data for
     * @return a configuration file of the stored data
     */
    public Configuration getPlayerData(@NotNull UUID uuid, boolean create) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        if (isPlayerDataLoaded(uuid))
            return playerDataCache.get(uuid);
        else
            if (create) return loadPlayerData(uuid);
            else return loadPlayerDataNoCreate(uuid);
    }

    /**
     * Load player's data configuration file from their uuid into the cache.
     * Use {@link #getPlayerData(UUID, boolean)} to get a player's data.
     *
     * @param uuid the uuid of the player to load the data for
     * @return a configuration file of the stored data
     */
    public Configuration loadPlayerData(@NotNull UUID uuid) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            boolean newPlayer = false;
            if (isPlayerDataLoaded(uuid)) return getPlayerData(uuid, true);
            File dataFile = new File(PLUGIN.getDataFolder() + "/playerdata/", uuid.toString() + ".yml");
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                newPlayer = true;
            }
            Configuration playerConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(dataFile);
            if (newPlayer)
                saveDefaultData(playerConfig, dataFile, uuid);
            playerDataCache.put(uuid, playerConfig);
            return playerConfig;
        } catch (IOException ioe) {
            return null;
            // TODO: 8/06/2020 data create/save exception
        }
    }

    /**
     * Load player's data configuration file from their uuid into the cache.
     * Use {@link #getPlayerData(UUID, boolean)} to get a player's data.
     *
     * @param uuid the uuid of the player to load the data for
     * @return a configuration file of the stored data
     */
    public Configuration loadPlayerDataNoCreate(@NotNull UUID uuid) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return null;
        }
        try {
            if (isPlayerDataLoaded(uuid)) return getPlayerData(uuid, false);
            File dataFile = new File(PLUGIN.getDataFolder() + "/playerdata/", uuid.toString() + ".yml");
            if (!dataFile.exists())
                return null;
            Configuration playerConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(dataFile);
            playerDataCache.put(uuid, playerConfig);
            return playerConfig;
        } catch (IOException ioe) {
            return null;
            // TODO: 8/06/2020 data create/save exception
        }
    }

    private void saveDefaultData(@NotNull Configuration config, @NotNull File dataFile, @NotNull UUID uuid) throws IOException {
        Date date = new Date();
        date.setTime(System.currentTimeMillis());
        DateFormat df = new SimpleDateFormat("dd MMM yyyy, hh:mm (Z)");
        df.setTimeZone(TimeZone.getDefault());
        config.set("firstJoin", df.format(date));
        config.set("joinId", (lastJoinId + 1));
        config.set("lastLogin", System.currentTimeMillis());
        config.set("staffHide", false);
        mainDataConfig.set("lastJoinID", (lastJoinId + 1));
        mainDataConfig.set(String.valueOf((lastJoinId + 1)), uuid.toString());
        saveMainDataConfig();
        lastJoinId++;
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, dataFile);
    }

    /**
     * Save player's data configuration file
     *
     * @param uuid uuid of the player to get the data file of
     */
    public void savePlayerData(@NotNull UUID uuid) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        try {
            if (!isPlayerDataLoaded(uuid)) return;
            File dataFile = new File(PLUGIN.getDataFolder() + "/playerdata/", uuid.toString() + ".yml");
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                return;
            }
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(playerDataCache.get(uuid), dataFile);
        } catch (IOException ioe) {
            // TODO: 8/06/2020 data create/save exception
        }
    }

    /**
     * @param uuid uuid of the player to check for a loaded data config
     * @return true if the player's data config is loaded in the cache, false if the player's data config is not loaded in the cache
     */
    public boolean isPlayerDataLoaded(@NotNull UUID uuid) {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return false;
        }
        return playerDataCache.containsKey(uuid);
    }
}
