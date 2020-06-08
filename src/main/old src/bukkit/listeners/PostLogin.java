package com.i54mpenguin.punisher.bukkit.listeners;

import com.i54mpenguin.punisher.bukkit.PunisherBukkit;
import me.fiftyfour.punisher.universal.util.NameFetcher;
import me.fiftyfour.punisher.universal.util.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class PostLogin implements Listener {

    @EventHandler
    public void onPostLogin(PlayerLoginEvent event){
        Bukkit.getServer().getScheduler().runTaskAsynchronously(PunisherBukkit.getInstance(), () ->{
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            final String fetcheduuid = uuid.toString().replace("-", "");
            final String targetName = player.getName();
            //if stored name in namefetcher does not equal target name then recache the name
            if (!NameFetcher.hasNameStored(fetcheduuid) || !NameFetcher.getStoredName(fetcheduuid).equals(targetName))
                NameFetcher.storeName(fetcheduuid, targetName);
            UUIDFetcher.updateStoredUUID(targetName, fetcheduuid);
            PunisherBukkit.updateRepCache();
        });
    }
}
