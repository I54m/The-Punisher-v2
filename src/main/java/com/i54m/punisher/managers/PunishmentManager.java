package com.i54m.punisher.managers;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.chats.StaffChat;
import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.PunishmentCalculationException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.storage.StorageManager;
import com.i54m.punisher.objects.ActivePunishments;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.Permissions;
import com.i54m.punisher.utils.UUIDFetcher;
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

public class PunishmentManager implements Manager {// TODO: 7/11/2020 fully implement metadata

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

    public void issue(@NotNull final Punishment punishment, @Nullable ProxiedPlayer player, final boolean addHistory, final boolean announce, final boolean minusRep) throws PunishmentsStorageException {
        @Nullable final ProxiedPlayer finalPlayer = player;
        WORKER_MANAGER.runWorker(new WorkerManager.Worker(() -> {
            try {
                storageManager.dumpNew();
            } catch (PunishmentsStorageException pse) {
                ERROR_HANDLER.log(pse);
                if (finalPlayer != null && finalPlayer.isConnected()) ERROR_HANDLER.alert(pse, finalPlayer);
                else ERROR_HANDLER.adminChatAlert(pse, ProxyServer.getInstance().getConsole());
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
                                StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                            else
                                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                        } else {
                            PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                            if (announce)
                                StaffChat.sendMessage(new ComponentBuilder("CONSOLE Warned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
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
                            StaffChat.sendMessage(new ComponentBuilder("CONSOLE Issued Warning on: " + targetname + " for: " + reasonMessage + ". Warning was originally issued by: " + playername).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                        PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + playername + ". This warning was originally issued on: " + punishment.getIssueDate());
                    }
                } else if (target == null || !target.isConnected()) {
                    PendingPunishments.put(punishment.getTargetUUID(), punishment.getId());
                    if (player != null && player.isConnected()) {
                        PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + player.getName() + ". Warning will be issued when they are online next.");
                        if (announce)
                            StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                        else
                            player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    } else {
                        PunisherPlugin.getLOGS().info(targetname + " Was Warned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid) + ". This warning will be issued when they are online next.");
                        if (announce)
                            StaffChat.sendMessage(new ComponentBuilder("CONSOLE Warned: " + targetname + " for: " + reasonMessage + ". This warning will be issued when they are online next.").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
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
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    else
                        player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Kicked for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Kicked: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
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
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    else
                        player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());

                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Muted for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Muted: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
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
                    if (punishment.isPermanent()) {
                        String banMessage = PLUGIN.getConfig().getString("PermBan Message").replace("%timeleft%", timeleft).replace("%reason%", reason);
                        target.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                    } else {
                        String banMessage = PLUGIN.getConfig().getString("TempBan Message").replace("%timeleft%", timeleft).replace("%reason%", reason);
                        target.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                    }
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
                        StaffChat.sendMessage(new ComponentBuilder(player.getName() + " Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    else
                        player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                } else {
                    PunisherPlugin.getLOGS().info(targetname + " Was Banned for: " + reasonMessage + " by: " + NameFetcher.getName(targetuuid));
                    if (announce)
                        StaffChat.sendMessage(new ComponentBuilder("CONSOLE Banned: " + targetname + " for: " + reasonMessage).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                    else
                        PLUGIN.getProxy().getConsole().sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("CONSOLE Banned: " + targetname + " for: " + reasonMessage).event(punishment.getHoverEvent()).color(ChatColor.RED).create());
                }
            }
            if (minusRep) REPUTATION_MANAGER.minusRep(targetuuid, repLoss);
        }
    }

    public void remove(@NotNull Punishment punishment, @Nullable ProxiedPlayer player, boolean removed, boolean removeHistory, boolean announce) throws PunishmentsStorageException {// TODO: 12/03/2020  need to rewrite this
        UUID targetuuid = punishment.getTargetUUID();
        storageManager.loadUser(targetuuid, true);
        String reason = punishment.getReason();
        String targetname = punishment.getTargetName();
        UUID removeruuid = player != null ? player.getUniqueId() : UUIDFetcher.getBLANK_UUID();
        if (targetname == null)
            targetname = NameFetcher.getName(punishment.getTargetUUID());

        if (!punishment.isActive()) {
            if (player != null)
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot remove a punishment that is not active!").color(ChatColor.RED).create());
            else
                ERROR_HANDLER.log(new PunishmentCalculationException("Punishment is not active!", "Punishment removal"));
            return;
        }

        if (punishment.hasMetaData())
            if (punishment.getMetaData().isLocked()) {
                if (player != null)
                    player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("This punishment is currently locked and must be unlocked before any actions can be taken on it!").color(ChatColor.RED).create());// TODO: 9/11/2020 make a way for admins to unlock/lock punishments
                else
                    ERROR_HANDLER.log(new PunishmentCalculationException("Punishment is locked!", "Punishment removal"));
                return;
            }


        if (isMuted(targetuuid) && punishment.isMute()) {
            UUID oldPunisherID = getMute(targetuuid).getPunisherUUID();
            if (!Permissions.higher(removeruuid, oldPunisherID) && player != null) {
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot unmute: " + targetname + " as they have an active mute by someone with higher permissions than you!").color(ChatColor.RED).create());
                return;
            }
            if (player != null && player.isConnected()) {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " unmuted: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                else
                    player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently unmuted " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                PunisherPlugin.getLOGS().info(targetname + " was unmuted by: " + player.getName());
            } else {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder("CONSOLE unmuted: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                PunisherPlugin.getLOGS().info(targetname + " was unmuted by: " + NameFetcher.getName(targetuuid));
            }
        } else if (isBanned(targetuuid) && punishment.isBan()) {
            UUID oldPunisherID = getBan(targetuuid).getPunisherUUID();
            if (!Permissions.higher(removeruuid, oldPunisherID) && player != null) {
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("You cannot unban: " + targetname + " as they have an active ban by someone with higher permissions than you!").color(ChatColor.RED).create());
                return;
            }
            if (player != null && player.isConnected()) {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " unbanned: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                else
                    player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("Silently unbanned " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                PunisherPlugin.getLOGS().info(targetname + " was unbanned by: " + player.getName());
            } else {
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder("CONSOLE unbanned: " + targetname).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                PunisherPlugin.getLOGS().info(targetname + " was unbanned by: " + NameFetcher.getName(targetuuid));
            }
        }
        if (punishment.isRepBan())
            unlock(targetuuid);

        punishment.setRemoverUUID(removeruuid);

        if (removed)
            punishment.setStatus(Punishment.Status.Removed);
        else
            punishment.setStatus(Punishment.Status.Expired);

        removeActive(punishment);

        if (removeHistory) {// TODO: 22/06/2020  need to rewrite this part to check for MetaData.appliesToHistory
            punishment.getMetaData().setAppliesToHistory(false);
            if (player != null) {
                PunisherPlugin.getLOGS().info(player.getName() + " removed punishment: " + reason + " on player: " + targetname + " through unpunish");
                player.sendMessage(new ComponentBuilder(PLUGIN.getPrefix()).append("The punishment: " + reason + " on: " + targetname + " has been removed!").color(ChatColor.RED).event(punishment.getHoverEvent()).create());
                if (announce)
                    StaffChat.sendMessage(new ComponentBuilder(player.getName() + " unpunished: " + targetname + " for the offence: " + reason).color(ChatColor.RED).event(punishment.getHoverEvent()).create());
            }
        }
        storageManager.updatePunishment(punishment);
    }

    @Deprecated
    public long calculateExpiration(Punishment punishment) throws PunishmentsStorageException {//todo verify that this is actually calculating correctly
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

    public long calculateExpiration(UUID targetUUID, String reason) throws PunishmentsStorageException {
        if (reason.toUpperCase().contains("MANUAL:")) {
            String[] reasonargs = reason.toUpperCase().split(":");
            Punishment.Type type = Punishment.Type.valueOf(reasonargs[1]);
            if (type != Punishment.Type.BAN && type != Punishment.Type.MUTE) return 0;
            return translateExpiration(reasonargs[2]);
        }

        int punishmentno = storageManager.getOffences(targetUUID, reason);
        punishmentno++;
        if (punishmentno > storageManager.getPunishmentReasons().get(reason.toUpperCase()))
            punishmentno = storageManager.getPunishmentReasons().get(reason.toUpperCase());

        Punishment.Type type = Punishment.Type.valueOf(PLUGIN.getPunishments().getString(reason + "." + punishmentno + ".type").toUpperCase());
        if (type != Punishment.Type.BAN && type != Punishment.Type.MUTE) return 0;

        return translateExpiration(PLUGIN.getPunishments().getString(reason + "." + punishmentno + ".length"));
    }

    public long translateExpiration(String length) { // TODO: 15/11/2020 implement more widely
        if (length.toLowerCase().contains("perm"))
            return (long) 3.154e+12 + System.currentTimeMillis();
        else if (length.endsWith("M"))
            return (long) 2.628e+9 * (long) Integer.parseInt(length.replace("M", "")) + System.currentTimeMillis();
        else if (length.toLowerCase().endsWith("w"))
            return (long) 6.048e+8 * (long) Integer.parseInt(length.replace("w", "")) + System.currentTimeMillis();
        else if (length.toLowerCase().endsWith("d"))
            return (long) 8.64e+7 * (long) Integer.parseInt(length.replace("d", "")) + System.currentTimeMillis();
        else if (length.toLowerCase().endsWith("h"))
            return (long) 3.6e+6 * (long) Integer.parseInt(length.replace("h", "")) + System.currentTimeMillis();
        else if (length.endsWith("m"))
            return 60000 * (long) Integer.parseInt(length.replace("m", "")) + System.currentTimeMillis();
        else if (length.toLowerCase().endsWith("s"))
            return 1000 * (long) Integer.parseInt(length.replace("s", "")) + System.currentTimeMillis();
        else return 0;
    }

    public Punishment.Type calculateType(UUID targetuuid, String reason) throws PunishmentsStorageException {
        if (reason.toUpperCase().contains("MANUAL:"))
            return Punishment.Type.valueOf(reason.toUpperCase().split(":")[1]);

        int punishmentno = storageManager.getOffences(targetuuid, reason);
        punishmentno++;
        if (punishmentno > storageManager.getPunishmentReasons().get(reason.toUpperCase()))
            punishmentno = storageManager.getPunishmentReasons().get(reason.toUpperCase());

        return Punishment.Type.valueOf(PLUGIN.getPunishments().getString(reason + "." + punishmentno + ".type").toUpperCase());
    }

    public double calculateRepLoss(@NotNull Punishment punishment) throws PunishmentsStorageException {
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

    public Punishment constructPunishment(UUID targetUUID, String reason, UUID punisherUUID) {
        try {
            return new Punishment(
                    calculateType(targetUUID, reason),
                    reason,
                    calculateExpiration(targetUUID, reason),
                    targetUUID,
                    NameFetcher.getName(targetUUID),
                    punisherUUID,
                    reason.toLowerCase().replace("_", " ").replace(":", " "),
                    new Punishment.MetaData());
        } catch (PunishmentsStorageException pse) {
            ERROR_HANDLER.log(new PunishmentCalculationException("Storage exception in either type or expiration calculation", "Punishment construction", pse));
            return null;
        }
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
        } catch (PunishmentsStorageException pse) {
            ERROR_HANDLER.log(pse);
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
        if (!hasActivePunishment(targetuuid))
            storageManager.getActivePunishmentCache().put(targetuuid, activePunishments);
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
                        throw new PunishmentsStorageException("Revoking punishment", punishment.getTargetName(), this.getClass().getName(), e);
                    } catch (PunishmentsStorageException pse) {
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pse);
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
