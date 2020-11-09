package com.i54m.punisher;

import com.i54m.protocol.api.injector.NettyPipelineInjector;
import com.i54m.protocol.api.listener.PlayerListener;
import com.i54m.protocol.inventory.InventoryModule;
import com.i54m.protocol.items.ItemsModule;
import com.i54m.protocol.world.WorldModule;
import com.i54m.punisher.chats.AdminChat;
import com.i54m.punisher.chats.StaffChat;
import com.i54m.punisher.commands.*;
import com.i54m.punisher.listeners.*;
import com.i54m.punisher.managers.PlayerDataManager;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.ReputationManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.managers.storage.StorageManager;
import com.i54m.punisher.managers.storage.StorageType;
import com.i54m.punisher.objects.gui.ConfirmationGUI;
import com.i54m.punisher.objects.gui.punishgui.LevelOne;
import com.i54m.punisher.objects.gui.punishgui.LevelThree;
import com.i54m.punisher.objects.gui.punishgui.LevelTwo;
import com.i54m.punisher.objects.gui.punishgui.LevelZero;
import com.i54m.punisher.utils.LuckPermsHook;
import com.i54m.punisher.utils.Permissions;
import com.i54m.punisher.utils.UpdateChecker;
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
import java.util.logging.*;
import java.util.logging.Formatter;

public class PunisherPlugin extends Plugin {

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

    /*
     * Manager instances
     */
    @Getter(AccessLevel.PUBLIC)
    private StorageManager storageManager;
    @Getter(AccessLevel.PUBLIC)
    private StorageType storageType;
    private static final PlayerDataManager PLAYER_DATA_MANAGER = PlayerDataManager.getINSTANCE();
    private static final ReputationManager REPUTATION_MANAGER = ReputationManager.getINSTANCE();
    private static final PunishmentManager PUNISHMENT_MANAGER = PunishmentManager.getINSTANCE();
    private static final WorkerManager WORKER_MANAGER = WorkerManager.getINSTANCE();

    /**
     * Staff monitoring system, this helps the plugin to track how many staff are on each server.
     * THIS DOES NOT GIVE THEM STAFF PERMS THIS IS JUST USED FOR STAFF COUNTS AND /STAFF LIST.
     */
    private final Map<ServerInfo, ArrayList<ProxiedPlayer>> staff = new HashMap<>();

    /*
     * Config variables
     *  TODO: 8/06/2020 implement new gui configs
     */
//    private File levelZeroGUIFile, levelOneGUIFile, levelTwoGUIFile, levelThreeGUIFile, confirmationGUIFile, historyGUIFile;
    private File configFile, punishmentsFile;
    @Getter
    private Configuration punishments, config;
//    @Getter
//    private Configuration levelZeroGUI, levelOneGUI, levelTwoGUI, levelThreeGUI, confirmationGUI, historyGUI;

    /*
     * Logging system variables
     */
    private FileHandler logsHandler;
    private boolean loggingStarted = false;
    @Getter
    private static final Logger LOGS = Logger.getLogger("Punisher Logs");

    /*
     * Miscellaneous variables
     */
    @Getter // TODO: 8/06/2020 only send prefix in messages that will show in chat
    private String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;
    @Getter
    private LuckPermsHook luckPermsHook;
    @Getter
    private final NettyPipelineInjector nettyPipelineInjector = new NettyPipelineInjector();
    private ScheduledTask staffFlagging;
    public final ArrayList<ServerInfo> chatOffServers = new ArrayList<>();

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
            getLogger().info("Preloading plugin...");
            //setup plugin instance
            setInstance(this);
            //initialize packet system
            initProtocol();
            getLogger().info("Plugin preloaded!");
        } catch (Exception e) {
            getLogger().severe("Unable to complete plugin preload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is the main start method which sill go through and start and setup each part of the plugin in the correct order.
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

            //preload gui menus
            setupMenus();
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
                saveDefaultConfig();
                config.set("Configversion", this.getDescription().getVersion());
                saveConfig();
            }

            //setup plugin prefixes
            prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Plugin-Wide", "&7[&cPunisher&7] "));
            StaffChat.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Staff-Chat", "&7[&cSC&7] &7[%server%] %player%: "));
            AdminChat.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.Admin-Chat", "&7[&3AC&7] &7[%server%] %player%: "));
            BroadcastCommand.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.broadcast-prefix", "&7[&3Broadcast&7] &7[%server%] %player%: "));
            SuperBroadcast.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("Plugin-prefixes.superbroadcast-prefix", "&b&lAnnouncement: &r"));
            StaffChat.color = ChatColor.valueOf(config.getString("Staff-Chat-Color", "RED"));
            AdminChat.color = ChatColor.valueOf(config.getString("Admin-Chat-Color", "AQUA"));
            BroadcastCommand.color = ChatColor.valueOf(config.getString("Broadcast-Chat-Color", "WHITE"));
            SuperBroadcast.color = ChatColor.valueOf(config.getString("SuperBroadcast-Chat-Color", "WHITE"));
            if (!config.getString("superbroadcast-title").equalsIgnoreCase("none")) {
                String[] titleargs = config.getString("superbroadcast-title").split(":");
                try {
                    SuperBroadcast.title = ProxyServer.getInstance().createTitle().subTitle(new ComponentBuilder(titleargs[0]).create())
                            .fadeIn(Integer.parseInt(titleargs[1])).stay(Integer.parseInt(titleargs[2])).fadeOut(Integer.parseInt(titleargs[3]));
                } catch (NumberFormatException nfe){
                    throw new NumberFormatException("One of your superbroadcast-title numbers is not a number! (fade in, stay or fade out");
                } catch (IndexOutOfBoundsException ioobe){
                    throw new IndexOutOfBoundsException("You have not supplied enough arguments in the superbroadcast-title!");
                }
            }

            //check for plugin update
            getLogger().info(prefix + ChatColor.GREEN + "Checking for updates...");
            if (!this.getDescription().getVersion().contains("BETA")
                    && !this.getDescription().getVersion().contains("PRE-RELEASE")
                    && !this.getDescription().getVersion().contains("DEV-BUILD")
                    && !this.getDescription().getVersion().contains("SNAPSHOT")
                    && !this.getDescription().getVersion().contains("LEGACY")) { // if not a special version name then check for updates normally
                try {
                    if (UpdateChecker.getCurrentVersion() == null) {
                        getLogger().info(prefix + ChatColor.GREEN + "Could not check for update!");
                        isUpdate = false;
                    } else if (UpdateChecker.check("${project.version}")) {
                        getLogger().warning(ChatColor.RED + "Update checker found an update, current version: " + this.getDescription().getVersion() + " latest version: " + UpdateChecker.getCurrentVersion());
                        getLogger().warning(ChatColor.RED + "This update was released on: " + UpdateChecker.getRealeaseDate());
                        getLogger().warning(ChatColor.RED + "This may fix some bugs and enhance features.");
                        getLogger().warning(ChatColor.RED + "You will no longer receive support for this version!");
                        LOGS.warning("Update checker found an update, current version: " + this.getDescription().getVersion() + " latest version: " + UpdateChecker.getCurrentVersion());
                        LOGS.warning("This update was released on: " + UpdateChecker.getRealeaseDate());
                        LOGS.warning("This may fix some bugs and enhance features.");
                        LOGS.warning("You will no longer receive support for this version!");
                        isUpdate = true;
                    } else {
                        getLogger().info(ChatColor.GREEN + "Plugin is up to date!");
                        isUpdate = false;
                    }
                } catch (Exception e) {
                    getLogger().severe(ChatColor.RED + e.getMessage());
                }
            } else if (this.getDescription().getVersion().contains("LEGACY")) {//if running a legacy version then alert user
                getLogger().warning(ChatColor.GREEN + "You are running a LEGACY version of The Punisher v2");
                getLogger().warning(ChatColor.GREEN + "This version is no longer updated with new features and ONLY MAJOR BUGS WILL BE FIXED!!");
                getLogger().warning(ChatColor.GREEN + "It is recommended that you update to the latest version to have new features.");
                getLogger().warning(ChatColor.GREEN + "Update checking is not needed in this version");
                isUpdate = false;
            } else {
                getLogger().info(ChatColor.GREEN + "You are running an unofficial release version of The Punisher v2");
                getLogger().info(ChatColor.GREEN + "Update checking is not needed in this version");
                isUpdate = false;
            }

            //check if rep on vote should be enabled
            if ((getProxy().getPluginManager().getPlugin("NuVotifier") != null || getProxy().getPluginManager().getPlugin("Votifier") != null) && config.getBoolean("Voting.addRepOnVote")) {
                getLogger().info(ChatColor.GREEN + "Enabled Rep on Vote feature!");
                getProxy().getPluginManager().registerListener(this, new PlayerVote());
            }

            //start selected storage manager, this must be started first to allow other managers to save and load data correctly
            getLogger().info(ChatColor.GREEN + "Starting Storage Manager...");
            try {
                storageType = StorageType.valueOf(config.getString("Storage-Type").toUpperCase().replace(" ", "_"));
                storageManager = storageType.getStorageManager();
            } catch (Exception e) {
                throw new IllegalArgumentException(config.getString("Storage-Type") + " is not a supported storage type, choices are one of the following: " + Arrays.toString(StorageType.values()));
            }
            storageManager.start();
            //start player data manager, this must be started next as the reputation manager depends on this for data
            getLogger().info(ChatColor.GREEN + "Starting Player Data Manager...");
            PLAYER_DATA_MANAGER.start();
            //start reputation manager, this must be started before the punishment manager as the punishment manager depends on this to alter and fetch reputation
            getLogger().info(ChatColor.GREEN + "Starting Reputation Manager...");
            REPUTATION_MANAGER.start();
            //start punishment manager
            getLogger().info(ChatColor.GREEN + "Starting Punishment Manager...");
            // TODO: 9/11/2020 make this work punManager.start();
            //start worker manager
            //this should be started last so that other managers don't start doing operations with workers when the plugin has not fully loaded
            //as this could cause errors while the server is not fully started and a player joins
            getLogger().info(ChatColor.GREEN + "Starting Worker Manager...");
            WORKER_MANAGER.start();
            //storage manager can only start caching data once we have started all the other managers up
            storageManager.startCaching();
            startStaffFlagging();

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
            //punishment manager is stopped second so that we no longer accept any new punishment activity
            if (PUNISHMENT_MANAGER.isStarted())
                PUNISHMENT_MANAGER.stop();
            //the storage manager is next so that we no longer maintain a cache or allow data loading/saving
            if (storageManager.isStarted())
                storageManager.stop();
            //the reputation manager is stopped next as it depends on the player data manager and must be stopped before it
            if (REPUTATION_MANAGER.isStarted())
                REPUTATION_MANAGER.stop();
            //the player data manager is stopped next as it is the last manager left before we stop all worker threads
            if (PLAYER_DATA_MANAGER.isStarted())
                PLAYER_DATA_MANAGER.stop();
            //the worker manager is stopped last so that we can safely stop any workers that have not yet finished
            if (WORKER_MANAGER.isStarted())
                WORKER_MANAGER.stop();

//            DiscordMain.shutdown();

            //stop logging and tidy up logs files last as we need to allow errors from shutting managers down to be logged
            File latestLogs = new File(getDataFolder() + "/logs/latest.log");
            if (latestLogs.length() != 0) {
                LOGS.info("****END OF LOGS ENDING DATE: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")) + "****");
                SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy_HH-mm-ss");
                Files.move(latestLogs.toPath(), new File(getDataFolder() + "/logs/" + format.format(Calendar.getInstance().getTime()) + ".log").toPath());
            }
            logsHandler.close();
        } catch (Exception e) {
            e.printStackTrace();//print any errors as we can no longer log to the logs file and the errors are most likely not too important and can be ignored
        }

    }

    /**
     * This method is used to initialize the required modules for the protocol side of the plugin.
     */
    private void initProtocol() {
        ProxyServer.getInstance().getPluginManager().registerListener(this, new PlayerListener(this));
        ItemsModule.initModule();
        InventoryModule.initModule();
        WorldModule.initModule();
    }

    /**
     * this method is used to setup and pre load all our gui menus.
     */
    private void setupMenus() {
        LevelZero.setupMenu();
        LevelOne.setupMenu();
        LevelTwo.setupMenu();
        LevelThree.setupMenu();
        ConfirmationGUI.setupMenu();
    }

    /**
     * This method is used to register all our event listeners.
     */
    private void registerListeners() {
        getLogger().info(prefix + ChatColor.GREEN + "Registering Listeners...");
        getProxy().getPluginManager().registerListener(this, new PlayerChat());
        getProxy().getPluginManager().registerListener(this, new PrePlayerLogin());
        getProxy().getPluginManager().registerListener(this, new PlayerVote());
        getProxy().getPluginManager().registerListener(this, new PostPlayerLogin());
        getProxy().getPluginManager().registerListener(this, new TabComplete());
        getProxy().getPluginManager().registerListener(this, new ServerConnect());
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnect());
    }

    /**
     * This method is used to register all our commands.
     */
    private void registerCommands() {
        getProxy().getPluginManager().registerCommand(this, new PunishCommand());
        getLogger().info(prefix + ChatColor.GREEN + "Registering Commands...");
        getProxy().getPluginManager().registerCommand(this, new AdminCommands());
        getProxy().getPluginManager().registerCommand(this, new AltsCommand());
        getProxy().getPluginManager().registerCommand(this, new BanCommand());
        getProxy().getPluginManager().registerCommand(this, new BroadcastCommand());
        getProxy().getPluginManager().registerCommand(this, new HistoryCommand());
        getProxy().getPluginManager().registerCommand(this, new IpCommand());
        getProxy().getPluginManager().registerCommand(this, new IpHistCommand());
        getProxy().getPluginManager().registerCommand(this, new KickAllCommand());
        getProxy().getPluginManager().registerCommand(this, new KickCommand());
        getProxy().getPluginManager().registerCommand(this, new MuteCommand());
        getProxy().getPluginManager().registerCommand(this, new NotesCommand());
        getProxy().getPluginManager().registerCommand(this, new PingCommand());
        getProxy().getPluginManager().registerCommand(this, new PlayerInfoCommand());
        getProxy().getPluginManager().registerCommand(this, new PunHelpCommand());
        getProxy().getPluginManager().registerCommand(this, new ReportCommand());
        getProxy().getPluginManager().registerCommand(this, new ReputationCommand());
        getProxy().getPluginManager().registerCommand(this, new SeenCommand());
        getProxy().getPluginManager().registerCommand(this, new StaffCommand());
        getProxy().getPluginManager().registerCommand(this, new StaffHideCommand());
        getProxy().getPluginManager().registerCommand(this, new StaffhistoryCommand());
        getProxy().getPluginManager().registerCommand(this, new SuperBroadcast());
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand());
        getProxy().getPluginManager().registerCommand(this, new UnmuteCommand());
        getProxy().getPluginManager().registerCommand(this, new UnpunishCommand());
        getProxy().getPluginManager().registerCommand(this, new viewRepCommand());
        getProxy().getPluginManager().registerCommand(this, new WarnCommand());
        //register chat commands
        getProxy().getPluginManager().registerCommand(this, new StaffChat());
        getProxy().getPluginManager().registerCommand(this, new AdminChat());
    }

    /**
     * This method loads all the config files and creates them if they don't exist.
     * @throws Exception If the config files could not be loaded.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void loadConfigs() throws Exception {
        punishmentsFile = new File(getDataFolder(), "punishments.yml");
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
                    getLogger().info(prefix + ChatColor.GREEN + "First log received!, Beginning Logging...");
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

        if (!punishmentsFile.exists()) {
            punishmentsFile.createNewFile();
            saveDefaultPunishments();
        }
        if (!configFile.exists()) {
            configFile.createNewFile();
            saveDefaultConfig();
        }
        try {
            punishments = ConfigurationProvider.getProvider(YamlConfiguration.class).load(punishmentsFile);
        } catch (YAMLException YAMLE) {
            getLogger().severe(ChatColor.RED + "Error: Could not load punishments config!");
            getLogger().severe(ChatColor.RED + "Error message: " + YAMLE.getMessage());
            throw new Exception("Could not load punishments config!", YAMLE);
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
     * This method saves the punishments config file.
     */
    public void savePunishments() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(punishments, punishmentsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method saves the default punishments to the punishments config file when it is first created.
     */
    public void saveDefaultPunishments() {
        try {
            Files.copy(getResourceAsStream("punishments.yml"), punishmentsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file", e);
        }
    }

    /**
     * This method saves the default config to the config file when it is first created.
     */
    private void saveDefaultConfig() {
        try {
            Files.copy(getResourceAsStream("config.yml"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
