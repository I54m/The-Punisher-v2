package com.i54mpenguin.punisher;

import com.i54mpenguin.protocol.api.injector.NettyPipelineInjector;
import com.i54mpenguin.protocol.api.listener.PlayerListener;
import com.i54mpenguin.protocol.inventory.InventoryModule;
import com.i54mpenguin.protocol.items.ItemsModule;
import com.i54mpenguin.punisher.chats.AdminChat;
import com.i54mpenguin.punisher.chats.StaffChat;
import com.i54mpenguin.punisher.commands.*;
import com.i54mpenguin.punisher.listeners.*;
import com.i54mpenguin.punisher.managers.DatabaseManager;
import com.i54mpenguin.punisher.managers.PlayerDataManager;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.managers.WorkerManager;
import com.i54mpenguin.punisher.objects.gui.ConfirmationGUI;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelOne;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelThree;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelTwo;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelZero;
import com.i54mpenguin.punisher.utils.LuckPermsHook;
import com.i54mpenguin.punisher.utils.Permissions;
import com.i54mpenguin.punisher.utils.UpdateChecker;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Plugin;
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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.*;

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
    private final PunishmentManager punManager = PunishmentManager.getINSTANCE();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final PlayerDataManager playerDataManager = PlayerDataManager.getINSTANCE();

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


    /**
     * This method is used to pre load the protocol side of the plugin and setup the plugin instance.
     */
    @Override
    public void onLoad() {
        try {
            getLogger().info("Preloading plugin...");
            //setup plugin instance
            setInstance(this);
            //initialize packet gui system
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
            // TODO: 8/06/2020 set this to work with player data configs
//            //make sure info config has last join id set
//            if (!playerInfoConfig.contains("lastjoinid")) {
//                playerInfoConfig.set("lastjoinid", 0);
//                saveInfo();
//            }
//            ServerConnect.lastJoinId = me.fiftyfour.punisher.bungee.PunisherPlugin.playerInfoConfig.getInt("lastjoinid", 0);
//
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
                    throw new NumberFormatException("One of you superbroadcast-title numbers is not a number! (fade in, stay or fade out");
                } catch (IndexOutOfBoundsException ioobe){
                    throw new IndexOutOfBoundsException("You have no supplied enough arguments in the superbroadcast-title!");
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
                        getLogger().warning(prefix + ChatColor.RED + "Update checker found an update, current version: " + this.getDescription().getVersion() + " latest version: " + UpdateChecker.getCurrentVersion());
                        getLogger().warning(prefix + ChatColor.RED + "This update was released on: " + UpdateChecker.getRealeaseDate());
                        getLogger().warning(prefix + ChatColor.RED + "This may fix some bugs and enhance features.");
                        getLogger().warning(prefix + ChatColor.RED + "You will no longer receive support for this version!");
                        LOGS.warning("Update checker found an update, current version: " + this.getDescription().getVersion() + " latest version: " + UpdateChecker.getCurrentVersion());
                        LOGS.warning("This update was released on: " + UpdateChecker.getRealeaseDate());
                        LOGS.warning("This may fix some bugs and enhance features.");
                        LOGS.warning("You will no longer receive support for this version!");
                        isUpdate = true;
                    } else {
                        getLogger().info(prefix + ChatColor.GREEN + "Plugin is up to date!");
                        isUpdate = false;
                    }
                } catch (Exception e) {
                    getLogger().severe(ChatColor.RED + e.getMessage());
                }
            } else if (this.getDescription().getVersion().contains("LEGACY")) {//if running a legacy version then alert user
                getLogger().warning(prefix + ChatColor.GREEN + "You are running a LEGACY version of The Punisher");
                getLogger().warning(prefix + ChatColor.GREEN + "This version is no longer updated with new features and ONLY MAJOR BUGS WILL BE FIXED!!");
                getLogger().warning(prefix + ChatColor.GREEN + "It is recommended that you update to the latest version to have new features.");
                getLogger().warning(prefix + ChatColor.GREEN + "Update checking is not needed in this version");
                isUpdate = false;
            } else {
                getLogger().info(prefix + ChatColor.GREEN + "You are running an unofficial release version of The Punisher");
                getLogger().info(prefix + ChatColor.GREEN + "Update checking is not needed in this version");
                isUpdate = false;
            }

            // TODO: 9/06/2020 implement rep on vote and discord bot
//            //check if rep on vote should be enabled
//            if ((getProxy().getPluginManager().getPlugin("NuVotifier") != null || getProxy().getPluginManager().getPlugin("Votifier") != null) && config.getBoolean("Voting.addRepOnVote")) {
//                getLogger().info(prefix + ChatColor.GREEN + "Enabled Rep on Vote feature!");
//                getProxy().getPluginManager().registerListener(this, new PlayerVote());
//            }
//            //check if discord integration is enabled
//            if (config.getBoolean("DiscordIntegration.Enabled"))
//                DiscordMain.startBot();

            //start player data manager
            getLogger().info(prefix + ChatColor.GREEN + "Starting Player Data Manager...");
            playerDataManager.start();
            //start database manager
            getLogger().info(prefix + ChatColor.GREEN + "Starting Database Manager...");
            dbManager.start();
            //start worker manager
            //this should be started last so that other managers don't start doing operations with workers when the plugin has not fully loaded
            getLogger().info(prefix + ChatColor.GREEN + "Starting Worker Manager...");
            workerManager.start();





            //plugin loading/enabling is now complete so we announce it
            setLoaded(true);
            long duration = (System.nanoTime() - startTime) / 1000000;
            getLogger().info(prefix + ChatColor.GREEN + "Successfully enabled The Punisher v" + this.getDescription().getVersion() + " By 54mpenguin (took " + duration + "ms)");
        } catch (Exception e) {
            //if we get any errors on startup we shutdown and alert the user
            onDisable();
            getLogger().severe(prefix + ChatColor.RED + "Could not enable The Punisher v" + this.getDescription().getVersion() + "!!");
            if (e.getCause() != null)
                getLogger().severe(prefix + ChatColor.RED + "Error cause: " + e.getCause());
            if (e.getMessage() != null)
                getLogger().severe(prefix + ChatColor.RED + "Error message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is the shutdown method and is used when we reload the plugin in {@link AdminCommands} as well as when the server shuts down.
     */
    @Override
    public void onDisable() {
        try {
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
            if (workerManager.isStarted())
                workerManager.stop();
            if (playerDataManager.isStarted())
                playerDataManager.stop();
            dbManager.stop();
//            DiscordMain.shutdown();
            File latestLogs = new File(getDataFolder() + "/logs/latest.log");
            if (latestLogs.length() != 0) {
                LOGS.info("****END OF LOGS ENDING DATE: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")) + "****");
                SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy_HH-mm-ss");
                Files.move(latestLogs.toPath(), new File(getDataFolder() + "/logs/" + format.format(Calendar.getInstance().getTime()) + ".log").toPath());
            }
            logsHandler.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This method is used to initialize the required modules for the protocol side of the plugin.
     */
    private void initProtocol() {
        ProxyServer.getInstance().getPluginManager().registerListener(this, new PlayerListener(this));
        ItemsModule.initModule();
        InventoryModule.initModule();
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
        getProxy().getPluginManager().registerListener(this, new PlayerLogin());
        getProxy().getPluginManager().registerListener(this, new PostPlayerLogin());
        getProxy().getPluginManager().registerListener(this, new PluginMessage());
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
        //todo start player data manager here
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
            Files.copy(getResourceAsStream("bungeeconfig.yml"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file", e);
        }
    }

}
