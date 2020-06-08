package com.i54mpenguin.punisher;

import com.i54mpenguin.protocol.api.injector.NettyPipelineInjector;
import com.i54mpenguin.protocol.api.listener.PlayerListener;
import com.i54mpenguin.protocol.inventory.InventoryModule;
import com.i54mpenguin.protocol.items.ItemsModule;
import com.i54mpenguin.punisher.commands.PunishCommand;
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
import com.i54mpenguin.punisher.commands.BroadcastCommand;
import com.i54mpenguin.punisher.commands.SuperBroadcast;
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

    /*
     * Config Variables
     *  TODO: 8/06/2020 add new files (gui.yml) and remove things that may be stored in a database (reputation, playerinfo, discordintegration, cooldowns, staffhide)
     */
    private File configFile, punishmentsFile;
    //private static File cooldownsFile, staffHideFile;
//    public static File reputationFile, playerInfoFile, discordIntegrationFile;
    @Getter
    private static Configuration punishmentsConfig, config;
            //reputationConfig, staffHideConfig, playerInfoConfig, cooldownsConfig;


    private static FileHandler logsHandler;
    @Getter
    private static final Logger LOGS = Logger.getLogger("Punisher Logs");
    @Getter // TODO: 8/06/2020 only send prefix in messages that will show in chat
    private String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;
    @Getter
    private LuckPermsHook luckPermsHook;

    @Getter
    private final NettyPipelineInjector nettyPipelineInjector = new NettyPipelineInjector();


    @Override
    public void onLoad() {
        getLogger().info("Preloading plugin...");
        //setup plugin instance
        setInstance(this);
        //initialize packet gui system
        initProtocol();
        getLogger().info("Plugin preloaded!");
    }

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
            //check main config version
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
                    } else if (UpdateChecker.check()) {
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
            } else if (this.getDescription().getVersion().contains("LEGACY")) {//if legacy then alert console
                getLogger().warning(prefix + ChatColor.GREEN + "You are running a LEGACY version of The Punisher");
                getLogger().warning(prefix + ChatColor.GREEN + "This version is no longer updated with new features and ONLY MAJOR BUGS WILL BE FIXED!!");
                getLogger().warning(prefix + ChatColor.GREEN + "It is recommended that you update to the latest version to have new features.");
                getLogger().warning(prefix + ChatColor.GREEN + "Update checking is not needed in this version");
                isUpdate = false;
            } else {
                getLogger().info(prefix + ChatColor.GREEN + "You are running a PRE-RELEASE version of The Punisher");
                getLogger().info(prefix + ChatColor.GREEN + "Update checking is not needed in this version");
                isUpdate = false;
            }

            //check if rep on vote should be enabled
            if ((getProxy().getPluginManager().getPlugin("NuVotifier") != null || getProxy().getPluginManager().getPlugin("Votifier") != null) && config.getBoolean("Voting.addRepOnVote")) {
                getLogger().info(prefix + ChatColor.GREEN + "Enabled Rep on Vote feature!");
                getProxy().getPluginManager().registerListener(this, new PlayerVote());
            }
            //check if discord integration is enabled
            if (config.getBoolean("DiscordIntegration.Enabled"))
                DiscordMain.startBot();

            //start logging to logs file
            getLogger().info(prefix + ChatColor.GREEN + "Beginning Logging...");
            LOGS.info("****START OF LOGS BEGINNING DATE: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "****"); // TODO: 15/05/2020 maybe make it so this only get's logged once we receive the first log

            //start database manager
            getLogger().info(prefix + ChatColor.GREEN + "Starting Database Manager...");
            dbManager.start();
            //start worker manager
            getLogger().info(prefix + ChatColor.GREEN + "Starting Worker Manager...");
            workerManager.start();






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

    @Override
    public void onDisable() {

    }

    private void initProtocol() {
        ProxyServer.getInstance().getPluginManager().registerListener(this, new PlayerListener(this));
        ItemsModule.initModule();
        InventoryModule.initModule();
    }

    private void setupMenus() {
        LevelZero.setupMenu();
        LevelOne.setupMenu();
        LevelTwo.setupMenu();
        LevelThree.setupMenu();
        ConfirmationGUI.setupMenu();
    }

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

    public void loadConfigs() throws Exception {
        punishmentsFile = new File(getDataFolder(), "punishments.yml");
        configFile = new File(getDataFolder(), "config.yml");
//        staffHideFile = new File(getDataFolder(), "/data/staffhide.yml");
//        reputationFile = new File(getDataFolder(), "/data/reputation.yml");
//        cooldownsFile = new File(getDataFolder(), "/data/cooldowns.yml");
//        playerInfoFile = new File(getDataFolder(), "/data/playerinfo.yml");
//        discordIntegrationFile = new File(getDataFolder(), "/data/discordintegration.yml");
        File logsdir = new File(getDataFolder() + "/logs/");
        File datadir = new File(getDataFolder() + "/data/");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        if (!logsdir.exists()) {
            logsdir.mkdir();
        }
        if (!datadir.exists()) {
            datadir.mkdir();
        }
        // TODO: 15/05/2020 make it save current log as latest.log and then rename previous latest.log if it is not empty
        SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy_HH-mm-ss");
        logsHandler = new FileHandler(getDataFolder() + "/logs/" + format.format(Calendar.getInstance().getTime()) + ".log");
        logsHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                if (record.getLevel() != Level.SEVERE)
                    getLogger().log(record);
                SimpleDateFormat logTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(record.getMillis());
                return "\n" + record.getLevel() + " "
                        + logTime.format(cal.getTime())
                        + ": "
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
//        if (!cooldownsFile.exists())
//            cooldownsFile.createNewFile();
//        if (!reputationFile.exists())
//            reputationFile.createNewFile();
//        if (!playerInfoFile.exists())
//            playerInfoFile.createNewFile();
//        if (!discordIntegrationFile.exists())
//            discordIntegrationFile.createNewFile();
//        if (!staffHideFile.exists())
//            staffHideFile.createNewFile();
        try {
            punishmentsConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(punishmentsFile);
        } catch (YAMLException YAMLE) {
            getLogger().severe(prefix + ChatColor.RED + "Error: Could not load punishments config!");
            getLogger().severe(prefix + ChatColor.RED + "Error message: " + YAMLE.getMessage());
            throw new Exception("Could not load punishments config!", YAMLE);
        }
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (YAMLException YAMLE) {
            getLogger().severe(prefix + ChatColor.RED + "Error: Could not load main config!");
            getLogger().severe(prefix + ChatColor.RED + "Error message: " + YAMLE.getMessage());
            throw new Exception("Could not load main config!", YAMLE);
        }
//        try {
//            cooldownsConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(cooldownsFile);
//            playerInfoConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(playerInfoFile);
//            staffHideConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(staffHideFile);
//            reputationConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(reputationFile);
//        } catch (YAMLException YAMLE) {
//            getLogger().severe(prefix + ChatColor.RED + "Error: Could not load data config files!");
//            getLogger().severe(prefix + ChatColor.RED + "These configs are not meant to be altered!");
//            getLogger().severe(prefix + ChatColor.RED + "If you have not altered them this may be a bug!");
//            getLogger().severe(prefix + ChatColor.RED + "Error message: " + YAMLE.getMessage());
//            throw new Exception("Could not load data config files!", YAMLE);
//        }
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(punishmentsConfig, punishmentsFile);
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveRep() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(reputationConfig, reputationFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveInfo() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(playerInfoConfig, playerInfoFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveCooldowns() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(cooldownsConfig, cooldownsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveStaffHide() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(staffHideConfig, staffHideFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDefaultPunishments() {
        try {
            Files.copy(getResourceAsStream("punishments.yml"), punishmentsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file", e);
        }
    }

    private void saveDefaultConfig() {
        try {
            Files.copy(getResourceAsStream("bungeeconfig.yml"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file", e);
        }
    }

}
