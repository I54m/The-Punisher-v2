package com.i54m.punisher.objects;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.exceptions.PunishmentIssueException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.storage.StorageManager;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import lombok.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
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
     * The Reason we are punishing the player for.
     */
    @Getter
    private final String reason;
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
     * The MetaData of the punishment. This contains useful information
     * that cannot be reliably calculated from other variables.
     */
    private MetaData metaData;

    /**
     * This is the constructor that all new punishments should use as it will generate a new id for it.
     * @param type The type of punishment that this is.
     * @param reason The reason for this punishment.
     * @param expiration The date in milliseconds that this punishment expires (not required for warns or kicks).
     * @param targetUUID The UUID of the person to be punished.
     * @param targetName The name (with capitalization) of the person to be punished.
     * @param punisherUUID The UUID of the person that issued the punishment.
     * @param message The message that the target will see as the reason.
     * @param metaData The MetaData of the punishment.
     */
    public Punishment(@NotNull Type type, @NotNull String reason, @Nullable Long expiration, @NotNull UUID targetUUID, @NotNull String targetName, @NotNull UUID punisherUUID, @Nullable String message, @NotNull MetaData metaData) {
        StorageManager storageManager = PunisherPlugin.getInstance().getStorageManager();
        this.id = storageManager.getNextID();
        this.type = type;
        this.reason = reason;
        this.expiration = expiration == null ? 0 : expiration;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.punisherUUID = punisherUUID;
        this.message = message;
        this.removerUUID = null;
        this.status = Status.Created;
        storageManager.NewPunishment(this);
        this.metaData = metaData;
    }

    /**
     * This constructor is used to reconstruct punishments when we want to put them into the cache.
     * @param id The Id of the punishment, this is unique to each punishment.
     * @param type The type of punishment that this is.
     * @param reason The reason for this punishment.
     * @param issueDate The date and time the punishement was issued.
     * @param expiration The date in milliseconds that this punishment expires (not required for warns or kicks).
     * @param targetUUID The UUID of the person to be punished.
     * @param targetName The name (with capitalization) of the person to be punished.
     * @param punisherUUID The UUID of the person that issued the punishment.
     * @param message The message that the target will see as the reason.
     * @param status The current status of the punishment eg: removed, issued, pending, etc.
     * @param removerUUID The UUID of the person that removed the punishment, If the punishment has not yet been removed then this is null.
     * @param metaData The MetaData of the punishment.
     */
    public Punishment(@NotNull Integer id, @NotNull Type type, @NotNull String reason, @Nullable String issueDate, @Nullable Long expiration, @NotNull UUID targetUUID, @Nullable String targetName, @NotNull UUID punisherUUID, @Nullable String message, @NotNull Status status, @Nullable UUID removerUUID, @NotNull MetaData metaData) {
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
        this.metaData = metaData;
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
     * A punishment is considered permanent if it is 10 or more years.
     * @return true if the punishment is 10 or more years long, false if it is less than 10 years.
     */
    public boolean isPermanent() {
        if (isExpired() || isOverridden() || isIssued() || isRemoved()) return false;
        return (int) ((expiration - System.currentTimeMillis()) / 3.154e+10) >= 10;
    }

    /**
     * @return true if the punishment was issued manually else false.
     * This is just the inverse of {@link Punishment#isAutomatic()}.
     */
    public boolean isManual() {
        return !getMetaData().automaticCalculation;
    }

    /**
     * @return true if the punishment was issued using automatic calculation else false.
     * This is just the inverse of {@link Punishment#isManual()}.
     */
    public boolean isAutomatic() {
        return getMetaData().automaticCalculation;
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
        if (hasMetaData()) return getMetaData().reputationBan;
        else return false;
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
     * @return true if the punishment contains MetaData else false.
     */
    public boolean hasMetaData(){return this.metaData != null;}

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
            punisherUUID = UUIDFetcher.getBLANK_UUID();

        //expiration check
        if (isWarn() || isKick() || isTest())
            expiration = 0;
        else if ((isBan() || isMute()) && !hasExpiration())
            expiration = (long) 3.154e+12 + System.currentTimeMillis();

        //message check
        if (!hasMessage())
            message = "No Reason Given";
    }

    /**
     * This will set the issue date of the punishment to the current date and time in the local timezone.
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
     * @param removerUUID The UUID of the player that removed the punishment
     * @return The punishment itself with the updated info so that it can continue to be used.
     */
    public Punishment setRemoverUUID(UUID removerUUID) {
        this.removerUUID = removerUUID;
        return this;
    }

    /**
     * @param metaData The MetaData to set on the punishment
     * @return The punishment itself with the updated info so that it can continue to be used.
     */
    public Punishment setMetaData(MetaData metaData) {
        this.metaData = metaData;
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
        BaseComponent[] type;
        if (isBan())
            type = new ComponentBuilder("Ban").color(ChatColor.RED).create();
        else if (isMute())
            type = new ComponentBuilder("Mute").color(ChatColor.GOLD).create();
        else if (isKick())
            type = new ComponentBuilder("Kick").color(ChatColor.YELLOW).create();
        else if (isWarn())
            type = new ComponentBuilder("Warn").color(ChatColor.YELLOW).create();
        else type = new ComponentBuilder(getType().toString()).create();
        ComponentBuilder text = new ComponentBuilder("").append(type).append(" on: " + targetName).color(ChatColor.RED)
                .append("\nId: ").color(ChatColor.RED).append("#" + id).color(ChatColor.WHITE)
                .append("\nPunished By: ").color(ChatColor.RED).append(NameFetcher.getName(punisherUUID)).color(ChatColor.WHITE)
                .append("\nDate: ").color(ChatColor.RED).append(getIssueDate()).color(ChatColor.WHITE)
                .append("\nReason: ").color(ChatColor.RED).append(reason).color(ChatColor.WHITE);

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
            text.append("\nRemoved by: ").color(ChatColor.RED).append(NameFetcher.getName(removerUUID)).color(ChatColor.WHITE);
        }
        return text;
    }

    /**
     * @return the MetaData of the punishment. If none exists it will create some with the default options as a safeguard.
     */
    public MetaData getMetaData() {
        if (!hasMetaData())
            setMetaData(new MetaData());
        return metaData;
    }

    @AllArgsConstructor
    public static class MetaData {
        Boolean reputationBan;
        Boolean automaticCalculation;
        Boolean locked;
        Boolean appliesToHistory;

        public MetaData() {
            //creates a new set of metadata with the default settings
            reputationBan = false;
            automaticCalculation = false;
            locked = false;
            appliesToHistory = true;
        }

        public MetaData setLocked(Boolean locked) {
            this.locked = locked;
            return this;
        }

        public MetaData setReputationBan(Boolean reputationBan) {
            this.reputationBan = reputationBan;
            return this;
        }

        public MetaData setAutomaticCalculation(Boolean automaticCalculation) {
            this.automaticCalculation = automaticCalculation;
            return this;
        }

        public MetaData setAppliesToHistory(Boolean appliesToHistory) {
            this.appliesToHistory = appliesToHistory;
            return this;
        }

        public Boolean isReputationBan() {
            return reputationBan;
        }

        public Boolean isAutomaticCalculation() {
            return automaticCalculation;
        }

        public Boolean appliesToHistory() {
            return appliesToHistory;
        }

        public Boolean isLocked() {
            return locked;
        }
    }
}