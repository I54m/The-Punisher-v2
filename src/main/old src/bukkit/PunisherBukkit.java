package com.i54mpenguin.punisher.bukkit;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import com.i54mpenguin.punisher.bukkit.commands.BukkitAdmin;
import com.i54mpenguin.punisher.bukkit.commands.ClearChat;
import com.i54mpenguin.punisher.bukkit.commands.PunishGUI;
import com.i54mpenguin.punisher.bukkit.commands.ToggleChat;
import com.i54mpenguin.punisher.bukkit.listeners.PluginMessage;
import com.i54mpenguin.punisher.bukkit.listeners.PostLogin;
import com.i54mpenguin.punisher.bukkit.objects.confirmationgui.ConfirmationGUI;
import com.i54mpenguin.punisher.bukkit.objects.confirmationgui.ConfirmationGUIv1_13;
import com.i54mpenguin.punisher.bukkit.objects.confirmationgui.ConfirmationGUIv1_8;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelone.LevelOnePunishMenu;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelone.LevelOnePunishMenuV1_13;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelone.LevelOnePunishMenuV1_8;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelone.LevelOnePunishMenuV1_9;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelthree.*;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.leveltwo.*;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelzero.LeveLZeroPunishMenuV1_13;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelzero.LeveLZeroPunishMenuV1_8;
import com.i54mpenguin.punisher.bukkit.objects.punishmenu.levelzero.LevelZeroPunishMenu;
import com.i54mpenguin.punisher.utils.LuckPermsHook;
import com.i54mpenguin.punisher.utils.Permissions;
import com.i54mpenguin.punisher.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class PunisherBukkit extends JavaPlugin implements Listener {
    public static boolean update = false;
    public static Map<String, Double> repCache = new HashMap<>();
    private static PunisherBukkit instance;
    private static long nextRepUpdate;
    public String chatState = "on";
    private String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;

    public static PunisherBukkit getInstance() {
        return instance;
    }

    public LuckPermsHook luckPermsHook = null;

    private static void setInstance(PunisherBukkit instance) {
        PunisherBukkit.instance = instance;
    }

    public static void updateRepCache() {
        nextRepUpdate = System.nanoTime() + 500000000;
        for (Player players : Bukkit.getOnlinePlayers()) {
            try {
                ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(outbytes);
                out.writeUTF("getrepcache");
                out.writeUTF(players.getUniqueId().toString().replace("-", ""));
                players.sendPluginMessage(PunisherBukkit.getPlugin(PunisherBukkit.class), "punisher:minor", outbytes.toByteArray());
                out.close();
                outbytes.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public LevelZeroPunishMenu levelZeroPunishMenu;
    public LevelOnePunishMenu levelOnePunishMenu;
    public LevelTwoPunishMenu levelTwoPunishMenu;
    public LevelThreePunishMenu levelThreePunishMenu;
    public ConfirmationGUI confirmationGUI;
    public static String COMPATIBILITY = "N/A";

    @Override
    public void onEnable() { // TODO: 15/03/2020 maybe add a placeholderapi place holder for reputation
        setInstance(this);
        //setup punish gui menus & check version
        //      check for dependencies
        luckPermsHook = new LuckPermsHook().hook();
        if (luckPermsHook == null) {
            getLogger().warning(prefix + ChatColor.RED + "Luck Perms not detected, Plugin has been Disabled!");
            this.setEnabled(false);
            return;
        }
        Permissions.init();//init permissions to hook into luckperms
        if (!setupPunishMenus()) {
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "This version of the punisher is not compatible with minecraft " + this.getServer().getVersion());
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "Please update your server to support Minecraft 1.8+");
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "Plugin Disabled!");
            this.setEnabled(false);
            return;
        }
        levelZeroPunishMenu.setupMenu();
        levelOnePunishMenu.setupMenu();
        levelTwoPunishMenu.setupMenu();
        levelThreePunishMenu.setupMenu();
        confirmationGUI.setupMenu();
        getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Running Compatibility for: " + COMPATIBILITY);
        //register commands
        getCommand("punish").setExecutor(new PunishGUI());
        getCommand("clearchat").setExecutor(new ClearChat());
        getCommand("togglechat").setExecutor(new ToggleChat());
        getCommand("punisherbukkit").setExecutor(new BukkitAdmin());
        //register plugin message channels
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "punisher:minor", new PluginMessage());
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "punisher:minor");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "punisher:main", new PunishGUI());
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "punisher:main");
        //check for update
        getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Checking for updates...");
        if (!this.getDescription().getVersion().contains("BETA") && !this.getDescription().getVersion().contains("PRE-RELEASE")
                && !this.getDescription().getVersion().contains("DEV-BUILD") && !this.getDescription().getVersion().contains("SNAPSHOT")) {
            try {
                if (UpdateChecker.getCurrentVersion() == null) {
                    getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Could not check for update!");
                    update = false;
                } else if (UpdateChecker.check()) {
                    getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "Update checker found an update, current version: " + this.getDescription().getVersion() + " latest version: " + UpdateChecker.getCurrentVersion());
                    getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "This update was released on: " + UpdateChecker.getRealeaseDate());
                    getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "This may fix some bugs and enhance features.");
                    getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "You will no longer receive support for this version!");
                    update = true;
                } else {
                    getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Plugin is up to date!");
                    update = false;
                }
            } catch (Exception e) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + e.getMessage());
            }
        }
        if (this.getDescription().getVersion().contains("LEGACY")) {
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "You are running a LEGACY version of The PunisherPlugin");
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "This version is no longer updated with new features and ONLY MAJOR BUGS WILL BE FIXED!!");
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "It is recommended that you update your server to 1.13.2 to have the new features.");
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Update checking is not needed in these versions");
            update = false;
        } else if (this.getDescription().getVersion().contains("BETA") && this.getDescription().getVersion().contains("PRE-RELEASE")
                && this.getDescription().getVersion().contains("DEV-BUILD") && this.getDescription().getVersion().contains("SNAPSHOT")) {
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "You are running a PRE-RELEASE version of The PunisherPlugin");
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Update checking is not needed in these versions");
            update = false;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "MVdWPlaceholderApi detected, enabling reputation placeholder...");
            Bukkit.getPluginManager().registerEvents(new PostLogin(), this);
            PlaceholderAPI.registerPlaceholder(this, "punisher_reputation", (event) -> {
                if (event.isOnline() && event.getPlayer() != null) {
                    if (nextRepUpdate <= System.nanoTime())
                        updateRepCache();
                    Player player = event.getPlayer();
                    String uuid = player.getUniqueId().toString().replace("-", "");
                    if (repCache.get(uuid) != null) {
                        //format rep with color
                        StringBuilder reputation = new StringBuilder();
                        double rep = repCache.get(uuid);
                        String repString = new DecimalFormat("##.##").format(rep);
                        if (rep == 5) {
                            reputation.append(ChatColor.WHITE).append("(").append(repString).append("/10").append(")");
                        } else if (rep > 5) {
                            reputation.append(ChatColor.GREEN).append("(").append(repString).append("/10").append(")");
                        } else if (rep < 5 && rep > -1) {
                            reputation.append(ChatColor.YELLOW).append("(").append(repString).append("/10").append(")");
                        } else if (rep < -1 && rep > -8) {
                            reputation.append(ChatColor.GOLD).append("(").append(repString).append("/10").append(")");
                        } else if (rep < -8) {
                            reputation.append(ChatColor.RED).append("(").append(repString).append("/10").append(")");
                        }
                        return reputation.toString();
                    }
                }
                return "-";
            });

        } else
            getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "MVDWPlaceholderApi Not detected reputation placeholder will not work!");
    }

    private boolean setupPunishMenus() {
        if (this.getServer().getVersion().contains("1.8")) {
            //use 1.8 menus
            levelZeroPunishMenu = new LeveLZeroPunishMenuV1_8();
            levelOnePunishMenu = new LevelOnePunishMenuV1_8();
            levelTwoPunishMenu = new LevelTwoPunishMenuV1_8();
            levelThreePunishMenu = new LevelThreePunishMenuV1_8();
            confirmationGUI = new ConfirmationGUIv1_8();
            COMPATIBILITY = "MC: 1.8";
            return true;
        } else if (this.getServer().getVersion().contains("1.9") || this.getServer().getVersion().contains("1.10") || this.getServer().getVersion().contains("1.11") || this.getServer().getVersion().contains("1.12")) {
            //use 1.9 menus
            levelZeroPunishMenu = new LeveLZeroPunishMenuV1_8();//no change since last version
            levelOnePunishMenu = new LevelOnePunishMenuV1_9();
            levelTwoPunishMenu = new LevelTwoPunishMenuV1_9();
            levelThreePunishMenu = new LevelThreePunishMenuV1_9();
            confirmationGUI = new ConfirmationGUIv1_8();//no change since last version
            COMPATIBILITY = "MC: 1.9 - 1.12";
            return true;
        } else if (this.getServer().getVersion().contains("1.13") || this.getServer().getVersion().contains("1.14")) {
            //use 1.13 menus
            levelZeroPunishMenu = new LeveLZeroPunishMenuV1_13();
            levelOnePunishMenu = new LevelOnePunishMenuV1_13();
            levelTwoPunishMenu = new LevelTwoPunishMenuV1_13();
            levelThreePunishMenu = new LevelThreePunishMenuV1_13();
            confirmationGUI = new ConfirmationGUIv1_13();
            COMPATIBILITY = "MC: 1.13 - 1.14";
            return true;
        }else if (this.getServer().getVersion().contains("1.15") || this.getServer().getVersion().contains("1.16")){
            //use 1.15 menus
            levelZeroPunishMenu = new LeveLZeroPunishMenuV1_13();//no change since last version
            levelOnePunishMenu = new LevelOnePunishMenuV1_13();//no change since last version
            levelTwoPunishMenu = new LevelTwoPunishMenuV1_15();
            levelThreePunishMenu = new LevelThreePunishMenuV1_15();
            confirmationGUI = new ConfirmationGUIv1_13();//no change since last version
            COMPATIBILITY = "MC: 1.15+ (Latest Compatibility)";
            return true;
        } else return false;

    }
}
