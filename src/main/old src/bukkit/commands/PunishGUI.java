package com.i54m.punisher.bukkit.commands;


import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.i54m.punisher.bukkit.PunisherBukkit;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.utils.Permissions;
import com.i54m.punisher.utils.UserFetcher;
import me.fiftyfour.punisher.universal.util.NameFetcher;
import me.fiftyfour.punisher.universal.util.UUIDFetcher;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.UUID;


public class PunishGUI implements PluginMessageListener, CommandExecutor {
    private static final String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "Punisher" + ChatColor.GRAY + "] " + ChatColor.RESET;
    private static final PunisherBukkit plugin = PunisherBukkit.getInstance();

    @SuppressWarnings("UnstableApiUsage")
    private static void sendPluginMessage(@NotNull Player player, String channel, @NotNull String... messages) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (String msg : messages) {
            out.writeUTF(msg);
        }
        player.sendPluginMessage(plugin, channel, out.toByteArray());
    }

    public static void punishmentSelected(String targetuuid, String targetName, int slot, ItemStack item, Player clicker) {
        try {
            UUID formattedUUID = UUIDFetcher.formatUUID(targetuuid);// TODO: 18/04/2020 possible error with the api not hooking properly or users not being fetched correctly?
            User user = plugin.luckPermsHook.getApi().getUserManager().getUser(formattedUUID);
            if (user == null) {
                UserFetcher userFetcher = new UserFetcher();
                userFetcher.setUuid(formattedUUID);
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future<User> userFuture = executorService.submit(userFetcher);
                user = userFuture.get(500, TimeUnit.MILLISECONDS);
                executorService.shutdown();
                if (user == null) throw new IllegalArgumentException("User cannot be null!");
            }
            if (Permissions.higher(clicker, user)) {
                if (PunisherBukkit.COMPATIBILITY.equals("MC: 1.7 - 1.8"))
                    clicker.playSound(clicker.getLocation(), Sound.valueOf("ORB_PICKUP"), 100, 1.5f);
                else
                    clicker.playSound(clicker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1.5f);

                sendPluginMessage(clicker, "punisher:main", "punish", targetuuid, targetName, String.valueOf(slot), item.toString());
            } else
                clicker.sendMessage(prefix + ChatColor.RED + "You cannot Punish that player!");
        } catch (Exception e) {
            DataFecthException dfe = new DataFecthException("User instance required for punishment level checking", targetName, "User Instance", Permissions.class.getName(), e);
            clicker.sendMessage(prefix + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + "Luckperms was unable to fetch permission data on: " + targetName);
            clicker.sendMessage(prefix + ChatColor.RED + "This error will be logged! Please Inform an admin asap, this plugin will no longer function as intended! ");
            sendPluginMessage(clicker, "punisher:minor", "log", "SEVERE", "ERROR: Luckperms was unable to fetch permission data on: " + targetName);
            sendPluginMessage(clicker, "punisher:minor", "log", "SEVERE", "Error message: " + dfe.getMessage());
            sendPluginMessage(clicker, "punisher:minor", "log", "SEVERE", "Cause Error message: " + dfe.getCause().getMessage());
            StringBuilder stacktrace = new StringBuilder();
            for (StackTraceElement stackTraceElement : dfe.getStackTrace()) {
                stacktrace.append(stackTraceElement.toString()).append("\n");
            }
            sendPluginMessage(clicker, "punisher:minor", "log", "SEVERE", "Stack Trace: " + stacktrace.toString());
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("punish") || label.equalsIgnoreCase("p") || label.equalsIgnoreCase("pun")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must be a player to use this command!");
                return false;
            }
            Player p = (Player) sender;
            if (p.hasPermission("punisher.punish.level.0")) {
                if (args.length <= 0) {
                    p.sendMessage(ChatColor.RED + "Please provide a player's name!");
                    return false;
                }
                Player findTarget = Bukkit.getPlayer(args[0]);
                String targetuuid = null;
                Future<String> future = null;
                ExecutorService executorService = null;
                if (findTarget != null) {
                    targetuuid = findTarget.getUniqueId().toString().replace("-", "");
                    UUID formatedUuid = UUIDFetcher.formatUUID(targetuuid);
                    if (formatedUuid.equals(p.getUniqueId())) {
                        p.sendMessage(prefix + ChatColor.RED + "You may not punish yourself!");
                        return false;
                    }
                } else {
                    UUIDFetcher uuidFetcher = new UUIDFetcher();
                    uuidFetcher.fetch(args[0]);
                    executorService = Executors.newSingleThreadExecutor();
                    future = executorService.submit(uuidFetcher);
                }
                if (future != null) {
                    try {
                        targetuuid = future.get(1, TimeUnit.SECONDS);
                    } catch (TimeoutException te) {
                        p.sendMessage(prefix + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + "Connection to mojang API took too long! Unable to fetch " + args[0] + "'s uuid!");
                        p.sendMessage(prefix + ChatColor.RED + "This error will be logged! Please Inform an admin asap, this plugin will no longer function as intended! ");
                        sendPluginMessage(p, "punisher:minor", "log", "SEVERE", "ERROR: Connection to mojang API took too long! Unable to fetch " + args[0] + "'s uuid!");
                        sendPluginMessage(p, "punisher:minor", "log", "SEVERE", "Error message: " + te.getMessage());
                        StringBuilder stacktrace = new StringBuilder();
                        for (StackTraceElement stackTraceElement : te.getStackTrace()) {
                            stacktrace.append(stackTraceElement.toString()).append("\n");
                        }
                        sendPluginMessage(p, "punisher:minor", "log", "SEVERE", "Stack Trace: " + stacktrace.toString());
                        executorService.shutdown();
                        return false;
                    } catch (Exception e) {
                        p.sendMessage(prefix + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + "Unexpected Error while setting up GUI! Unable to fetch " + args[0] + "'s uuid!");
                        p.sendMessage(prefix + ChatColor.RED + "This error will be logged! Please Inform an admin asap, this plugin will no longer function as intended! ");
                        sendPluginMessage(p, "punisher:minor", "log", "SEVERE", "ERROR: Unexpected error while setting up GUI! Unable to fetch " + args[0] + "'s uuid!");
                        sendPluginMessage(p, "punisher:minor", "log", "SEVERE", "Error message: " + e.getMessage());
                        StringBuilder stacktrace = new StringBuilder();
                        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                            stacktrace.append(stackTraceElement.toString()).append("\n");
                        }
                        sendPluginMessage(p, "punisher:minor", "log", "SEVERE", "Stack Trace: " + stacktrace.toString());
                        executorService.shutdown();
                        return false;
                    }
                    executorService.shutdown();
                }
                if (targetuuid == null) {
                    p.sendMessage(ChatColor.RED + "That is not a player's name!");
                    return false;
                }
                String targetName = NameFetcher.getName(targetuuid);
                if (targetName == null) {
                    targetName = args[0];
                }
                if (!PunisherBukkit.repCache.containsKey(targetuuid)) {
                    sendPluginMessage(p, "punisher:main", "getrep", targetuuid, targetName);
                } else {
                    double rep = PunisherBukkit.repCache.get(targetuuid);
                    String repString = new DecimalFormat("##.##").format(rep);
                    StringBuilder reputation = new StringBuilder();
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
                    openGUI(p, targetuuid, targetName, reputation.toString());
                }

            } else {
                p.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return false;
            }
        }
        return false;
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull final Player player, @NotNull byte[] message) {
        if (channel.equals("punisher:main")) {
            try {
                ByteArrayInputStream inBytes = new ByteArrayInputStream(message);
                DataInputStream in = new DataInputStream(inBytes);
                String subchannel = in.readUTF();
                if (subchannel.equals("rep")) {
                    String repstring = in.readUTF();
                    double rep;
                    String targetuuid = in.readUTF();
                    String targetname = in.readUTF();
                    StringBuilder reputation = new StringBuilder();
                    try {
                        rep = Double.parseDouble(repstring);
                    } catch (NumberFormatException e) {
                        reputation.append(ChatColor.WHITE).append("(").append("-").append("/10").append(")");
                        openGUI(player, targetuuid, targetname, reputation.toString());
                        in.close();
                        inBytes.close();
                        return;
                    }
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
                    openGUI(player, targetuuid, targetname, reputation.toString());
                }
                in.close();
                inBytes.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void openGUI(final Player p, String targetuuid, String targetName, String reputation) {
        plugin.getLogger().severe("UUID: " + targetuuid);
        plugin.getLogger().severe("NAME: " + targetName);
        if (p.hasPermission("punisher.punish.level.3")) {
            plugin.levelThreePunishMenu.open(p, targetuuid, targetName, reputation);
        } else if (p.hasPermission("punisher.punish.level.2")) {
            plugin.levelTwoPunishMenu.open(p, targetuuid, targetName, reputation);
        } else if (p.hasPermission("punisher.punish.level.1")) {
            plugin.levelOnePunishMenu.open(p, targetuuid, targetName, reputation);
        } else if (p.hasPermission("punisher.punish.level.0")) {
            plugin.levelZeroPunishMenu.open(p, targetuuid, targetName, reputation);
        } else {
            p.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        }
    }
}