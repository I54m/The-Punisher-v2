package com.i54mpenguin.punisher.objects;

import com.i54mpenguin.punisher.exceptions.PunishmentIssueException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.utils.NameFetcher;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ToString
public class Punishment {

    /**
     * The Id of the punishment, this is unique to each punishment.
     */
    @Getter
    private final int id;
    /**
     * The type of punishment that this is eg: ban, mute, kick or warn.
     */
    @Getter
    private final Type type;
    /**
     *
     */// TODO: 9/06/2020 to be removed in favor of custom reasons
    @Deprecated
    @Getter
    private final Reason reason;
    /**
     * The date the punishment was originally issued by the punisher.
     */
    private String issueDate;
    /**
     * The date the punishment expires stored in milliseconds.
     */
    @Getter
    private long expiration;
    /**
     * The UUID of the person to be punished.
     */
    @Getter
    private final UUID targetUUID;
    /**
     * The name (with capitalization) of the person to be punished.
     */
    @Getter
    private String targetName;
    /**
     * The UUID of the person that issued the punishment.
     */
    @Getter
    private UUID punisherUUID;
    /**
     * The message that the target will see as the reason.
     */
    @Getter
    private String message;
    /**
     * The current status of the punishment eg: removed, issued, pending, etc.
     */
    @Getter
    private Status status;
    /**
     * The UUID of the person that removed the punishment,
     * If the punishment has not yet been removed then this is null.
     */
    @Getter
    private UUID removerUUID;

    /**
     * This is the constructor that all new punishments should use as it will generate a new id for it.
     * @param type The type of punishment that this is.
     * @param reason The reason for this punishment.
     * @param expiration The date in milliseconds that this punishment expires (not required for warns or kicks).
     * @param targetUUID The UUID of the person to be punished.
     * @param targetName The name (with capitalization) of the person to be punished.
     * @param punisherUUID The UUID of the person that issued the punishment.
     * @param message The message that the target will see as the reason.
     */
    public Punishment(@NotNull Type type, @NotNull Reason reason, @Nullable Long expiration, UUID targetUUID, @NotNull String targetName, UUID punisherUUID, @Nullable String message) {
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

    /**
     * This constructor is used to reconstruct punishments when we want to put them into the cache.
     * @param id The Id of the punishment, this is unique to each punishment.
     * @param type The type of punishment that this is.
     * @param reason The reason for this punishment.
     * @param expiration The date in milliseconds that this punishment expires (not required for warns or kicks).
     * @param targetUUID The UUID of the person to be punished.
     * @param targetName The name (with capitalization) of the person to be punished.
     * @param punisherUUID The UUID of the person that issued the punishment.
     * @param message The message that the target will see as the reason.
     * @param status The current status of the punishment eg: removed, issued, pending, etc.
     * @param removerUUID The UUID of the person that removed the punishment, If the punishment has not yet been removed then this is null.
     */
    public Punishment(@NotNull Integer id, @NotNull Type type, @NotNull Reason reason, @Nullable String issueDate, @Nullable Long expiration, UUID targetUUID, @Nullable String targetName, UUID punisherUUID, @Nullable String message, @NotNull Status status, UUID removerUUID) {
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

    /**
     * This dictates what the punishment will do eg: will it ban or mute the target.
     */
    public enum Type {
        BAN, KICK, MUTE, WARN, ALL
    }

    /**
     * This is used to help show what has happened to the punishment eg: has it been issued or is it still pending.
     */
    public enum Status {
        Created, Pending, Active, Issued, Overridden, Expired, Removed
    }

    /**
     * The reason we are issuing the punishment for.
     */
    @Deprecated
    public enum Reason { // TODO: 8/06/2020 remove as this needs to be configurable
        Minor_Chat_Offence, Major_Chat_Offence, DDoS_DoX_Threats, Inappropriate_Link, Scamming, X_Raying, AutoClicker, Fly_Speed_Hacking, Disallowed_Mods, Malicious_PvP_Hacks, Server_Advertisement,
        Greifing, Exploiting, Tpa_Trapping, Impersonation, Other_Minor_Offence, Other_Major_Offence, Other_Offence, Custom
    }

    /**
     * A punishment is considered permanent if it is 10 or more years.
     * @return true if the punishment is 10 or more years long, false if it is less than 10 years.
     */
    public boolean isPermanent() {
        if (getStatus() != Status.Active) return false;
        long millis;
        millis = expiration - System.currentTimeMillis();
        int yearsleft = (int) (millis / 3.154e+10);
        return yearsleft >= 10;
    }

    /**
     * @return true if the punishment was issued manually else false.
     */
    public boolean isManual() {
        return reason.toString().contains("Other") || reason == Reason.Custom;
    }

    /**
     * @return true if the punishment is a ban else false.
     */
    public boolean isBan() {
        return this.type == Type.BAN;
    }

    /**
     * @return true if the punishment is a permanent ban that was made by console and contains the message "Overly Toxic", these are the main signs of a reputation ban.
     */
    public boolean isRepBan() {
        return isBan() && punisherUUID.equals("CONSOLE") && isCustom() && isPermanent() && message.contains("Overly Toxic");
    }

    /**
     * @return true if the punishment is a mute else false.
     */
    public boolean isMute() {
        return this.type == Type.MUTE;
    }

    /**
     * @return true if the punishment is a warn else false.
     */
    public boolean isWarn() {
        return this.type == Type.WARN;
    }

    /**
     * @return true if the punishment is a kick else false.
     */
    public boolean isKick() {
        return this.type == Type.KICK;
    }

    /**
     * If a punishment uses the type "ALL" its is considered a test
     * @return true if the punishment is considered a test else false.
     */
    public boolean isTest() {
        return this.type == Type.ALL;
    }

    /**
     * @return true if the reason is custom else false.
     */
    public boolean isCustom() {
        return this.reason == Reason.Custom;
    }

    /**
     * If the punishment is pending it means it has been issued by the punisher
     * but has not yet been issued on the target.
     * @return true if the status of the punishment is "Pending" else false.
     */
    public boolean isPending() {
        return this.status == Status.Pending;
    }

    /**
     * If the punishment is active it means it has been issued and is still
     * being enforced (this only applies to mutes and bans).
     * @return true if the status of the punishment is "Active" else false.
     */
    public boolean isActive() {
        return this.status == Status.Active;
    }

    /**
     * If the punishment is issued it means it has been issued to the target
     * and doesn't need anymore enforcement (this only applies to warns and kicks).
     * @return true if the status of the punishment is "Issued" else false.
     */
    public boolean isIssued() {
        return this.status == Status.Issued;
    }

    /**
     * If the punishment is overridden it means that someone with a higher
     * permission level than the previous punisher has issued another punishment
     * on the target resulting in the punishment issued by the punisher with the
     * lower permission level being disregarded in favor of the one done by
     * someone with higher permissions.
     * @return true if the status of the punishment is "Overridden" else false.
     */
    public boolean isOverridden() {
        return this.status == Status.Overridden;
    }

    /**
     * If the Punishment has expired it means that the expiration date has
     * been reached and the punishment is no longer being enforced (this only applies to mutes and bans).
     * @return true if the status of the punishment is "Expired" else false.
     */
    public boolean isExpired() {
        return this.status == Status.Expired;
    }

    /**
     * If the punishment has been removed it means that it has been removed
     * by someone and is no longer being enforced (this only applies to mutes and bans).
     * @return true if the status of the punishment is "Removed" else false.
     */
    public boolean isRemoved() {
        return this.status == Status.Removed;
    }

    /**
     * @return true if the punishment has an expiration date else false.
     */
    public boolean hasExpiration() {
        return this.expiration != 0;
    }

    /**
     * @return true if the punishment has a message else false.
     */
    public boolean hasMessage() {
        return !(this.message == null || this.message.isEmpty());
    }

    /**
     * This method verifies that the punishment has all the variables
     * that it requires in order to be properly issued, if a punishment
     * is missing something that it should have it will set it to a default.
     */
    public void verify() {
        String name = NameFetcher.getName(targetUUID);

        //targetname check
        if (targetName == null && name != null)
            targetName = name;

        //punisher uuid check
        if (punisherUUID == null)
            punisherUUID = UUID.fromString("0-0-0-0-0");

        //expiration check
        if (isWarn() || isKick() || isTest())
            expiration = 0;
        else if ((isBan() || isMute()) && !hasExpiration())
            expiration = (long) 3.154e+12 + System.currentTimeMillis();

        //message check
        if (isCustom() && !hasMessage())
            message = "No Reason Given";
    }

    /**
     * This will set the issue dat of the punishment to the current date & time in the local timezone.
     * This can only be set once and will throw an error and make no change if you try to set it again.
     * When you set it twice this method will throw a {@link PunishmentIssueException} telling you that
     * you cannot set the issue date more than once. To make sure that things still run smoothly it will
     * return the current issue date and not interrupt anything.
     * @return The punishment itself with the updated issue date so that it can continue to be used.
     */
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

    /**
     * @param punisherUUID The UUID of the punisher that is issuing the punishment.
     * @return The punishment itself with the updated info so that it can continue to be used.
     */
    public Punishment setPunisherUUID(UUID punisherUUID) {
        this.punisherUUID = punisherUUID;
        return this;
    }

    /**
     * @param expiration The {@link System#currentTimeMillis()} plus the amount of time in milliseconds that the punishment should be.
     * @return The punishment itself with the updated info so that it can continue to be used.
     * @see System#currentTimeMillis()
     */
    public Punishment setExpiration(long expiration) {
        this.expiration = expiration;
        return this;
    }

    /**
     * @param message The message that the target will see when they receive the punishment.
     * @return The punishment itself with the updated info so that it can continue to be used.
     */
    public Punishment setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    /**
     * @param status The status to set on the punishment.
     * @return The punishment itself with the updated info so that it can continue to be used.
     */
    public Punishment setStatus(@NotNull Status status) {
        this.status = status;
        return this;
    }

    /**
     * @param removerUUID the UUID of the player that removed the punishment
     * @return The punishment itself with the updated info so that it can continue to be used.
     */
    public Punishment setRemoverUUID(UUID removerUUID) {
        this.removerUUID = removerUUID;
        return this;
    }

    /**
     * @return The formatted date that the punishment was issued by the punisher returns "N/A" if not set.
     */
    public String getIssueDate() {return issueDate == null ? "N/A" : issueDate;}

    /**
     * @return A hover event that can be used in a {@link ComponentBuilder} for the bungeecord chat api.
     * @see ComponentBuilder
     * @see net.md_5.bungee.api.chat.BaseComponent
     * @see net.md_5.bungee.api.chat.TextComponent
     */
    public HoverEvent getHoverEvent() {return new HoverEvent(HoverEvent.Action.SHOW_TEXT, getHoverText().create());}

    /**
     * @return Information about the punishment formatted nicely that can be used in {@link #getHoverEvent()}.
     * @see ComponentBuilder
     * @see net.md_5.bungee.api.chat.BaseComponent
     * @see net.md_5.bungee.api.chat.TextComponent
     */
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
}