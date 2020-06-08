package com.i54mpenguin.punisher.managers;

import com.i54mpenguin.punisher.PunisherPlugin;
import lombok.Getter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The PlayerDataManager handles all player data requests and data fetching
 */
public class PlayerDataManager {

    /**
     * Private manager instance with a getter method.
     */
    @Getter
    private static final PlayerDataManager INSTANCE = new PlayerDataManager();

    /**
     * Main plugin instance.
     */
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    /**
     * Cache to keep all loaded player data configuration files.
     */
    private final Map<UUID, Configuration> playerDataCache = new HashMap<>();

    /**
     * Private manager constructor
     */
    private PlayerDataManager() {}

    /**
     * Fetch the player's data configuration file.
     *
     * @param player the player to fetch the data for
     * @return a configuration file of the stored data
     */
    public Configuration getPlayerData(ProxiedPlayer player) {
        return getPlayerData(player.getUniqueId());
    }

    /**
     * Fetch the player's data configuration file from their uuid.
     * If the player's data is not already loaded we will load it into the cache and return it
     *
     * @param uuid the uuid of the player to fetch the data for
     * @return a configuration file of the stored data
     */
    public Configuration getPlayerData(UUID uuid) {
        if (isPlayerDataLoaded(uuid))
            return playerDataCache.get(uuid);
        else
            return loadPlayerData(uuid);
    }

    /**
     * Load player's data configuration file from their uuid into the cache.
     * Use {@link #getPlayerData(UUID)} to get a player's data as this does not check the cache.
     *
     * @param uuid the uuid of the player to load the data for
     * @return a configuration file of the stored data
     */
    public Configuration loadPlayerData(UUID uuid) {
        try {
            File dataFile = new File(plugin.getDataFolder() + "/playerdata/", uuid.toString() + ".yml");
            if (!dataFile.exists())
                dataFile.createNewFile();
            Configuration playerConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(dataFile);
            playerDataCache.put(uuid, playerConfig);
            return playerConfig;
        } catch (IOException ioe) {
            return null;
            // TODO: 8/06/2020 data create/save exception
        }
    }

    /**
     * Save player's data configuration file
     *
     * @param uuid uuid of the player to get the data file of
     */
    public void savePlayerData(UUID uuid) {
        try {
            if (!isPlayerDataLoaded(uuid)) return;
            File dataFile = new File(plugin.getDataFolder() + "/playerdata/", uuid.toString() + ".yml");
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
    public boolean isPlayerDataLoaded(UUID uuid) {
        return playerDataCache.containsKey(uuid);
    }
}
