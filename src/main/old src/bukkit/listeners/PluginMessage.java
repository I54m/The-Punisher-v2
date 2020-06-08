package com.i54mpenguin.punisher.bukkit.listeners;

import com.i54mpenguin.punisher.bukkit.PunisherBukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class PluginMessage implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals("punisher:minor")) {
            try {
                ByteArrayInputStream inBytes = new ByteArrayInputStream(message);
                DataInputStream in = new DataInputStream(inBytes);
                String action = in.readUTF();
                if (action.equals("playsound")) {
                    String[] soundstrings = in.readUTF().split(":");
                    Sound sound;
                    try {
                        sound = Sound.valueOf(soundstrings[0]);
                    }catch (Exception e){
                        //sound doesn't exist on this version of mc
                        return;
                    }
                    int volume = Integer.parseInt(soundstrings[1]);
                    float pitch = Float.parseFloat(soundstrings[2]);
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } else if (action.equals("repcache")) {
                    double rep = Double.parseDouble(in.readUTF());
                    String uuid = in.readUTF();
                    PunisherBukkit.repCache.put(uuid, rep);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
