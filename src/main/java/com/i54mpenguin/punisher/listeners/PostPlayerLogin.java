package com.i54mpenguin.punisher.listeners;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.chats.StaffChat;
import com.i54mpenguin.punisher.exceptions.PunishmentIssueException;
import com.i54mpenguin.punisher.exceptions.PunishmentsDatabaseException;
import com.i54mpenguin.punisher.handlers.ErrorHandler;
import com.i54mpenguin.punisher.managers.storage.DatabaseManager;
import com.i54mpenguin.punisher.managers.PunishmentManager;
import com.i54mpenguin.punisher.managers.WorkerManager;
import com.i54mpenguin.punisher.objects.Punishment;
import com.i54mpenguin.punisher.utils.NameFetcher;
import com.i54mpenguin.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PostPlayerLogin implements Listener {
    private final PunishmentManager punishmngr = PunishmentManager.getINSTANCE();
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private final DatabaseManager dbManager = DatabaseManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();

    @EventHandler
    public void onPostLogin(PostLoginEvent event){
        final ProxiedPlayer player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String fetcheduuid = uuid.toString().replace("-", "");
        final String targetName = player.getName();
        if (punishmngr.hasPendingPunishment(fetcheduuid)){
            ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                try {
                    punishmngr.issue(punishmngr.getPendingPunishment(fetcheduuid), null, false, true, false);
                }catch (SQLException sqle){
                    try{
                        throw new PunishmentIssueException("SQL Exception Caused punishment reissue on login to fail", punishmngr.getPendingPunishment(fetcheduuid), sqle);
                    }catch (PunishmentIssueException pie){
                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                        errorHandler.log(pie);
                        errorHandler.adminChatAlert(pie, ProxyServer.getInstance().getPlayer(uuid));
                        errorHandler.alert(pie, ProxyServer.getInstance().getPlayer(uuid));
                    }
                }
            }, 10, TimeUnit.SECONDS);
        }

        workerManager.runWorker(new WorkerManager.Worker(() ->{
           //if stored name in namefetcher does not equal target name then recache the name
            if (!NameFetcher.hasNameStored(fetcheduuid) || !NameFetcher.getStoredName(fetcheduuid).equals(targetName))
                NameFetcher.storeName(fetcheduuid, targetName);
            UUIDFetcher.updateStoredUUID(targetName, fetcheduuid);
        }));

        try {
            if (punishmngr.isBanned(fetcheduuid)) {
                if (player.hasPermission("punisher.bypass")) {
                    punishmngr.remove(punishmngr.getBan(fetcheduuid), null, true, true, false);
                    PunisherPlugin.getLOGS().info(player.getName() + " Bypassed their ban and were unbanned");
                    plugin.getProxy().getScheduler().schedule(plugin, () ->
                                    StaffChat.sendMessage(new ComponentBuilder(targetName + " Bypassed their ban, Unbanning...").color(ChatColor.RED).event(punishmngr.getBan(fetcheduuid).getHoverEvent()).create(), true)
                            , 5, TimeUnit.SECONDS);
                } else {
                    Punishment ban = punishmngr.getBan(fetcheduuid);
                    if (System.currentTimeMillis() > ban.getExpiration()) {
                        punishmngr.remove(punishmngr.getBan(fetcheduuid), null, false, false, false);
                        PunisherPlugin.getLOGS().info(player.getName() + "'s ban expired so they were unbanned");
                    } else {
                        String timeleft = punishmngr.getTimeLeft(ban);
                        String reason = ban.getMessage();
                        if (ban.isPermanent()) {
                            String banMessage = plugin.getConfig().getString("PermBan Message").replace("%timeleft%", timeleft).replace("%reason%", reason);
                            player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                        } else {
                            String banMessage = plugin.getConfig().getString("TempBan Message").replace("%timeleft%", timeleft).replace("%reason%", reason);
                            player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', banMessage)));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            try {
                throw new PunishmentsDatabaseException("Revoking punishment", targetName, this.getClass().getName(), e);
            } catch (PunishmentsDatabaseException pde) {
                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                errorHandler.log(pde);
                errorHandler.loginError(event);
            }
        }


        workerManager.runWorker(new WorkerManager.Worker(() ->{
            try {
                //check for banned alts
                ArrayList<String> altslist = new ArrayList<>();
                String ip = player.getAddress().getHostString();
                String sqlip1 = "SELECT * FROM `altlist` WHERE ip='" + ip + "'";
                PreparedStatement stmtip1 = dbManager.connection.prepareStatement(sqlip1);
                ResultSet resultsip1 = stmtip1.executeQuery();
                while (resultsip1.next()) {
                    String concacc = resultsip1.getString("uuid");
                    altslist.add(concacc);
                }
                stmtip1.close();
                resultsip1.close();
                altslist.remove(uuid.toString().replace("-", ""));
                if (!altslist.isEmpty()) {
                    altslist.removeIf((String alt) -> !punishmngr.isBanned(alt));
                    StringBuilder bannedalts = new StringBuilder();
                    int i = 0;
                    for (String alts : altslist) {
                        bannedalts.append(NameFetcher.getName(alts));
                        if (!(i + 1 >= altslist.size()))
                            bannedalts.append(", ");
                        i++;
                    }
                    if (bannedalts.length() > 0)
                        plugin.getProxy().getScheduler().schedule(plugin, () -> StaffChat.sendMessage(targetName + " Might have banned alts: " + bannedalts.toString(), true), 5, TimeUnit.SECONDS);
                }
            } catch (SQLException sqle) {
                try {
                    throw new PunishmentsDatabaseException("Checking for banned alts", targetName, PostPlayerLogin.class.getName(), sqle);
                } catch (PunishmentsDatabaseException pde) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(pde);
                    errorHandler.loginError(event);
                }
            }
        }));
    }
}
