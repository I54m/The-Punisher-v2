package com.i54mpenguin.punisher.managers;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.chats.StaffChat;
import com.i54mpenguin.punisher.exceptions.ManagerNotStartedException;
import com.i54mpenguin.punisher.exceptions.PunishmentCalculationException;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.storage.StorageManager;
import com.i54mpenguin.punisher.objects.ActivePunishments;
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
import java.sql.SQLException;
import java.util.*;

public class PunishmentManager implements Manager {

    private static final WorkerManager WORKER_MANAGER = WorkerManager.getINSTANCE();
    private static final ReputationManager REPUTATION_MANAGER = ReputationManager.getINSTANCE();
    @Getter
    private static final PunishmentManager INSTANCE = new PunishmentManager();
    private static StorageManager storageManager;
    private final ArrayList<UUID> lockedUsers = new ArrayList<>();
    private final Map<UUID, Integer> PendingPunishments = new HashMap<>();
    private boolean locked = true;

    private PunishmentManager() {
    }

    @Override
    public void start() {
        if (!locked) {
            ERROR_HANDLER.log(new Exception("Punishment Manager Already started!"));
            return;
        }
        storageManager = PLUGIN.getStorageManager();
        locked = false;
    }

    @Override
    public boolean isStarted() {
        return !locked;
    }

    @Override
    public void stop() {
        if (locked) {
            ERROR_HANDLER.log(new ManagerNotStartedException(this.getClass()));
            return;
        }
        locked = true;

    }

    public void issue(@NotNull final Punishment punishment, @Nullable ProxiedPlayer player, final boolean addHistory, final boolean announce, final boolean minusRep) throws PunishmentsDatabaseException {
        @Nullable final ProxiedPlayer finalPlayer = player;
        WORKER_MANAGER.runWorker(new WorkerManager.Worker(() -> {
            try {
                storageManager.dumpNew();
            } catch (PunishmentsDatabaseException pde) {
                ERROR_HANDLER.log(pde);
                if (finalPlayer != null && finalPlayer.isConnected()) ERROR_HANDLER.alert(pde, finalPlayer);
                else ERROR_HANDLER.adminChatAlert(pde, ProxyServer.getInstance().getConsole());
            }
        }));

        if (player != null && player.isConnected() && punishment.getPunisherUUID() == null)
            storageManager.updatePunishment(punishment.setPunisherUUID(player.getUniqueId()));
        if ((punishment.isMute() || punishment.isBan()) && !punishment.hasExpiration())
            storageManager.updatePunishment(punishment.setExpiration(calculateExpiration(punishment)));

        punishment.verify();

        if (!punishment.isPending())
            storageManager.updatePunishment(punishment.setStatus(Punishment.Status.Pending));
        boolean reissue = false;
        if (PendingPunishments.containsValue(punishment.getId()))
            reissue = true;

        Punishment.Type type = punishment.getType();
        String reason = punishment.getReason();
        UUID targetuuid = punishment.getTargetUUID();
        String targetname = punishment.getTargetName();
        UUID punisherUUID = punishment.getPunisherUUID();
        long expiration = punishment.getExpiration();
        double repLoss = calculateRepLoss(punishment);
        ProxiedPlayer target;
        try {
            target = PLUGIN.getProxy().getPlayer(targetuuid);
        } catch (NullPointerException npe) {
            target = null;
        }
        player = player == null ? PLUGIN.getProxy().getPlayer(punisherUUID) : player;

        if (isMuted(targetuuid) && type == Punishment.Type.MUTE) {
            UUID oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(player, oldPunisherID) && !punisherUUID.equals(UUIDFetcher.getBLANK_UUID())) {
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot mute: " + targetname + " as they have an active mute by someone with higher permissions than you!").color(ChatColor.RED).event(getMute(targetuuid).getHoverEvent()).create());
                return;
            }
        } else if (isBanned(targetuuid) && type == Punishment.Type.BAN) {
            UUID oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(player, oldPunisherID) && !punisherUUID.equals(UUIDFetcher.getBLANK_UUID())) {
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot ban: " + targetname + " as they have an active ban by someone with higher permissions than you!").color(ChatColor.RED).event(getMute(targetuuid).getHoverEvent()).create());
                return;
            }
        }

        if (isLocked(targetuuid)) {
            if (player != null)
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Punishing " + targetname + " is currently locked! Please try again later!").color(ChatColor.RED).create());
            return;
        }

        storageManager.createHistory(targetuuid);
        storageManager.createStaffHistory(punisherUUID);

        switch (type) {
            case WARN: {
                String reasonMessage;
                if (punishment.getMessage() != null)
                    reasonMessage = punishment.getMessage();
                else
                    reasonMessage = reason;
                if (target != null && target.isConnected()) {
                    if (PLUGIN.getConfig().getBoolean("Warn Sound.Enabled")) {
                        try {
                            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
                            DataOutputStream out = new DataOutputStream(outbytes);
                            out.writeUTF("playsound");
                            out.writeUTF(PLUGIN.getConfig().getString("Warn Sound.Sound"));
                            target.getServer().sendData("punisher:minor", outbytes.toByteArray());
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                    if (PLUGIN.getConfig().getBoolean("Warn Title.Enabled")) {
                        String titleText = PLUGIN.getConfig().getString("Warn Title.Title Message");
                        String subtitleText = PLUGIN.getConfig().getString("Warn Title.Subtitle Message");
                        if (punishment.getMessage() != null) {
                            titleText = titleText.replace("%reason%", punishment.getMessage());
                            subtitleText = subtitleText.replace("%reason%", punishment.getMessage());
                        } else {
                            titleText = titleText.replace("%reason%", reason);
                            subtitleText = subtitleText.replace("%reason%", reason);
                        }
                        ProxyServer.getInstance().createTitle().title(new TextComponent(ChatColor.translateAlternateColorCodes('&', titleText)))
                                .subTitle(new TextComponent(ChatColor.translateAlternateColorCodes('&', subtitleText)))
                                .fadeIn(PLUGIN.getConfig().getInt("Warn Title.Fade In"))
                                .stay(PLUGIN.getConfig().getInt("Warn Title.Stay")).fadeOut(PLUGIN.getConfig().getInt("Warn Title.Fade Out"))
                                .send(target);
                    }
                    String warnMessage = PLUGIN.getConfig().getString("Warn Message");
                    if (punishment.getMessage() != null)
                        warnMessage = warnMessage.replace("%reason%", punishment.getMessage());
                    else
                        warnMessage = warnMessage.replace("%reason%", reason);
                    target.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', warnMessage)).create());
                    if (!reissue) {
                        storageManager.updatePunishment(punishment.setIssueDate().setStatus(Punishment.Status.Issued));
                        if (player != null && player.isConnected()) {
                            PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + player.getName());
                            if (announce)
                                StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                            else
                                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                        } else {
                            PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                            if (announce)
                                StaffChat.sendMessage(new ComponentBuilder("CONSOLE Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                            else
                                PLUGIN.getProxy().getConsole().sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("CONSOLE Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).create());
                        }
                    } else {
                        storageManager.updatePunishment(punishment.setStatus(Punishment.Status.Issued));
                        PendingPunishments.remove(punishment.getTargetUUID());
                        String playername = NameFetcher.getName(punisherUUID);
                        ProxiedPlayer player2 = (player != null && player.isConnected()) ? player : ProxyServer.getInstance().getPlayer(punisherUUID);
                        if (player2 != null && player2.isConnected())
                            player2.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Your warning on " + targetname + " for: " + reasonMessage + " has now been issued.").color(ChatColor.RED).create());
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
                            player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    } else {
                        PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid) + ". This warning will be issued when they are online next.");
                        if (announce)
                            StaffChat.sendMessage(new ComponentBuilder("CONSOLE Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                        else
                            PLUGIN.getProxy().getConsole().sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("CONSOLE Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    }
                }
                if (addHistory) {
                    storageManager.incrementHistory(punishment);
                    storageManager.incrementStaffHistory(punishment);
                }
                if (minusRep) REPUTATION_MANAGER.minusRep(targetuuid, repLoss);
                return;
            }
            case KICK: {
                String reasonMessage;
                if (punishment.getMessage() != null) {
                    reasonMessage = punishment.getMessage();
                } else {
                    reasonMessage = reason;
                }
                if (target != null && target.isConnected()) {
                    String kickMessage = PLUGIN.getConfig().getString("Kick Message");
                    kickMessage = kickMessage.replace("%reason%", reasonMessage);
                    target.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', kickMessage)));
                }
                if (addHistory) {
                    storageManager.incrementHistory(punishment);
                    storageManager.incrementStaffHistory(punishment);
                }
                if (player != null && player.isConnected()) {
                    PunisherPlugin.getLOGS().info(targetname + " Was Kicked for: " + reasonMessage + " by: " + player.getName());
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Kicked for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        PLUGIN.getProxy().getConsole().sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("CONSOLE Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                }
                if (minusRep) REPUTATION_MANAGER.minusRep(targetuuid, repLoss);
                return;
            }
            case MUTE: {
                String reasonMessage;
                if (punishment.getMessage() != null) {
                    reasonMessage = punishment.getMessage();
                } else {
                    reasonMessage = reason;
                }
                String timeleft = getTimeLeft(punishment);
                if (target != null && target.isConnected()) {
                    if (PLUGIN.getConfig().getBoolean("Mute Sound.Enabled")) {
                        try {
                            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
                            DataOutputStream out = new DataOutputStream(outbytes);
                            out.writeUTF("playsound");
                            out.writeUTF(PLUGIN.getConfig().getString("Mute Sound.Sound"));
                            target.getServer().sendData("punisher:minor", outbytes.toByteArray());
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                    if (PLUGIN.getConfig().getBoolean("Mute Title.Enabled")) {
                        String titleText = PLUGIN.getConfig().getString("Mute Title.Title Message");
                        String subtitleText = PLUGIN.getConfig().getString("Mute Title.Subtitle Message");
                        titleText = titleText.replace("%reason%", reasonMessage);
                        subtitleText = subtitleText.replace("%reason%", reasonMessage);
                        if (announce)
                            ProxyServer.getInstance().createTitle().title(new TextComponent(ChatColor.translateAlternateColorCodes('&', titleText)))
                                    .subTitle(new TextComponent(ChatColor.translateAlternateColorCodes('&', subtitleText)))
                                    .fadeIn(PLUGIN.getConfig().getInt("Mute Title.Fade In"))
                                    .stay(PLUGIN.getConfig().getInt("Mute Title.Stay")).fadeOut(PLUGIN.getConfig().getInt("Mute Title.Fade Out"))
                                    .send(target);
                    }
                    String muteMessage;
                    if (punishment.isPermanent())
                        muteMessage = PLUGIN.getConfig().getString("PermMute Message").replace("%reason%", reasonMessage).replace("%timeleft%", timeleft);
                    else
                        muteMessage = PLUGIN.getConfig().getString("TempMute Message").replace("%reason%", reasonMessage).replace("%timeleft%", timeleft);
                    if (announce)
                        target.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', muteMessage)).create());
                }
                if (addHistory) {
                    storageManager.incrementHistory(punishment);
                    storageManager.incrementStaffHistory(punishment);
                }
                if (hasActivePunishment(targetuuid) && isMuted(targetuuid)) {
                    storageManager.updatePunishment(getMute(targetuuid).setStatus(Punishment.Status.Overridden));// TODO: 8/03/2020 maybe make a way for the expiration to stack rather than just overriding it although doesn't litebans do this too?
                    removeActive(punishment);
                }
                storageManager.updatePunishment(punishment.setExpiration(expiration)
                        .setIssueDate()
                        .setStatus(Punishment.Status.Active));
                addActive(punishment);
                PendingPunishments.remove(targetuuid);
                if (player != null && player.isConnected()) {
                    PunisherPlugin.getLOGS().info(targetname + " Was Muted for: " + reasonMessage + " by: " + player.getName());
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());

                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Muted for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        PLUGIN.getProxy().getConsole().sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("CONSOLE Muted: " + targetname + " for: " + reasonMessage).event(punishment.getHoverEvent()).color(ChatColor.RED).create());
                }
                if (minusRep) REPUTATION_MANAGER.minusRep(targetuuid, repLoss);
                return;
            }
            case BAN: {
                String reasonMessage;
                if (punishment.getMessage() != null)
                    reasonMessage = punishment.getMessage();
                else
                    reasonMessage = reason;
                String timeleft = getTimeLeft(punishment);
                if (target != null && target.isConnected()) {
                    String banMessage;
                    if (punishment.isPermanent())
                        banMessage = PLUGIN.getConfig().getString("PermBan Message").replace("%timeleft%", timeleft);
                    else
                        banMessage = PLUGIN.getConfig().getString("TempBan Message").replace("%timeleft%", timeleft);
                    banMessage = banMessage.replace("%reason%", reasonMessage);
                    target.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                }
                if (addHistory) {
                    storageManager.incrementHistory(punishment);
                    storageManager.incrementStaffHistory(punishment);
                }
                if (hasActivePunishment(targetuuid) && isBanned(targetuuid)) {
                    storageManager.updatePunishment(getBan(targetuuid).setStatus(Punishment.Status.Overridden));
                    removeActive(punishment);
                }
                storageManager.updatePunishment(punishment.setExpiration(expiration)
                        .setIssueDate()
                        .setStatus(Punishment.Status.Active));
                addActive(punishment);
                PendingPunishments.remove(targetuuid);
                if (player != null && player.isConnected()) {
                    PunisherPlugin.getLOGS().info(targetname + " Was Banned for: " + reasonMessage + " by: " + player.getName());
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Banned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                    else
                        PLUGIN.getProxy().getConsole().sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("CONSOLE Banned: " + targetname + " for: " + reasonMessage).event(punishment.getHoverEvent()).color(ChatColor.RED).create());
                }
            }
            if (minusRep) REPUTATION_MANAGER.minusRep(targetuuid, repLoss);
        }
    }

    public void remove(@NotNull Punishment punishment, @Nullable ProxiedPlayer player, boolean removed, boolean removeHistory, boolean announce) throws PunishmentsDatabaseException {// TODO: 12/03/2020  need to rewrite this
        UUID targetuuid = punishment.getTargetUUID();
        String reason = punishment.getReason();
        String targetname = punishment.getTargetName();
        UUID removeruuid = player != null ? player.getUniqueId() : UUIDFetcher.getBLANK_UUID();
        if (targetname == null)
            targetname = NameFetcher.getName(punishment.getTargetUUID());

        if (isMuted(targetuuid) && punishment.getType() == Punishment.Type.MUTE) {
            UUID oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(removeruuid, oldPunisherID) && player != null) {
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot unmute: " + targetname + " as they have an active mute by someone with higher permissions than you!").color(ChatColor.RED).create());
                return;
            }
        } else if (isBanned(targetuuid) && punishment.getType() == Punishment.Type.BAN) {
            UUID oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(removeruuid, oldPunisherID) && player != null) {
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot unban: " + targetname + " as they have an active ban by someone with higher permissions than you!").color(ChatColor.RED).create());
                return;
            }
        }

        if (isMuted(targetuuid) && punishment.getType() == Punishment.Type.MUTE) {
            if (player != null && player.isConnected()) {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Unmuted: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
                else
                    player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently unmuted " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
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
                    player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently unbanned " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
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
        storageManager.updatePunishment(removed ? punishment.setStatus(Punishment.Status.Removed) : punishment.setStatus(Punishment.Status.Expired));
        removeActive(punishment);

        if (removeHistory) {// TODO: 22/06/2020  need to rewrite this part
//            String sqlhist = "SELECT * FROM `history` WHERE UUID='" + targetuuid + "'";
//            PreparedStatement stmthist = storageManager.connection.prepareStatement(sqlhist);
//            ResultSet resultshist = stmthist.executeQuery();
//            if (resultshist.next()) {
//                int current;
//                if (punishment.isCustom()) {
//                    current = resultshist.getInt("Manual_Punishments");
//                } else {
//                    current = resultshist.getInt(reason.toString());
//                }
//                if (current != 0) {
//                    current--;
//                } else {
//                    if (player != null) {
//                        player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append(targetname + " Already has 0 punishments for that offence!").color(ChatColor.RED).create());
//                        return;
//                    }
//                }
//                String collumn;
//                if (reason.contains("Manual")) {
//                    collumn = "Manual_Punishments";
//                } else {
//                    collumn = reason;
//                }
//                String sqlhistupdate = "UPDATE `history` SET `" + collumn + "`='" + current + "' WHERE `UUID`='" + targetuuid + "' ;";
//                PreparedStatement stmthistupdate = storageManager.connection.prepareStatement(sqlhistupdate);
//                stmthistupdate.executeUpdate();
//                stmthistupdate.close();

            if (player != null) {
                PunisherPlugin.getLOGS().info(player.getName() + " removed punishment: " + reason + " on player: " + targetname + " Through unpunish");
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("The punishment: " + reason + " on: " + targetname + " has been removed!").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Unpunished: " + targetname + " for the offence: " + reason).color(ChatColor.RED).event(punishment.getHoverEvent()).create(), true);
            }
        }
    }

    public long calculateExpiration(Punishment punishment) throws PunishmentsDatabaseException {//todo verify that this is actually calculating correctly
        switch (punishment.getReason()) {
            case "Other_Minor_Offence":
                return (long) 6.048e+8 + System.currentTimeMillis();
            case "Other_Major_Offence":
                return (long) 2.628e+9 + System.currentTimeMillis();
            case "Other_Offence":
                return (long) 1.8e+6 + System.currentTimeMillis();
        }
        int punishmentno = storageManager.getOffences(punishment.getTargetUUID(), punishment.getReason());
        punishmentno++;
        if (punishmentno > 5) punishmentno = 5;
        return (long) (60000 * PLUGIN.getPunishments().getDouble(punishment.getReason() + "." + punishmentno + ".length")) + System.currentTimeMillis();
    }

    public Punishment.Type calculateType(UUID targetuuid, String reason) throws PunishmentCalculationException, PunishmentsDatabaseException {
        if (reason.contains("Manual") || reason.equals("Custom") || reason.contains("Other"))
            throw new PunishmentCalculationException("Punishment reason must be an automatic punishment type when calculating punishment type!", "type");
        int punishmentno = storageManager.getOffences(targetuuid, reason);
        punishmentno++;
        if (punishmentno > 5) punishmentno = 5;
        return Punishment.Type.valueOf(PLUGIN.getPunishments().getString(reason + "." + punishmentno + ".type").toUpperCase());
    }

    public double calculateRepLoss(@NotNull Punishment punishment) throws PunishmentsDatabaseException {
        if (punishment.isManual() && (punishment.isBan() || punishment.isMute()))
            return PLUGIN.getConfig().getDouble("ReputationScale." + punishment.getType().toString() + "." + 5);
        if (punishment.isManual() && (punishment.isWarn() || punishment.isKick()))
            return PLUGIN.getConfig().getDouble("ReputationScale." + punishment.getType().toString());
        if (punishment.isBan() || punishment.isMute()) {
            int offence = storageManager.getOffences(punishment.getTargetUUID(), punishment.getReason());
            offence++;
            if (offence > 5) offence = 5;
            return PLUGIN.getConfig().getDouble("ReputationScale." + punishment.getType().toString() + "." + offence);
        } else {
            return PLUGIN.getConfig().getDouble("ReputationScale." + punishment.getType().toString());
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

    public boolean isBanned(UUID targetUUID) {
        if (hasActivePunishment(targetUUID))
            return storageManager.getActivePunishmentCache().get(targetUUID).banActive();
        else return false;
    }

    public boolean isMuted(UUID targetUUID) {
        if (hasActivePunishment(targetUUID))
            return storageManager.getActivePunishmentCache().get(targetUUID).muteActive();
        else return false;
    }

    public boolean hasActivePunishment(UUID targetUUID) {
        return storageManager.getActivePunishmentCache().containsKey(targetUUID);
    }

    public boolean hasPendingPunishment(UUID targetUUID) {
        return PendingPunishments.containsKey(targetUUID);
    }

    // TODO: 22/06/2020  need to redo the below totals as the punishment cache will no longer contain all punishments
    public int totalBans() {
        int bans = 0;
        for (Punishment punishment : storageManager.getPunishmentCache().values()) {
            if (punishment.isBan())
                bans++;
        }
        return bans;
    }

    public int totalMutes() {
        int mutes = 0;
        for (Punishment punishment : storageManager.getPunishmentCache().values()) {
            if (punishment.isMute())
                mutes++;
        }
        return mutes;
    }

    public int totalPunishments() {
        return storageManager.getPunishmentCache().size();
    }

    public Punishment getBan(UUID targetUUID) {
        if (!isBanned(targetUUID)) return null;
        return getActivePunishments(targetUUID).getBan();
    }

    public Punishment getMute(UUID targetUUID) {
        if (!isMuted(targetUUID)) return null;
        return getActivePunishments(targetUUID).getMute();
    }

    public Punishment getPunishment(int id) {
        try {
            return storageManager.getPunishmentFromId(id);
        } catch (PunishmentsDatabaseException pde) {
            ERROR_HANDLER.log(pde);
            return null;
        }
    }

    public ActivePunishments getActivePunishments(UUID targetUUID) {
        if (hasActivePunishment(targetUUID)) {
            return storageManager.getActivePunishmentCache().get(targetUUID);
        }
        return null;
    }

    public Punishment getPendingPunishment(UUID targetUUID) {
        if (hasPendingPunishment(targetUUID))
            return storageManager.getPunishmentCache().get(PendingPunishments.get(targetUUID));
        return null;
    }

    public TreeMap<Integer, Punishment> getPunishments(UUID targetUUID) {
        TreeMap<Integer, Punishment> punishments = new TreeMap<>();
        for (Punishment punishment : storageManager.getPunishmentCache().values()) {
            if (punishment.getTargetUUID().equals(targetUUID))
                punishments.put(punishment.getId(), punishment);
        }
        return punishments;
    }

    public Punishment punishmentLookup(@Nullable Integer id, @NotNull Punishment.Type type, @NotNull String reason, @NotNull String issueDate, @Nullable Long expiration, @NotNull UUID targetUUID) {
        if (id != null && storageManager.getPunishmentCache().containsKey(id))
            return storageManager.getPunishmentCache().get(id);
        for (Punishment punishment : storageManager.getPunishmentCache().values()) {
            if (punishment.getTargetUUID().equals(targetUUID))
                if (punishment.getIssueDate().equals(issueDate))
                    if (punishment.getType() == type)
                        if (punishment.getReason().equals(reason))
                            if (expiration != null && punishment.getExpiration() == expiration)
                                return punishment;
                            else
                                return punishment;
        }
        return null;
    }

    public void lock(UUID targetuuid) {
        if (!isLocked(targetuuid))
            lockedUsers.add(targetuuid);
    }

    public void unlock(UUID targetuuid) {
        lockedUsers.remove(targetuuid);
    }

    private void removeActive(@NotNull Punishment punishment) {
        UUID targetuuid = punishment.getTargetUUID();
        if (hasActivePunishment(targetuuid)) {
            ActivePunishments activePunishments = storageManager.getActivePunishmentCache().get(targetuuid);
            if (punishment.getType() == Punishment.Type.BAN) activePunishments.setBan(null);
            else if (punishment.getType() == Punishment.Type.MUTE) activePunishments.setMute(null);
        }
    }

    private void addActive(@NotNull Punishment punishment) {
        UUID targetuuid = punishment.getTargetUUID();
        ActivePunishments activePunishments = hasActivePunishment(targetuuid) ? getActivePunishments(targetuuid) : new ActivePunishments();
        if (punishment.getType() == Punishment.Type.BAN) activePunishments.setBan(punishment);
        else if (punishment.getType() == Punishment.Type.MUTE) activePunishments.setMute(punishment);
        if (!hasActivePunishment(targetuuid)) storageManager.getActivePunishmentCache().put(targetuuid, activePunishments);
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
            return minutesleft + "m " + secondsleft + "s";
        else
            return secondsleft + "s";
    }

    public String getTimeLeft(@NotNull Punishment punishment) {
        String timeLeftRaw = getTimeLeftRaw(punishment);
        if (timeLeftRaw.equals(String.valueOf(0)))
            return "Punishment has Expired!";
        else if (timeLeftRaw.equals(String.valueOf(-1)))
            return "Punishment is Permanent!";
        else
            return "Expires in: " + timeLeftRaw;
    }

    public boolean isLocked(UUID targetUUID) {
        return lockedUsers.contains(targetUUID);
    }

    @Nullable
    public Punishment override(@NotNull Punishment punishment1, @NotNull Punishment punishment2) {
        if (punishment1.getType() != punishment2.getType()) return null;
        if (Permissions.higher(punishment1.getPunisherUUID(), punishment2.getPunisherUUID())) return punishment1;
        else return punishment2;
    }
}
