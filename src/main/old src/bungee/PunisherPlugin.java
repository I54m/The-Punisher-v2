package me.fiftyfour.punisher.bungee;

import com.i54m.punisher.chats.AdminChat;
import com.i54m.punisher.chats.StaffChat;
import com.i54m.punisher.managers.DatabaseManager;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.utils.LuckPermsHook;
import com.i54m.punisher.utils.Permissions;
import com.i54m.punisher.utils.UpdateChecker;
import lombok.Getter;
import lombok.Setter;
import me.fiftyfour.punisher.bungee.discordbot.DiscordMain;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class PunisherPlugin extends Plugin implements Listener {

    private File configFile;
    private static File cooldownsFile, staffHideFile;
    private static FileHandler logsHandler;
    public static File reputationFile, punishmentsFile, playerInfoFile, discordIntegrationFile;
    public static Configuration punishmentsConfig, config, reputationConfig, staffHideConfig, playerInfoConfig, cooldownsConfig;
    public static final Logger LOGS = Logger.getLogger("Punisher Logs");

    private final PunishmentManager punManager = PunishmentManager.getINSTANCE();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();

    public static boolean update;
    public static boolean enabled = false;
    public String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;
    public LuckPermsHook luckPermsHook;
    public Map<ServerInfo, ArrayList<ProxiedPlayer>> staff = new HashMap<>();

    @Getter
    @Setter
    private static PunisherPlugin instance;

    @Override
    public void onLoad() {
        //set instance
        setInstance(this);
        if (instance == null || getInstance() == null)
            throw new NullPointerException("Could not setup required load variables!");
    }

    @Override
    public void onEnable() {
        //todo go through all the messages sent in the plugin and reformat them to look nicer
        try {
            long startTime = System.nanoTime();
            //check for dependencies
            luckPermsHook = new LuckPermsHook().hook();
            if (luckPermsHook == null) {
                getLogger().warning(prefix + ChatColor.RED + "Luck Perms not detected, Plugin has been Disabled!");
                getProxy().getPluginManager().unregisterCommands(this);
                getProxy().getPluginManager().unregisterListeners(this);
                return;
            }
            Permissions.init();//init permissions to hook into luckperms
            //register plugin channels
            getProxy().registerChannel("punisher:main");
            getProxy().registerChannel("punisher:minor");
            //register commands
            registerCommands();
            //register event listeners
            registerListeners();

            //load all configs and create them if they don't exist
            getLogger().info(prefix + ChatColor.GREEN + "Loading Configs...");
            loadConfigs();
            //check main config version
            if (!config.getString("Configversion").equals(this.getDescription().getVersion())) {
                getLogger().warning(prefix + ChatColor.RED + "Old config.yml detected!");
                getLogger().warning(prefix + ChatColor.RED + "Renaming old config to old_config.yml!");
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
            //make sure info config has last join id set
            if (!playerInfoConfig.contains("lastjoinid")) {
                playerInfoConfig.set("lastjoinid", 0);
                saveInfo();
            }
            ServerConnect.lastJoinId = PunisherPlugin.playerInfoConfig.getInt("lastjoinid", 0);
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
                        update = false;
                    } else if (UpdateChecker.check()) {
                        getLogger().warning(prefix + ChatColor.RED + "Update checker found an update, current version: " + this.getDescription().getVersion() + " latest version: " + UpdateChecker.getCurrentVersion());
                        getLogger().warning(prefix + ChatColor.RED + "This update was released on: " + UpdateChecker.getRealeaseDate());
                        getLogger().warning(prefix + ChatColor.RED + "This may fix some bugs and enhance features.");
                        getLogger().warning(prefix + ChatColor.RED + "You will no longer receive support for this version!");
                        LOGS.warning("Update checker found an update, current version: " + this.getDescription().getVersion() + " latest version: " + UpdateChecker.getCurrentVersion());
                        LOGS.warning("This update was released on: " + UpdateChecker.getRealeaseDate());
                        LOGS.warning("This may fix some bugs and enhance features.");
                        LOGS.warning("You will no longer receive support for this version!");
                        update = true;
                    } else {
                        getLogger().info(prefix + ChatColor.GREEN + "Plugin is up to date!");
                        update = false;
                    }
                } catch (Exception e) {
                    getLogger().severe(ChatColor.RED + e.getMessage());
                }
            } else if (this.getDescription().getVersion().contains("LEGACY")) {//if legacy then alert console
                getLogger().warning(prefix + ChatColor.GREEN + "You are running a LEGACY version of The Punisher");
                getLogger().warning(prefix + ChatColor.GREEN + "This version is no longer updated with new features and ONLY MAJOR BUGS WILL BE FIXED!!");
                getLogger().warning(prefix + ChatColor.GREEN + "It is recommended that you update to the latest version to have new features.");
                getLogger().warning(prefix + ChatColor.GREEN + "Update checking is not needed in this version");
                update = false;
            } else {
                getLogger().info(prefix + ChatColor.GREEN + "You are running a PRE-RELEASE version of The Punisher");
                getLogger().info(prefix + ChatColor.GREEN + "Update checking is not needed in this version");
                update = false;
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

            //successful start message
            enabled = true;
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

    public void onDisable() {
            try {
                getProxy().getPluginManager().unregisterListeners(this);
                getProxy().getPluginManager().unregisterCommands(this);
                if (workerManager.isStarted())
                    workerManager.stop();
                dbManager.stop();
                DiscordMain.shutdown();
                LOGS.info("****END OF LOGS ENDING DATE: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "****"); // TODO: 15/05/2020 maybe remove this
                logsHandler.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }


}