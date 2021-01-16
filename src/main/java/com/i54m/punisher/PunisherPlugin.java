package com.i54m.punisher;

import com.i54m.punisher.chats.AdminChat;
import com.i54m.punisher.chats.StaffChat;
import com.i54m.punisher.commands.*;
import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.listeners.PlayerChat;
import com.i54m.punisher.listeners.PostPlayerLogin;
import com.i54m.punisher.listeners.PrePlayerLogin;
import com.i54m.punisher.listeners.TabComplete;
import com.i54m.punisher.managers.PlayerDataManager;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.managers.storage.FlatFileManager;
import com.i54m.punisher.managers.storage.StorageManager;
import com.i54m.punisher.utils.LuckPermsHook;
import com.i54m.punisher.utils.Permissions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Formatter;
import java.util.logging.*;

public class PunisherPlugin extends Plugin {

    private static final PlayerDataManager PLAYER_DATA_MANAGER = PlayerDataManager.getINSTANCE();
    private static final PunishmentManager PUNISHMENT_MANAGER = PunishmentManager.getINSTANCE();
    private static final WorkerManager WORKER_MANAGER = WorkerManager.getINSTANCE();
    @Getter
    private static final Logger LOGS = Logger.getLogger("Punisher Logs");
    /*
     * Startup Variables
     */
    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static PunisherPlugin instance;
    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static boolean isUpdate = false;
    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static boolean isLoaded = false;
    public final ArrayList<ServerInfo> chatOffServers = new ArrayList<>();
    /*
     * Manager instances
     */
    @Getter(AccessLevel.PUBLIC)
    private final StorageManager storageManager = FlatFileManager.getINSTANCE();
    /**
     * Staff monitoring system, this helps the plugin to track how many staff are on each server.
     * THIS DOES NOT GIVE THEM STAFF PERMS THIS IS JUST USED FOR STAFF COUNTS AND /STAFF LIST.
     */
    private final Map<ServerInfo, ArrayList<ProxiedPlayer>> staff = new HashMap<>();
    /*
     * Config variables
     */
    private File configFile;
    @Getter
    private Configuration config;
    public File discordIntegrationFile = new File(getDataFolder() + "/data", "discordData.yml");
    /*
     * Logging system variables
     */
    private FileHandler logsHandler;
    private boolean loggingStarted = false;
    /*
     * Miscellaneous variables
     */
    @Getter // TODO: 8/06/2020 only send prefix in messages that will show in chat
    private String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;
    @Getter
    private LuckPermsHook luckPermsHook;
    private ScheduledTask staffFlagging;

    /**
     * @param server server to get a staff list for
     * @return a list of staff on that server
     */
    public ArrayList<ProxiedPlayer> getStaff(ServerInfo server) {
        if (staff.containsKey(server)) return staff.get(server);
        else return new ArrayList<>();
    }

    /**
     * @return a list of servers that have staff on them
     */
    public Set<ServerInfo> getStaffServers() {
        return staff.keySet();
    }

    /**
     * @param server server to add a staff member to
     * @param player player to flag as staff on that server
     */
    public void addStaff(ServerInfo server, ProxiedPlayer player) {
        removeStaff(player);
        ArrayList<ProxiedPlayer> staff = new ArrayList<>(this.staff.get(server));
        staff.remove(player);
        staff.add(player);
        this.staff.put(server, staff);
    }

    /**
     * @param player player to remove as being flagged as a staff member on all servers
     */
    public void removeStaff(ProxiedPlayer player) {
        for (ServerInfo server : getProxy().getServers().values()) {
            if (!this.staff.get(server).isEmpty() && this.staff.get(server).contains(player)) {
                ArrayList<ProxiedPlayer> staff = this.staff.get(server);
                staff.remove(player);
                this.staff.put(server, staff);
            }
        }
    }


    /**
     * This method is used to pre load the protocol side of the plugin and setup the plugin instance.
     */
    @Override
    public void onLoad() {
        try {
            getLogger().info(ChatColor.GREEN + "Preloading plugin...");
            //setup plugin instance
            setInstance(this);
            //initialize packet system
            getLogger().info(ChatColor.GREEN + "Plugin preloaded!");
        } catch (Exception e) {
            getLogger().severe(ChatColor.RED + "Unable to complete plugin preload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is the main start method which will go through and start and setup each part of the plugin in the correct order.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onEnable() {
        try {
            //record start time
            long startTime = System.nanoTime();

            //check for required dependencies and hook into any that are found
            luckPermsHook = new LuckPermsHook().hook();
            if (luckPermsHook == null) {
                getLogger().warning(ChatColor.RED + "Luck Perms not detected, Plugin has been Disabled!");
                getProxy().getPluginManager().unregisterCommands(this);
                getProxy().getPluginManager().unregisterListeners(this);
                return;
            }
            //initialize permissions module and hooks
            Permissions.init();

            //register commands
            registerCommands();
            //register event listeners
            registerListeners();

            //load all configs and create them if they don't exist
            getLogger().info(ChatColor.GREEN + "Loading Configs...");
            loadConfigs();
            //check main config version and rename to old_config.yml if it is outdated
            if (!config.getString("Configversion").equals(this.getDescription().getVersion())) {
                getLogger().warning(ChatColor.RED + "Old config.yml detected!");
                getLogger().warning(ChatColor.RED + "Renaming old config to old_config.yml!");
                File newfile = new File(getDataFolder(), "old_config.yml");
                try {
                    Files.move(configFile.toPath(), newfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    configFile.createNewFile();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                saveDefaultConfig();// TODO: 6/08/2020 not correctly copying defaults into new config file
                config.set("Configversion", this.getDescription().getVersion());
                saveConfig();
            }

            //setup plugin prefixes
            prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Plugin-Wide", "&7[&cPunisher&7] "));
            StaffChat.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Staff-Chat", "&7[&cSC&7] &7[%server%] %player%: "));
            AdminChat.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Admin-Chat", "&7[&3AC&7] &7[%server%] %player%: "));
            StaffChat.prefixRaw = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Staff-Chat-Raw", "&7[&cSC&7] "));
            AdminChat.prefixRaw = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Admin-Chat-Raw", "&7[&3AC&7] "));
            SuperBroadcast.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.superbroadcast-prefix", "&b&lAnnouncement: &r"));
            StaffChat.color = ChatColor.of(config.getString("Staff-Chat-Color", "RED"));
            AdminChat.color = ChatColor.of(config.getString("Admin-Chat-Color", "AQUA"));
            SuperBroadcast.color = ChatColor.of(config.getString("SuperBroadcast-Chat-Color", "WHITE"));
            if (!config.getString("superbroadcast-title").equalsIgnoreCase("none")) {
                String[] titleargs = config.getString("superbroadcast-title").split(":");
                try {
                    SuperBroadcast.title = ProxyServer.getInstance().createTitle().subTitle(new ComponentBuilder(titleargs[0]).create())
                            .fadeIn(Integer.parseInt(titleargs[1])).stay(Integer.parseInt(titleargs[2])).fadeOut(Integer.parseInt(titleargs[3]));
                } catch (NumberFormatException nfe) {
                    throw new NumberFormatException("One of your superbroadcast-title numbers is not a number! (fade in, stay or fade out");
                } catch (IndexOutOfBoundsException ioobe) {
                    throw new IndexOutOfBoundsException("You have not supplied enough arguments in the superbroadcast-title!");
                }
            }

            //start selected storage manager, this must be started first to allow other managers to save and load data correctly
            getLogger().info(ChatColor.GREEN + "Starting Storage Manager...");
            storageManager.start();
            //start player data manager, this must be started next as the reputation manager depends on this for data
            getLogger().info(ChatColor.GREEN + "Starting Player Data Manager...");
            PLAYER_DATA_MANAGER.start();
            //start punishment manager
            getLogger().info(ChatColor.GREEN + "Starting Punishment Manager...");
            PUNISHMENT_MANAGER.start();
            //start worker manager
            //this should be started last so that other managers don't start doing operations with workers when the plugin has not fully loaded
            //as this could cause errors while the server is not fully started and a player joins
            getLogger().info(ChatColor.GREEN + "Starting Worker Manager...");
            WORKER_MANAGER.start();
            //storage manager can only start caching data once we have started all the other managers up
            storageManager.startCaching();
            startStaffFlagging();

            //check if discord integration is enabled
            if (config.getBoolean("DiscordIntegration.Enabled"))
                DiscordMain.startBot();

            //plugin loading/enabling is now complete so we announce it
            setLoaded(true);
            long duration = (System.nanoTime() - startTime) / 1000000;
            getLogger().info(ChatColor.GREEN + "Successfully enabled The Punisher v" + this.getDescription().getVersion() + " By I54m (took " + duration + "ms)");
        } catch (Exception e) {
            //if we get any errors on startup we shutdown and alert the user
            onDisable();
            getLogger().severe(ChatColor.RED + "Could not enable The Punisher v" + this.getDescription().getVersion() + "!!");
            if (e.getCause() != null)
                getLogger().severe(ChatColor.RED + "Error cause: " + e.getCause());
            if (e.getMessage() != null)
                getLogger().severe(ChatColor.RED + "Error message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is the shutdown method and is used when we reload the plugin in {@link AdminCommands} as well as when the server shuts down.
     */
    @Override
    public void onDisable() {
        try {
            if (staffFlagging != null)
                staffFlagging.cancel();
            //unregister all commands and listeners in an attempt to limit operations that use the managers.
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
            if (config.getBoolean("DiscordIntegration.Enabled"))
                DiscordMain.shutdown();
            //punishment manager is stopped second so that we no longer accept any new punishment activity
            if (PUNISHMENT_MANAGER.isStarted())
                PUNISHMENT_MANAGER.stop();
            //the storage manager is next so that we no longer maintain a cache or allow data loading/saving
            if (storageManager.isStarted())
                storageManager.stop();
            //the player data manager is stopped next as it is the last manager left before we stop all worker threads
            if (PLAYER_DATA_MANAGER.isStarted())
                PLAYER_DATA_MANAGER.stop();
            //the worker manager is stopped last so that we can safely stop any workers that have not yet finished
            if (WORKER_MANAGER.isStarted())
                WORKER_MANAGER.stop();

            //stop logging and tidy up logs files last as we need to allow errors from shutting managers down to be logged
            File latestLogs = new File(getDataFolder() + "/logs/latest.log");
            if (latestLogs.length() != 0) {
                LOGS.info("****END OF LOGS ENDING DATE: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")) + "****");
                SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy_HH-mm-ss");
                logsHandler.close();
                Files.move(latestLogs.toPath(), new File(getDataFolder() + "/logs/" + format.format(Calendar.getInstance().getTime()) + ".log").toPath());
            } else logsHandler.close();
        } catch (Exception e) {
            e.printStackTrace();//print any errors as we can no longer log to the logs file and the errors are most likely not too important and can be ignored
        }

    }

    /**
     * This method is used to register all our event listeners.
     */
    private void registerListeners() {
        getLogger().info(ChatColor.GREEN + "Registering Listeners...");
        getProxy().getPluginManager().registerListener(this, new PlayerChat());
        getProxy().getPluginManager().registerListener(this, new PostPlayerLogin());
        getProxy().getPluginManager().registerListener(this, new PrePlayerLogin());
        getProxy().getPluginManager().registerListener(this, new TabComplete());
    }

    /**
     * This method is used to register all our commands.
     */
    private void registerCommands() {
        getLogger().info(ChatColor.GREEN + "Registering Commands...");
        getProxy().getPluginManager().registerCommand(this, new AdminCommands());
        getProxy().getPluginManager().registerCommand(this, new AltsCommand());
        getProxy().getPluginManager().registerCommand(this, new BanCommand());
        getProxy().getPluginManager().registerCommand(this, new ClearChatCommand());
        getProxy().getPluginManager().registerCommand(this, new HistoryCommand());
        getProxy().getPluginManager().registerCommand(this, new IpCommand());
        getProxy().getPluginManager().registerCommand(this, new IpHistCommand());
        getProxy().getPluginManager().registerCommand(this, new KickAllCommand());
        getProxy().getPluginManager().registerCommand(this, new KickCommand());
        getProxy().getPluginManager().registerCommand(this, new MuteCommand());
        getProxy().getPluginManager().registerCommand(this, new PingCommand());
        getProxy().getPluginManager().registerCommand(this, new PunHelpCommand());
        getProxy().getPluginManager().registerCommand(this, new SeenCommand());
        getProxy().getPluginManager().registerCommand(this, new StaffCommand());
        getProxy().getPluginManager().registerCommand(this, new StaffHideCommand());
        getProxy().getPluginManager().registerCommand(this, new StaffhistoryCommand());
        getProxy().getPluginManager().registerCommand(this, new SuperBroadcast());
        getProxy().getPluginManager().registerCommand(this, new ToggleChatCommand());
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand());
        getProxy().getPluginManager().registerCommand(this, new UnmuteCommand());
        getProxy().getPluginManager().registerCommand(this, new WarnCommand());
        //register chat commands
        getProxy().getPluginManager().registerCommand(this, new AdminChat());
        getProxy().getPluginManager().registerCommand(this, new StaffChat());
    }

    /**
     * This method loads all the config files and creates them if they don't exist.
     *
     * @throws Exception If the config files could not be loaded.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void loadConfigs() throws Exception {
        configFile = new File(getDataFolder(), "config.yml");
        File logsDir = new File(getDataFolder() + "/logs/");
        File playerDataDir = new File(getDataFolder() + "/playerdata/");
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        if (!logsDir.exists())
            logsDir.mkdir();
        if (!playerDataDir.exists())
            playerDataDir.mkdir();
        logsHandler = new FileHandler(getDataFolder() + "/logs/latest.log");
        logsHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                if (!loggingStarted) {
                    //start logging to logs file
                    loggingStarted = true;
                    getLogger().info(ChatColor.GREEN + "First log received!, Beginning Logging...");
                    LOGS.info("****START OF LOGS BEGINNING DATE: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")) + "****");
                }
                if (record.getLevel() != Level.SEVERE)
                    getLogger().log(record);
                SimpleDateFormat logTime = new SimpleDateFormat("dd-MMM HH:mm:ss");
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(record.getMillis());
                return "\n[" + record.getLevel() + "] "
                        + "[" + logTime.format(cal.getTime())
                        + "]: "
                        + record.getMessage();
            }
        });
        LOGS.setUseParentHandlers(false);
        LOGS.addHandler(logsHandler);

        if (!configFile.exists()) {
            configFile.createNewFile();
            saveDefaultConfig();
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (YAMLException YAMLE) {
            getLogger().severe(ChatColor.RED + "Error: Could not load main config!");
            getLogger().severe(ChatColor.RED + "Error message: " + YAMLE.getMessage());
            throw new Exception("Could not load main config!", YAMLE);
        }
    }

    /**
     * This method saves the main config file.
     */
    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method saves the default config to the config file when it is first created.
     */
    private void saveDefaultConfig() {
        try {
            Files.copy(getResourceAsStream("bungeeconfig.yml"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file", e);
        }
    }

    private void startStaffFlagging() {
        staff.clear();
        for (ServerInfo servers : getProxy().getServers().values()) {
            ArrayList<ProxiedPlayer> staffPlayers = new ArrayList<>();
            for (ProxiedPlayer players : getProxy().getPlayers()) {
                if (players.hasPermission("punisher.staff") &&
                        players.isConnected() &&
                        players.getServer() != null &&
                        players.getServer().getInfo().equals(servers))
                    staffPlayers.add(players);
            }
            staff.put(servers, staffPlayers);
        }
        staffFlagging = getProxy().getScheduler().schedule(this, () -> WORKER_MANAGER.runWorker(new WorkerManager.Worker(() -> {
            staff.clear();
            for (ServerInfo servers : getProxy().getServers().values()) {
                ArrayList<ProxiedPlayer> staffPlayers = new ArrayList<>();
                for (ProxiedPlayer players : getProxy().getPlayers()) {
                    if (players.hasPermission("punisher.staff") &&
                            players.isConnected() &&
                            players.getServer() != null &&
                            players.getServer().getInfo().equals(servers))
                        staffPlayers.add(players);
                }
                staff.put(servers, staffPlayers);
            }
        })), 5, 3, TimeUnit.SECONDS);
    }

}