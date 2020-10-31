package com.i54m.punisher.managers;

import com.i54m.punisher.exceptions.ManagerNotStartedException;
import com.i54m.punisher.exceptions.PunishmentsStorageException;
import com.i54m.punisher.objects.Punishment;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ReputationManager implements Manager {

    @Getter
    private static final ReputationManager INSTANCE = new ReputationManager();
    private ReputationManager() {}

    private static final PunishmentManager PUNISHMENT_MANAGER = PunishmentManager.getINSTANCE();
    private static final PlayerDataManager PLAYER_DATA_MANAGER = PlayerDataManager.getINSTANCE();
    private boolean locked = true;
    private double banAt = -10.0;
    private double startingRep = 5.0;

    @Override
    public void start() {
        if (!locked) {
            ERROR_HANDLER.log(new Exception("Reputation Manager Already started!"));
            return;
        }
        if (PLAYER_DATA_MANAGER.isStarted()) {
            banAt = PLUGIN.getConfig().getDouble("Reputation.Rep-Ban.Ban-At", -10);
            startingRep = PLUGIN.getConfig().getDouble("Reputation.Starting-Rep", 5.0);
            locked = false;
        } else ERROR_HANDLER.log(new ManagerNotStartedException(PLAYER_DATA_MANAGER.getClass(), getClass()));
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

    public void minusRep(UUID uuid, double amount) {
        Configuration config = PLAYER_DATA_MANAGER.getPlayerData(uuid, false);
        double currentRep = config.getDouble("Reputation", startingRep);
        currentRep = currentRep - amount;
        if (currentRep > 10.0)
            currentRep = 10.0;
        config.set("Reputation", currentRep);
        PLAYER_DATA_MANAGER.savePlayerData(uuid);
        if (PLUGIN.getConfig().getBoolean("Reputation.Rep-Ban.enabled") && currentRep <= banAt)
            repBan(uuid);
    }

    public void addRep(UUID uuid, double amount) {
        Configuration config = PLAYER_DATA_MANAGER.getPlayerData(uuid, false);
        double currentRep = config.getDouble("Reputation", startingRep);
        currentRep = currentRep + amount;
        if (currentRep > 10.0)
            currentRep = 10.0;
        config.set("Reputation", currentRep);
        PLAYER_DATA_MANAGER.savePlayerData(uuid);
        if (PLUGIN.getConfig().getBoolean("Reputation.Rep-Ban.enabled") && currentRep <= banAt)
            repBan(uuid);
    }

    public void setRep(UUID uuid, double amount) {
        if (amount > 10.0)
            amount = 10.0;
        PLAYER_DATA_MANAGER.getPlayerData(uuid, false).set("Reputation", amount);
        PLAYER_DATA_MANAGER.savePlayerData(uuid);
        if (PLUGIN.getConfig().getBoolean("Reputation.Rep-Ban.enabled") && amount <= banAt)
            repBan(uuid);

    }

    public String getRep(UUID uuid) {
        return String.valueOf(PLAYER_DATA_MANAGER.getPlayerData(uuid, false).getDouble("Reputation", startingRep));
    }

    private void repBan(UUID targetUUID) {
        PUNISHMENT_MANAGER.lock(targetUUID);
        PLUGIN.getProxy().getScheduler().schedule(PLUGIN, () -> {
            try {
                String targetName = NameFetcher.getName(targetUUID);
                Punishment ban = new Punishment(Punishment.Type.BAN,
                        "Rep-Ban",
                        (long) 3.154e+12 + System.currentTimeMillis(),
                        targetUUID,
                        targetName,
                        UUIDFetcher.getBLANK_UUID(),
                        "Overly Toxic (Rep dropped below " + banAt + ")");
                PUNISHMENT_MANAGER.issue(ban, null, false, true, false);
            } catch (Exception e) {
                ERROR_HANDLER.log(new PunishmentsStorageException("Issuing rep ban", NameFetcher.getName(targetUUID), ReputationManager.class.getName(), e));
            }
        }, 2, TimeUnit.SECONDS);
    }
}