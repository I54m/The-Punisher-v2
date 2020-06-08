package com.i54mpenguin.punisher.objects;

import lombok.Getter;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.exceptions.PunishmentIssueException;
import me.fiftyfour.punisher.universal.util.NameFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Punishment {

    @Getter
    private final int id;
    @Getter
    private final Type type;
    @Getter
    private final Reason reason;
    private String issueDate;
    @Getter
    private long expiration;
    @Getter
    private final String targetUUID;
    @Getter
    private String targetName, punisherUUID, message;
    @Getter
    private Status status;
    @Getter
    private String removerUUID;

    public Punishment(@NotNull Type type, @NotNull Reason reason, @Nullable Long expiration, @NotNull String targetUUID, @NotNull String targetName, @NotNull String punisherUUID, @Nullable String message) {
        PunishmentManager punmgr = PunishmentManager.getINSTANCE();
        this.id = punmgr.getNextid();
        this.type = type;
        this.reason = reason;
        this.expiration = expiration == null ? 0 : expiration;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.punisherUUID = punisherUUID;
        this.message = message;

        this.status = Status.Created;
        punmgr.NewPunishment(this);
    }

    public Punishment(@NotNull Integer id, @NotNull Type type, @NotNull Reason reason, @NotNull String issueDate, @Nullable Long expiration, @NotNull String targetUUID, @NotNull String targetName, @NotNull String punisherUUID, @Nullable String message, @NotNull Status status, @Nullable String removerUUID) {
        this.id = id;
        this.type = type;
        this.reason = reason;
        this.issueDate = issueDate;
        this.expiration = expiration == null ? 0 : expiration;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.punisherUUID = punisherUUID;
        this.message = message;
        this.status = status;
        if (removerUUID != null)
            this.removerUUID = removerUUID;
    }

    public enum Type {
        BAN, KICK, MUTE, WARN, ALL
    }

    public enum Status {
        Created, Pending, Active, Issued, Overridden, Expired, Removed
    }

    public enum Reason {
        Minor_Chat_Offence, Major_Chat_Offence, DDoS_DoX_Threats, Inappropriate_Link, Scamming, X_Raying, AutoClicker, Fly_Speed_Hacking, Disallowed_Mods, Malicious_PvP_Hacks, Server_Advertisement,
        Greifing, Exploiting, Tpa_Trapping, Impersonation, Other_Minor_Offence, Other_Major_Offence, Other_Offence, Custom
    }

    public boolean isPermanent() {
        long millis;
        millis = expiration - System.currentTimeMillis();
        int yearsleft = (int) (millis / 3.154e+10);
        return yearsleft >= 10;
    }

    public boolean isManual() {
        return reason.toString().contains("Other") || reason == Reason.Custom;
    }

    public boolean isBan() {
        return this.type == Type.BAN;
    }

    public boolean isRepBan() {
        return isBan() && punisherUUID.equals("CONSOLE") && isCustom() && isPermanent() && message.contains("Overly Toxic");
    }

    public boolean isMute() {
        return this.type == Type.MUTE;
    }

    public boolean isWarn() {
        return this.type == Type.WARN;
    }

    public boolean isKick() {
        return this.type == Type.KICK;
    }

    public boolean isTest() {
        return this.type == Type.ALL;
    }

    public boolean isCustom() {
        return this.reason == Reason.Custom;
    }

    public boolean isPending() {
        return this.status == Status.Pending;
    }

    public boolean isActive() {
        return this.status == Status.Active;
    }

    public boolean isIssued() {
        return this.status == Status.Issued;
    }

    public boolean isOverridden() {
        return this.status == Status.Overridden;
    }

    public boolean isExpired() {
        return this.status == Status.Expired;
    }

    public boolean isRemoved() {
        return this.status == Status.Removed;
    }

    public boolean hasExpiration() {
        return this.expiration != 0;
    }

    public boolean hasMessage() {
        return !this.message.isEmpty();
    }

    public void verify() {
        String name = NameFetcher.getName(targetUUID);

        //targetname check
        if (targetName == null && name != null)
            targetName = name;

        //punisher uuid check
        if (punisherUUID == null)
            punisherUUID = "CONSOLE";

        //expiration check
        if (isWarn() || isKick() || isTest())
            expiration = 0;
        else if ((isBan() || isMute()) && !hasExpiration())
            expiration = (long) 3.154e+12 + System.currentTimeMillis();

        //message check
        if (isCustom() && !hasMessage())
            message = "No Reason Given";
    }

    public Punishment setIssueDate() {
        this.issueDate = this.issueDate == null ? DateTimeFormatter.ofPattern("dd/MMM/yy HH:mm").format(LocalDateTime.now()) : alert();
        return this;
    }

    private String alert() {
        try {
            throw new PunishmentIssueException("Issue Date cannot be set more than once!", this);
        } catch (PunishmentIssueException pie) {
            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
            errorHandler.log(pie);
        }
        return this.issueDate;
    }

    public Punishment setPunisherUUID(String punisherUUID) {
        this.punisherUUID = punisherUUID;
        return this;
    }

    public Punishment setExpiration(long expiration) {
        this.expiration = expiration;
        return this;
    }

    public Punishment setMessage(String message) {
        this.message = message;
        return this;
    }

    public Punishment setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Punishment setRemoverUUID(String removerUUID) {
        this.removerUUID = removerUUID;
        return this;
    }

    public String getIssueDate() {
        return issueDate == null ? "N/A" : issueDate;
    }

    public HoverEvent getHoverEvent() {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, getHoverText().create());
    }

    public ComponentBuilder getHoverText() {
        PunishmentManager punishmentManager = PunishmentManager.getINSTANCE();
        ComponentBuilder text = new ComponentBuilder(" on: " + targetName).color(ChatColor.RED)
                .append("\nId: ").color(ChatColor.RED).append("#" + id).color(ChatColor.WHITE)
                .append("\nPunished By: ").color(ChatColor.RED).append(NameFetcher.getName(punisherUUID)).color(ChatColor.WHITE)
                .append("\nDate: ").color(ChatColor.RED).append(getIssueDate()).color(ChatColor.WHITE)
                .append("\nReason: ").color(ChatColor.RED).append(reason.toString().replace("_", " ")).color(ChatColor.WHITE);

        if (isBan() || isMute()) {
            String timeLeftRaw = punishmentManager.getTimeLeftRaw(this);
            if (timeLeftRaw.equals(String.valueOf(0)))
                text.append("\nPunishment has Expired!").color(ChatColor.RED);
            else if (timeLeftRaw.equals(String.valueOf(-1)))
                text.append("\nPunishment is Permanent!").color(ChatColor.RED);
            else
                text.append("\nExpires in: ").color(ChatColor.RED).append(timeLeftRaw).color(ChatColor.WHITE);
        }
        if (hasMessage())
            text.append("\nMessage: ").color(ChatColor.RED).append(message).color(ChatColor.WHITE);
        if (!isActive() && removerUUID != null) {
            if (removerUUID.equals("CONSOLE"))
                text.append("\nRemoved by: ").color(ChatColor.RED).append("CONSOLE").color(ChatColor.WHITE);
            else
                text.append("\nRemoved by: ").color(ChatColor.RED).append(NameFetcher.getName(removerUUID)).color(ChatColor.WHITE);
        }
        return text;
    }

    @Override
    public String toString() {
        return "{ID = '" + id + "', Type = '" + type.toString() + "', Reason = '" + reason.toString() + "', issueDate = '" + issueDate + "', expiration = '" + expiration + "', targetUUID = '" + targetUUID +
                "', TargetName = '" + targetName + "', PunisherUUID = '" + punisherUUID + "', message = '" + message + "', Status = '" + status.toString() + "', removerUUID = '" + removerUUID + "'}";
    }
}