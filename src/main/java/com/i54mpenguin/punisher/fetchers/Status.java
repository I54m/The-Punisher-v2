package com.i54mpenguin.punisher.fetchers;

import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.objects.Punishment;
import com.i54mpenguin.punisher.utils.NameFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.concurrent.Callable;

public class Status implements Callable<BaseComponent[]> {

    private String targetuuid;
    private final PunishmentManager punishMnger = PunishmentManager.getINSTANCE();

    public void setTargetuuid(String targetuuid) {
        this.targetuuid = targetuuid;
    }

    @Override
    public BaseComponent[] call() {
        ComponentBuilder status = new ComponentBuilder("Current Status: ").color(ChatColor.GREEN);
        if (punishMnger.hasActivePunishment(targetuuid)) {
            if (punishMnger.isMuted(targetuuid)) {
                Punishment punishment = punishMnger.getMute(targetuuid);
                String timeLeft = punishMnger.getTimeLeft(punishment);
                String reasonMessage = punishment.getMessage() == null ? punishment.getReason().toString().replace("_", " ") : punishment.getMessage();
                String punisher = punishment.getPunisherUUID().equals("CONSOLE") ? punishment.getPunisherUUID() : NameFetcher.getName(punishment.getPunisherUUID());
                status.append("Muted for " + timeLeft + ". Reason: " + reasonMessage + " by: " + punisher).color(ChatColor.YELLOW).event(punishment.getHoverEvent());
            }
            if (punishMnger.isBanned(targetuuid)) {
                if (punishMnger.isMuted(targetuuid))
                    status.append(" & ").color(ChatColor.WHITE);
                Punishment punishment = punishMnger.getBan(targetuuid);
                String timeLeft = punishMnger.getTimeLeft(punishment);
                String reasonMessage = punishment.getMessage() == null ? punishment.getReason().toString().replace("_", " ") : punishment.getMessage();
                String punisher = punishment.getPunisherUUID().equals("CONSOLE") ? punishment.getPunisherUUID() : NameFetcher.getName(punishment.getPunisherUUID());
                status.append("Banned for " + timeLeft + ". Reason: " + reasonMessage + " by: " + punisher).color(ChatColor.RED).event(punishment.getHoverEvent());
            }
        } else status.append("No currently active punishments!").color(ChatColor.GREEN);
        return status.create();
    }
}
