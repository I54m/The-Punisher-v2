package com.i54mpenguin.punisher.managers;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.objects.Punishment;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class ReputationManager {
    private static final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private static final PunishmentManager punishMnger = PunishmentManager.getINSTANCE();

    public static void minusRep(String targetname, String uuid, double amount) {
//        if (!PunisherPlugin.reputationConfig.contains(uuid)) {
//            PunisherPlugin.reputationConfig.set(uuid,  Double.valueOf(new DecimalFormat("##.##").format((5.0 - amount))));
//        } else {
//            double currentRep = PunisherPlugin.reputationConfig.getDouble(uuid);
//            if ((currentRep - amount) > -10 && (currentRep - amount) < 10) {
//                PunisherPlugin.reputationConfig.set(uuid,  Double.valueOf(new DecimalFormat("##.##").format((currentRep - amount))));
//            } else if ((currentRep - amount) > 10) {
//                PunisherPlugin.reputationConfig.set(uuid, 10.00);
//            } else if ((currentRep - amount) <= -10) {
//                PunisherPlugin.reputationConfig.set(uuid, Double.valueOf(new DecimalFormat("##.##").format((currentRep - amount))));
//                repBan(uuid, targetname);
//            }
//        }
//        PunisherPlugin.saveRep();
    }

    public static void addRep(String targetname, String uuid, double amount) {
//        if (!PunisherPlugin.reputationConfig.contains(uuid))
//            PunisherPlugin.reputationConfig.set(uuid, Double.valueOf(new DecimalFormat("##.##").format((5.0 + amount))));
//        else {
//            double currentRep = PunisherPlugin.reputationConfig.getDouble(uuid);
//            if ((currentRep + amount) > -10 && (currentRep + amount) < 10)
//                PunisherPlugin.reputationConfig.set(uuid, Double.valueOf(new DecimalFormat("##.##").format((currentRep + amount))));
//            else if ((currentRep + amount) > 10)
//                PunisherPlugin.reputationConfig.set(uuid, 10.00);
//            else if ((currentRep + amount) <= -10) {
//                PunisherPlugin.reputationConfig.set(uuid, Double.valueOf(new DecimalFormat("##.##").format((currentRep + amount))));
//                repBan(uuid, targetname);
//            }
//        }
//        PunisherPlugin.saveRep();
    }

    public static void setRep(String targetname, String uuid, double amount) {
//        if (amount > 10)
//            PunisherPlugin.reputationConfig.set(uuid, 10.00);
//        else if (!(amount > -10)) {
//            PunisherPlugin.reputationConfig.set(uuid, Double.valueOf(new DecimalFormat("##.##").format(amount)));
//            repBan(uuid, targetname);
//        }else
//            PunisherPlugin.reputationConfig.set(uuid, Double.valueOf(new DecimalFormat("##.##").format(amount)));
//        PunisherPlugin.saveRep();

    }

    public static String getRep(String uuid) {
//        if (PunisherPlugin.reputationConfig.contains(uuid))
//            return String.valueOf(PunisherPlugin.reputationConfig.getDouble(uuid));
//        else return null;
        return null;
    }

    private static void repBan(String targetUUID, String targetName){
        punishMnger.lock(targetUUID);
        plugin.getProxy().getScheduler().schedule(plugin, () ->{
            try {
                Punishment ban = new Punishment(Punishment.Type.BAN, Punishment.Reason.Custom, (long) 3.154e+12 + System.currentTimeMillis(), targetUUID, targetName, "CONSOLE", "Overly Toxic (Rep dropped below -10)");
                punishMnger.issue(ban, null, false, true, false);
            } catch (SQLException e) {
                try {
                    throw new PunishmentsDatabaseException("Issuing perm rep ban", targetName, ReputationManager.class.getName(), e);
                } catch (PunishmentsDatabaseException pde) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(pde);
                }
            }
        }, 2, TimeUnit.SECONDS);
    }

}