package com.i54mpenguin.punisher.managers;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.chats.StaffChat;
import com.i54mpenguin.punisher.exceptions.PunishmentCalculationException;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.objects.Punishment;
import com.i54mpenguin.punisher.utils.NameFetcher;
import com.i54mpenguin.punisher.utils.Permissions;
import com.i54mpenguin.punisher.utils.UUIDFetcher;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PunishmentManager {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();

    @Getter
    private static final PunishmentManager INSTANCE = new PunishmentManager();
    private PunishmentManager() {}

    private final ArrayList<String> locked = new ArrayList<>();
    private final Map<String, Integer> PendingPunishments = new HashMap<>();

    // TODO: 16/03/2020 maybe add a start and stop method here so we don't end up doing punishments when other managers are stopped


    public void issue(@NotNull final Punishment punishment, @Nullable ProxiedPlayer player, final boolean addHistory, final boolean announce, final boolean minusRep) throws SQLException {
        @Nullable final ProxiedPlayer finalPlayer = player;
         workerManager.runWorker(new WorkerManager.Worker(() -> {
            try {
                dbManager.DumpNew();
            } catch (PunishmentsDatabaseException pde) {
                errorHandler.log(pde);
                if (finalPlayer != null && finalPlayer.isConnected()) errorHandler.alert(pde, finalPlayer);
                else errorHandler.adminChatAlert(pde, ProxyServer.getInstance().getConsole());
            }
        }));

        if (player != null && player.isConnected() && punishment.getPunisherUUID() == null)
            dbManager.updatePunishment(punishment.setPunisherUUID(player.getUniqueId().toString().replace("-", "")));
        if (punishment.isCustom() && !punishment.hasExpiration())
            dbManager.updatePunishment(punishment.setExpiration((long) 3.154e+12 + System.currentTimeMillis()));
        if ((punishment.isMute() || punishment.isBan()) && !punishment.hasExpiration())
            dbManager.updatePunishment(punishment.setExpiration(calculateExpiration(punishment)));

        punishment.verify();

        if (!punishment.isPending())
            dbManager.updatePunishment(punishment.setStatus(Punishment.Status.Pending));
        boolean reissue = false;
        if (PendingPunishments.containsValue(punishment.getId()))
            reissue = true;

        Punishment.Type type = punishment.getType();
        Punishment.Reason reason = punishment.getReason();
        String targetuuid = punishment.getTargetUUID();
        String targetname = punishment.getTargetName();
        String punisherUUID = punishment.getPunisherUUID();
        long expiration = punishment.getExpiration();
        double repLoss = calculateRepLoss(punishment);
        ProxiedPlayer target;
        try {
            target = plugin.getProxy().getPlayer(UUIDFetcher.formatUUID(targetuuid));
        } catch (NullPointerException npe){
            target = null;
        }
        player = player == null ? plugin.getProxy().getPlayer(punisherUUID) : player;

        if (isMuted(targetuuid) && type == Punishment.Type.MUTE) {
            String oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(player, oldPunisherID) && !punisherUUID.equals("CONSOLE")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot mute: " + targetname + " as they have an active mute by someone with higher permissions than you!").color(ChatColor.RED).event(getMute(targetuuid).getHoverEvent()).create());
                return;
            }
        } else if (isBanned(targetuuid) && type == Punishment.Type.BAN) {
            String oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(player, oldPunisherID) && !punisherUUID.equals("CONSOLE")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot ban: " + targetname + " as they have an active ban by someone with higher permissions than you!").color(ChatColor.RED).event(getMute(targetuuid).getHoverEvent()).create());
                return;
            }
        }

        if (locked.contains(targetuuid)) {
            if (player != null)
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Punishing " + targetname + " is currently locked! Please try again later!").color(ChatColor.RED).create());
            return;
        }

        dbManager.createHistory(targetuuid);
        dbManager.createStaffHistory(punisherUUID);

        switch (type) {
            case WARN: {
                String reasonMessage;
                if (punishment.getMessage() != null)
                    reasonMessage = punishment.getMessage();
                else
                    reasonMessage = reason.toString().replace("_", " ");
                if (target != null && target.isConnected()) {
                    if (plugin.getConfig().getBoolean("Warn Sound.Enabled")) {
                        try {
                            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
                            DataOutputStream out = new DataOutputStream(outbytes);
                            out.writeUTF("playsound");
                            out.writeUTF(plugin.getConfig().getString("Warn Sound.Sound"));
                            target.getServer().sendData("punisher:minor", outbytes.toByteArray());
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                    if (plugin.getConfig().getBoolean("Warn Title.Enabled")) {
                        String titleText = plugin.getConfig().getString("Warn Title.Title Message");
                        String subtitleText = plugin.getConfig().getString("Warn Title.Subtitle Message");
                        if (punishment.getMessage() != null) {
                            titleText = titleText.replace("%reason%", punishment.getMessage());
                            subtitleText = subtitleText.replace("%reason%", punishment.getMessage());
                        } else {
                            titleText = titleText.replace("%reason%", reason.toString());
                            subtitleText = subtitleText.replace("%reason%", reason.toString());
                        }
                        ProxyServer.getInstance().createTitle().title(new TextComponent(ChatColor.translateAlternateColorCodes('&', titleText)))
                                .subTitle(new TextComponent(ChatColor.translateAlternateColorCodes('&', subtitleText)))
                                .fadeIn(plugin.getConfig().getInt("Warn Title.Fade In"))
                                .stay(plugin.getConfig().getInt("Warn Title.Stay")).fadeOut(plugin.getConfig().getInt("Warn Title.Fade Out"))
                                .send(target);
                    }
                    String warnMessage = plugin.getConfig().getString("Warn Message");
                    if (punishment.getMessage() != null)
                        warnMessage = warnMessage.replace("%reason%", punishment.getMessage());
                    else
                        warnMessage = warnMessage.replace("%reason%", reason.toString().replace("_", " "));
                    target.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', warnMessage)).create());
                    if (!reissue) {
                        dbManager.updatePunishment(punishment.setIssueDate().setStatus(Punishment.Status.Issued));
                        if (player != null && player.isConnected()) {
                            PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + player.getName());
                            if (announce)
                                StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                            else
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Silently Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                        } else {
                            PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                            if (announce)
                                StaffChat.sendMessage(new ComponentBuilder("CONSOLE Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                            else
                                plugin.getProxy().getConsole().sendMessage(new ComponentBuilder(plugin.getPrefix()).append("CONSOLE Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).create());
                        }
                    } else {
                        dbManager.updatePunishment(punishment.setStatus(Punishment.Status.Issued));
                        PendingPunishments.remove(punishment.getTargetUUID());
                        String playername = NameFetcher.getName(punisherUUID);
                        ProxiedPlayer player2 = (player != null && player.isConnected()) ? player : ProxyServer.getInstance().getPlayer(UUIDFetcher.formatUUID(punisherUUID));
                        if (player2 != null && player2.isConnected())
                            player2.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Your warning on " + targetname + " for: " + reasonMessage + " has now been issued.").color(ChatColor.RED).create());
                        if (announce)
                            StaffChat.sendMessage(new ComponentBuilder("CONSOLE Issued Warning on: " + targetname + " for: " + reasonMessage + ". Warning was originally issued by: " + playername).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                        PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + playername + ". This warning was originally issued on: " + punishment.getIssueDate());
                    }
                } else if (target == null || !target.isConnected()) {
                    PendingPunishments.put(punishment.getTargetUUID(), punishment.getId());
                    if (player != null && player.isConnected()) {
                        PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + player.getName() + ". Warning will be issued when they are online next.");
                        if (announce)
                            StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                        else
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Silently Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    } else {
                        PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid) + ". This warning will be issued when they are online next.");
                        if (announce)
                            StaffChat.sendMessage(new ComponentBuilder("CONSOLE Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                        else
                            plugin.getProxy().getConsole().sendMessage(new ComponentBuilder(plugin.getPrefix()).append("CONSOLE Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    }
                }
                if (addHistory) {
                    dbManager.incrementHistory(punishment);
                    dbManager.incrementStaffHistory(punishment);
                }
                if (minusRep) ReputationManager.minusRep(targetname, targetuuid, repLoss);
                return;
            }
            case KICK: {
                String reasonMessage;
                if (punishment.getMessage() != null) {
                    reasonMessage = punishment.getMessage();
                } else {
                    reasonMessage = reason.toString().replace("_", " ");
                }
                if (target != null && target.isConnected()) {
                    String kickMessage = plugin.getConfig().getString("Kick Message");
                    kickMessage = kickMessage.replace("%reason%", reasonMessage);
                    target.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', kickMessage)));
                }
                if (addHistory) {
                    dbManager.incrementHistory(punishment);
                    dbManager.incrementStaffHistory(punishment);
                }
                if (player != null && player.isConnected()) {
                    PunisherPlugin.getLOGS().info(targetname + " Was Kicked for: " + reasonMessage + " by: " + player.getName());
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Silently Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Kicked for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        plugin.getProxy().getConsole().sendMessage(new ComponentBuilder(plugin.getPrefix()).append("CONSOLE Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                }
                if (minusRep)
                    ReputationManager.minusRep(targetname, targetuuid, repLoss);
                return;
            }
            case MUTE: {
                String reasonMessage;
                if (punishment.getMessage() != null) {
                    reasonMessage = punishment.getMessage();
                } else {
                    reasonMessage = reason.toString().replace("_", " ");
                }
                String timeleft = getTimeLeft(punishment);
                if (target != null && target.isConnected()) {
                    if (plugin.getConfig().getBoolean("Mute Sound.Enabled")) {
                        try {
                            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
                            DataOutputStream out = new DataOutputStream(outbytes);
                            out.writeUTF("playsound");
                            out.writeUTF(plugin.getConfig().getString("Mute Sound.Sound"));
                            target.getServer().sendData("punisher:minor", outbytes.toByteArray());
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                    if (plugin.getConfig().getBoolean("Mute Title.Enabled")) {
                        String titleText = plugin.getConfig().getString("Mute Title.Title Message");
                        String subtitleText = plugin.getConfig().getString("Mute Title.Subtitle Message");
                        titleText = titleText.replace("%reason%", reasonMessage);
                        subtitleText = subtitleText.replace("%reason%", reasonMessage);
                        if (announce)
                            ProxyServer.getInstance().createTitle().title(new TextComponent(ChatColor.translateAlternateColorCodes('&', titleText)))
                                    .subTitle(new TextComponent(ChatColor.translateAlternateColorCodes('&', subtitleText)))
                                    .fadeIn(plugin.getConfig().getInt("Mute Title.Fade In"))
                                    .stay(plugin.getConfig().getInt("Mute Title.Stay")).fadeOut(plugin.getConfig().getInt("Mute Title.Fade Out"))
                                    .send(target);
                    }
                    String muteMessage;
                    if (punishment.isPermanent())
                        muteMessage = plugin.getConfig().getString("PermMute Message").replace("%reason%", reasonMessage).replace("%timeleft%", timeleft);
                    else
                        muteMessage = plugin.getConfig().getString("TempMute Message").replace("%reason%", reasonMessage).replace("%timeleft%", timeleft);
                    if (announce)
                        target.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', muteMessage)).create());
                }
                if (addHistory) {
                    dbManager.incrementHistory(punishment);
                    dbManager.incrementStaffHistory(punishment);
                }
                if (hasActivePunishment(targetuuid) && isMuted(targetuuid)) {
                    dbManager.updatePunishment(getMute(targetuuid).setStatus(Punishment.Status.Overridden));// TODO: 8/03/2020 maybe make a way for the expiration to stack rather than just overriding it although doesn't litebans do this too?
                    removeActive(punishment);
                }
                dbManager.updatePunishment(punishment.setExpiration(expiration)
                        .setIssueDate()
                        .setStatus(Punishment.Status.Active));
                addActive(punishment);
                PendingPunishments.remove(targetuuid);
                if (player != null && player.isConnected()) {
                    PunisherPlugin.getLOGS().info(targetname + " Was Muted for: " + reasonMessage + " by: " + player.getName());
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Silently Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());

                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Muted for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        plugin.getProxy().getConsole().sendMessage(new ComponentBuilder(plugin.getPrefix()).append("CONSOLE Muted: " + targetname + " for: " + reasonMessage).event(punishment.getHoverEvent()).color(ChatColor.RED).create());
                }
                if (minusRep)
                    ReputationManager.minusRep(targetname, targetuuid, repLoss);
                return;
            }
            case BAN: {
                String reasonMessage;
                if (punishment.getMessage() != null)
                    reasonMessage = punishment.getMessage();
                else
                    reasonMessage = reason.toString().replace("_", " ");
                String timeleft = getTimeLeft(punishment);
                if (target != null && target.isConnected()) {
                    String banMessage;
                    if (punishment.isPermanent())
                        banMessage = plugin.getConfig().getString("PermBan Message").replace("%timeleft%", timeleft);
                    else
                        banMessage = plugin.getConfig().getString("TempBan Message").replace("%timeleft%", timeleft);
                    banMessage = banMessage.replace("%reason%", reasonMessage);
                    target.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                }
                if (addHistory) {
                    dbManager.incrementHistory(punishment);
                    dbManager.incrementStaffHistory(punishment);
                }
                if (hasActivePunishment(targetuuid) && isBanned(targetuuid)) {
                    dbManager.updatePunishment(getBan(targetuuid).setStatus(Punishment.Status.Overridden));
                    removeActive(punishment);
                }
                dbManager.updatePunishment(punishment.setExpiration(expiration)
                        .setIssueDate()
                        .setStatus(Punishment.Status.Active));
                addActive(punishment);
                PendingPunishments.remove(targetuuid);
                if (player != null && player.isConnected()) {
                    PunisherPlugin.getLOGS().info(targetname + " Was Banned for: " + reasonMessage + " by: " + player.getName());
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Silently Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Banned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        plugin.getProxy().getConsole().sendMessage(new ComponentBuilder(plugin.getPrefix()).append("CONSOLE Banned: " + targetname + " for: " + reasonMessage).event(punishment.getHoverEvent()).color(ChatColor.RED).create());
                }
            }
            if (minusRep)
                ReputationManager.minusRep(targetname, targetuuid, repLoss);
        }
    }

    public void remove(@NotNull Punishment punishment, @Nullable ProxiedPlayer player, boolean removed, boolean removeHistory, boolean announce) throws SQLException {// TODO: 12/03/2020  need to revamp this system a little
        String targetuuid = punishment.getTargetUUID();
        Punishment.Reason reason = punishment.getReason();
        String targetname = punishment.getTargetName();
        String removeruuid = player != null ? player.getUniqueId().toString().replace("-", "") : "CONSOLE";
        if (targetname == null)
            targetname = NameFetcher.getName(punishment.getTargetUUID());

        if (isMuted(targetuuid) && punishment.getType() == Punishment.Type.MUTE) {
            String oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(player, oldPunisherID) && !removeruuid.equals("CONSOLE")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot unmute: " + targetname + " as they have an active mute by someone with higher permissions than you!").color(ChatColor.RED).create());
                return;
            }
        } else if (isBanned(targetuuid) && punishment.getType() == Punishment.Type.BAN) {
            String oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(player, oldPunisherID) && !removeruuid.equals("CONSOLE")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You cannot unban: " + targetname + " as they have an active ban by someone with higher permissions than you!").color(ChatColor.RED).create());
                return;
            }
        }

        if (isMuted(targetuuid) && punishment.getType() == Punishment.Type.MUTE) {
            if (player != null && player.isConnected()) {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Unmuted: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                else
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Silently unmuted " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                PunisherPlugin.getLOGS().info(targetname + " Was Unmuted by: " + player.getName());
            } else {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder("CONSOLE Unmuted: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                PunisherPlugin.getLOGS().info(targetname + " Was Unmuted by: " + NameFetcher.getName(targetuuid));
            }
        }
        if (isBanned(targetuuid) && punishment.getType() == Punishment.Type.BAN) {
            if (player != null && player.isConnected()) {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Unbanned: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                else
                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Silently unbanned " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                PunisherPlugin.getLOGS().info(targetname + " Was Unbanned by: " + player.getName());
            } else {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder("CONSOLE Unbanned: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                PunisherPlugin.getLOGS().info(targetname + " Was Unbanned by: " + NameFetcher.getName(targetuuid));
            }
        }
        if (punishment.isRepBan())
            unlock(targetuuid);
        punishment.setRemoverUUID(removeruuid);
        dbManager.updatePunishment(removed ? punishment.setStatus(Punishment.Status.Removed) : punishment.setStatus(Punishment.Status.Expired));
        removeActive(punishment);
        if (removeHistory) {
            String sqlhist = "SELECT * FROM `history` WHERE UUID='" + targetuuid + "'";// TODO: 12/03/2020  make a method in dbmanager to fetch this
            PreparedStatement stmthist = dbManager.connection.prepareStatement(sqlhist);
            ResultSet resultshist = stmthist.executeQuery();
            if (resultshist.next()) {
                int current;
                if (punishment.isCustom()) {
                    current = resultshist.getInt("Manual_Punishments");
                } else {
                    current = resultshist.getInt(reason.toString());
                }
                if (current != 0) {
                    current--;
                } else {
                    if (player != null) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetname + " Already has 0 punishments for that offence!").color(ChatColor.RED).create());
                        return;
                    }
                }
                String collumn;
                if (reason.toString().contains("Manual")) {
                    collumn = "Manual_Punishments";
                } else {
                    collumn = reason.toString();
                }
                String sqlhistupdate = "UPDATE `history` SET `" + collumn + "`='" + current + "' WHERE `UUID`='" + targetuuid + "' ;";// TODO: 12/03/2020  make a method in dbmanager to do this
                PreparedStatement stmthistupdate = dbManager.connection.prepareStatement(sqlhistupdate);
                stmthistupdate.executeUpdate();
                stmthistupdate.close();
            }
            if (player != null) {
                PunisherPlugin.getLOGS().info(player.getName() + " removed punishment: " + reason.toString().replace("_", " ") + " on player: " + targetname + " Through unpunish");
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("The punishment: " + reason.toString().replace("_", " ") + " on: " + targetname + " has been removed!").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Unpunished: " + targetname + " for the offence: " + reason.toString().replace("_", " ")).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
            }
        }
    }

    public long calculateExpiration(Punishment punishment) throws SQLException {//todo verify that this is actually calculating correctly
        switch (punishment.getReason()) {
            case Other_Minor_Offence:
                return (long) 6.048e+8 + System.currentTimeMillis();
            case Other_Major_Offence:
                return (long) 2.628e+9 + System.currentTimeMillis();
            case Other_Offence:
                return (long) 1.8e+6 + System.currentTimeMillis();
        }
        int punishmentno = dbManager.getOffences(punishment.getTargetUUID(), punishment.getReason());
        punishmentno++;
        if (punishmentno > 5) punishmentno = 5;
        return (long) (60000 * plugin.getPunishments().getDouble(punishment.getReason().toString() + "." + punishmentno + ".length")) + System.currentTimeMillis();
    }

    public Punishment.Type calculateType(String targetuuid, Punishment.Reason reason) throws SQLException, PunishmentCalculationException {
        if (reason.toString().contains("Manual") || reason.toString().equals("Custom") || reason.toString().contains("Other"))
            throw new PunishmentCalculationException("Punishment reason must be an automatic punishment type when calculating punishment type!", "type");
        int punishmentno = dbManager.getOffences(targetuuid, reason);
        punishmentno++;
        if (punishmentno > 5) punishmentno = 5;
        return Punishment.Type.valueOf(plugin.getPunishments().getString(reason.toString() + "." + punishmentno + ".type").toUpperCase());
    }

    public double calculateRepLoss(@NotNull Punishment punishment) throws SQLException {
        if (punishment.isManual() && (punishment.isBan() || punishment.isMute()))
            return plugin.getConfig().getDouble("ReputationScale." + punishment.getType().toString() + "." + 5);
        if (punishment.isManual() && (punishment.isWarn() || punishment.isKick()))
            return plugin.getConfig().getDouble("ReputationScale." + punishment.getType().toString());
        if (punishment.isBan() || punishment.isMute()) {
            int offence = dbManager.getOffences(punishment.getTargetUUID(), punishment.getReason());
            offence++;
            if (offence > 5) offence = 5;
            return plugin.getConfig().getDouble("ReputationScale." + punishment.getType().toString() + "." + offence);
        } else {
            return plugin.getConfig().getDouble("ReputationScale." + punishment.getType().toString());
        }
    }

    public String[] translate(int slot, String item) throws PunishmentCalculationException {
        if ((slot == 11 || slot == 12) && item.contains("Spam, Flood ETC"))
            return new String[]{"Minor_Chat_Offence", "Minor Chat Offence"};
        else if ((slot == 12 || slot == 13) && item.contains("Racism, Disrespect ETC"))
            return new String[]{"Major_Chat_Offence", "Major Chat Offence"};
        else if (slot == 14 && item.contains("For Other Not Listed Offences"))
            return new String[]{"Other_Offence", "Other Offence"};
        else if (slot == 13 && item.contains("Includes Hinting At It and"))
            return new String[]{"DDoS_DoX_Threats", "DDoS/DoX Threats"};
        else if (slot == 14 && item.contains("Includes Pm's"))
            return new String[]{"Inappropriate_Link", "Inappropriate Link"};
        else if (slot == 15 && item.contains("When a player is unfairly taking a player's"))
            return new String[]{"Scamming", "Scamming"};
        else if (slot == 18 && item.contains("Mining Straight to Ores/Bases/Chests"))
            return new String[]{"X_Raying", "X-Raying"};
        else if (slot == 19 && item.contains("Using AutoClicker to Farm Mobs ETC"))
            return new String[]{"AutoClicker", "AutoClicker(Non PvP)"};
        else if (slot == 20 && item.contains("Includes Hacks Such as Jesus and Spider"))
            return new String[]{"Fly_Speed_Hacking", "Fly/Speed Hacking"};
        else if (slot == 21 && item.contains("Includes Hacks Such as Kill Aura and Reach"))
            return new String[]{"Malicious_PvP_Hacks", "Malicious PvP Hacks"};
        else if (slot == 22 && item.contains("Includes Hacks Such as Derp and Headless"))
            return new String[]{"Disallowed_Mods", "Disallowed Mods"};
        else if (slot == 23 && item.contains("Excludes Cobble Monstering and Bypassing land claims"))
            return new String[]{"Greifing", "Greifing"};
        else if (slot == 24 && item.contains("Warning: Must Also Clear Chat After you have proof!"))
            return new String[]{"Server_Advertisement", "Server Advertisement"};
        else if (slot == 25 && item.contains("Includes Bypassing Land Claims and Cobble Monstering"))
            return new String[]{"Exploiting", "Exploiting"};
        else if (slot == 26 && item.contains("Sending a TPA Request to Someone"))
            return new String[]{"Tpa_Trapping", "TPA-Trapping"};
        else if (slot == 30 && item.contains("For Other Minor Offences"))
            return new String[]{"Other_Minor_Offence", "Other Minor Offence"};
        else if (slot == 31 && item.contains("Any type of Impersonation"))
            return new String[]{"Impersonation", "Player Impersonation"};
        else if (slot == 32 && item.contains("Includes Inappropriate IGN's and Other Major Offences"))
            return new String[]{"Other_Major_Offence", "Other Major Offence"};
        else if (slot == 36 && item.contains("Manually Warn the Player"))
            return new String[]{"Custom", "WARN", "Manually Warned"};
        else if (slot == 37 && item.contains("Manually Mute the Player For 1 Hour"))
            return new String[]{"Custom", "MUTE", "Manually Muted for 1 Hour", String.valueOf((long) 3.6e+6 + System.currentTimeMillis())};
        else if (slot == 38 && item.contains("Manually Mute the Player For 1 Day"))
            return new String[]{"Custom", "MUTE", "Manually Muted for 1 Day", String.valueOf((long) 8.64e+7 + System.currentTimeMillis())};
        else if (slot == 39 && item.contains("Manually Mute the Player For 3 Days"))
            return new String[]{"Custom", "MUTE", "Manually Muted for 3 Days", String.valueOf((long) 2.592e+8 + System.currentTimeMillis())};
        else if (slot == 40 && item.contains("Manually Mute the Player For 1 Week"))
            return new String[]{"Custom", "MUTE", "Manually Muted for 1 Week", String.valueOf((long) 6.048e+8 + System.currentTimeMillis())};
        else if (slot == 41 && item.contains("Manually Mute the Player For 2 Weeks"))
            return new String[]{"Custom", "MUTE", "Manually Muted for 2 Weeks", String.valueOf((long) 1.21e+9 + System.currentTimeMillis())};
        else if (slot == 42 && item.contains("Manually Mute the Player For 3 Weeks"))
            return new String[]{"Custom", "MUTE", "Manually Muted for 3 Weeks", String.valueOf((long) 1.814e+9 + System.currentTimeMillis())};
        else if (slot == 43 && item.contains("Manually Mute the Player For 1 Month"))
            return new String[]{"Custom", "MUTE", "Manually Muted for 1 Month", String.valueOf((long) 2.628e+9 + System.currentTimeMillis())};
        else if (slot == 44 && item.contains("Manually Mute the Player Permanently"))
            return new String[]{"Custom", "MUTE", "Manually Muted Permanently", String.valueOf((long) 3.154e+12 + System.currentTimeMillis())};
        else if (slot == 45 && item.contains("Manually Kick the Player"))
            return new String[]{"Custom", "KICK", "Manually Kicked"};
        else if (slot == 46 && item.contains("Manually Ban The Player For 1 Hour"))
            return new String[]{"Custom", "BAN", "Manually Banned for 1 Hour", String.valueOf((long) 3.6e+6 + System.currentTimeMillis())};
        else if (slot == 47 && item.contains("Manually Ban the Player For 1 Day"))
            return new String[]{"Custom", "BAN", "Manually Banned for 1 Day", String.valueOf((long) 8.64e+7 + System.currentTimeMillis())};
        else if (slot == 48 && item.contains("Manually Ban the Player For 3 Days"))
            return new String[]{"Custom", "BAN", "Manually Banned for 3 Days", String.valueOf((long) 2.592e+8 + System.currentTimeMillis())};
        else if (slot == 49 && item.contains("Manually Ban the Player For 1 Week"))
            return new String[]{"Custom", "BAN", "Manually Banned for 1 Week", String.valueOf((long) 6.048e+8 + System.currentTimeMillis())};
        else if (slot == 50 && item.contains("Manually Ban the Player For 2 Weeks"))
            return new String[]{"Custom", "BAN", "Manually Banned for 2 Weeks", String.valueOf((long) 1.21e+9 + System.currentTimeMillis())};
        else if (slot == 51 && item.contains("Manually Ban the Player For 3 Weeks"))
            return new String[]{"Custom", "BAN", "Manually Banned for 3 Weeks", String.valueOf((long) 1.814e+9 + System.currentTimeMillis())};
        else if (slot == 52 && item.contains("Manually Ban the Player For 1 Month"))
            return new String[]{"Custom", "BAN", "Manually Banned For 1 Month", String.valueOf((long) 2.628e+9 + System.currentTimeMillis())};
        else if (slot == 53 && item.contains("Manually Ban the Player Permanently"))
            return new String[]{"Custom", "BAN", "Manually Banned Permanently", String.valueOf((long) 3.154e+12 + System.currentTimeMillis())};
        throw new PunishmentCalculationException("Could not translate clicked item to punishment properties", "Translating sent item to punishment properties");
    }

    public boolean isBanned(String targetUUID) {
        if (dbManager.ActivePunishmentCache.containsKey(targetUUID))
            for (Punishment punishment : getActivePunishments(targetUUID)) {
                if (punishment.isBan())
                    return true;
            }
        return false;
    }

    public boolean isMuted(String targetUUID) {
        if (dbManager.ActivePunishmentCache.containsKey(targetUUID))
            for (Punishment punishment : getActivePunishments(targetUUID)) {
                if (punishment.isMute())
                    return true;
            }
        return false;
    }

    public boolean hasActivePunishment(String targetUUID) {
        return dbManager.ActivePunishmentCache.containsKey(targetUUID);
    }

    public boolean hasPendingPunishment(String targetUUID) {
        return PendingPunishments.containsKey(targetUUID);
    }

    public int totalBans() {
        int bans = 0;
        for (Punishment punishment : dbManager.PunishmentCache.values()) {
            if (punishment.isBan())
                bans++;
        }
        return bans;
    }

    public int totalMutes() {
        int mutes = 0;
        for (Punishment punishment : dbManager.PunishmentCache.values()) {
            if (punishment.isMute())
                mutes++;
        }
        return mutes;
    }

    public int totalPunishments() {
        return dbManager.PunishmentCache.size();
    }

    public Punishment getBan(String targetUUID) {
        if (!isBanned(targetUUID)) return null;
        for (Punishment punishment : getActivePunishments(targetUUID)) {
            if (punishment.isBan())
                return punishment;
        }
        return null;
    }

    public Punishment getMute(String targetUUID) {
        if (!isMuted(targetUUID)) return null;
        for (Punishment punishment : getActivePunishments(targetUUID)) {
            if (punishment.isMute())
                return punishment;
        }
        return null;
    }

    public Punishment getPunishment(int id) {
        return dbManager.PunishmentCache.get(id);
    }

    public ArrayList<Punishment> getActivePunishments(String targetUUID) {
        if (hasActivePunishment(targetUUID)) {
            ArrayList<Punishment> activePunishments = new ArrayList<>();
            for (Integer id : dbManager.ActivePunishmentCache.get(targetUUID)) {
                activePunishments.add(getPunishment(id));
            }
            return activePunishments;
        }
        return null;
    }

    public int getActivePunishmentsAmount(String targetUUID) {
        return hasActivePunishment(targetUUID) ? getActivePunishments(targetUUID).size() : 0;
    }

    public Punishment getPendingPunishment(String targetUUID) {
        if (hasPendingPunishment(targetUUID))
            return dbManager.PunishmentCache.get(PendingPunishments.get(targetUUID));
        return null;
    }

    public TreeMap<Integer, Punishment> getPunishments(String targetUUID) {
        TreeMap<Integer, Punishment> punishments = new TreeMap<>();
        for (Punishment punishment : dbManager.PunishmentCache.values()) {
            if (punishment.getTargetUUID().equals(targetUUID))
                punishments.put(punishment.getId(), punishment);
        }
        return punishments;
    }

    public Punishment punishmentLookup(@Nullable Integer id, @NotNull Punishment.Type type, @NotNull Punishment.Reason reason, @NotNull String issueDate, @Nullable Long expiration, @NotNull String targetUUID) {
        if (id != null && dbManager.PunishmentCache.containsKey(id)) return dbManager.PunishmentCache.get(id);
        for (Punishment punishment : dbManager.PunishmentCache.values()) {
            if (punishment.getTargetUUID().equals(targetUUID))
                if (punishment.getIssueDate().equals(issueDate))
                    if (punishment.getType() == type)
                        if (punishment.getReason() == reason)
                            if (expiration != null && punishment.getExpiration() == expiration)
                                return punishment;
                            else
                                return punishment;
        }
        return null;
    }

    public void lock(String targetuuid) {
        if (!locked.contains(targetuuid))
            locked.add(targetuuid);
    }

    public void unlock(String targetuuid) {
        locked.remove(targetuuid);
    }

    private void removeActive(@NotNull Punishment punishment) {
        if (dbManager.ActivePunishmentCache.get(punishment.getTargetUUID()) == null || dbManager.ActivePunishmentCache.get(punishment.getTargetUUID()).size() == 1)
            dbManager.ActivePunishmentCache.remove(punishment.getTargetUUID());
        else
            dbManager.ActivePunishmentCache.get(punishment.getTargetUUID()).remove((Integer) getMute(punishment.getTargetUUID()).getId());
    }

    private void addActive(@NotNull Punishment punishment) {
        if (hasActivePunishment(punishment.getTargetUUID()))
            dbManager.ActivePunishmentCache.get(punishment.getTargetUUID()).add(punishment.getId());
        else {
            ArrayList<Integer> activePunishments = new ArrayList<>();
            activePunishments.add(punishment.getId());
            dbManager.ActivePunishmentCache.put(punishment.getTargetUUID(), activePunishments);
        }
    }

    public String getTimeLeftRaw(@NotNull Punishment punishment) {
        long millis = punishment.getExpiration() - System.currentTimeMillis();
        int yearsleft = (int) (millis / 3.154e+10);
        int monthsleft = (int) (millis / 2.628e+9 % 12);
        int weeksleft = (int) (millis / 6.048e+8 % 4.34524);
        int daysleft = (int) (millis / 8.64e+7 % 7);
        int hoursleft = (int) (millis / 3.6e+6 % 24);
        int minutesleft = (int) (millis / 60000 % 60);
        int secondsleft = (int) (millis / 1000 % 60);
        if (punishment.isPermanent())
            return String.valueOf(-1);
        if (secondsleft <= 0) {
            if (punishment.isActive()) {
                try {
                    remove(punishment, null, false, false, false);
                } catch (SQLException e) {
                    try {
                        throw new PunishmentsDatabaseException("Revoking punishment", punishment.getTargetName(), this.getClass().getName(), e);
                    } catch (PunishmentsDatabaseException pde) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pde);
                    }
                }
                PunisherPlugin.getLOGS().info(punishment.getTargetName() + "'s punishment expired so it was removed");
            }
            return String.valueOf(0);
        }
        if (yearsleft != 0)
            return yearsleft + "Yr " + monthsleft + "M " + weeksleft + "w " + daysleft + "d " + hoursleft + "h " + minutesleft + "m " + secondsleft + "s";
        else if (monthsleft != 0)
            return monthsleft + "M " + weeksleft + "w " + daysleft + "d " + hoursleft + "h " + minutesleft + "m " + secondsleft + "s";
        else if (weeksleft != 0)
            return weeksleft + "w " + daysleft + "d " + hoursleft + "h " + minutesleft + "m " + secondsleft + "s";
        else if (daysleft != 0)
            return daysleft + "d " + hoursleft + "h " + minutesleft + "m " + secondsleft + "s";
        else if (hoursleft != 0)
            return hoursleft + "h " + minutesleft + "m " + secondsleft + "s";
        else if (minutesleft != 0)
            return  minutesleft + "m " + secondsleft + "s";
        else
            return secondsleft + "s";
    }

    public String getTimeLeft(@NotNull Punishment punishment){
        String timeLeftRaw = getTimeLeftRaw(punishment);
        if (timeLeftRaw.equals(String.valueOf(0)))
            return "Punishment has Expired!";
        else if (timeLeftRaw.equals(String.valueOf(-1)))
            return "Punishment is Permanent!";
        else
            return "Expires in: " + timeLeftRaw;
    }

    public int getNextid() {
        return dbManager.PunishmentCache.isEmpty() ? 1 : dbManager.PunishmentCache.size() + 1;
    }

    public void NewPunishment(@NotNull Punishment punishment) {
        String message = punishment.getMessage();
        if (message.contains("%sinquo%"))
            message = message.replaceAll("%sinquo%", "'");
        if (message.contains("%dubquo%"))
            message = message.replaceAll("%dubquo%", "\"");
        if (message.contains("%bcktck%"))
            message = message.replaceAll("%bcktck%", "`");
        punishment.setMessage(message);
        if (locked.contains(punishment.getTargetUUID()))
            punishment.setStatus(Punishment.Status.Overridden);
        if (!dbManager.PunishmentCache.containsKey(punishment.getId()) && !locked.contains(punishment.getTargetUUID()))
            dbManager.PunishmentCache.put(punishment.getId(), punishment);
    }


}
