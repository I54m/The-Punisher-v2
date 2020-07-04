package com.i54m.protocol.world;

import com.i54m.protocol.api.protocol.ProtocolAPI;
import com.i54m.protocol.world.adapter.PlayerPositionAdapter;
import com.i54m.protocol.world.packet.NamedSoundEffect;
import com.i54m.protocol.world.packet.PlayerPosition;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldModule {

    private static final Map<UUID, Location> UUID_LOCATION_MAP = new ConcurrentHashMap<>();

    public static void initModule() {
        // TO_CLIENT
        ProtocolAPI.getPacketRegistration().registerPacket(Protocol.GAME, ProtocolConstants.Direction.TO_CLIENT, NamedSoundEffect.class, NamedSoundEffect.MAPPING);

        // TO_SERVER
        ProtocolAPI.getPacketRegistration().registerPacket(Protocol.GAME, ProtocolConstants.Direction.TO_SERVER, PlayerPosition.class, PlayerPosition.MAPPING);

        // Adapters
        ProtocolAPI.getEventManager().registerListener(new PlayerPositionAdapter());
    }

    public static void playSound(final ProxiedPlayer proxiedPlayer, final Sound sound, final SoundCategory category, final float volume, final float pitch) {
        final NamedSoundEffect soundEffect = new NamedSoundEffect();
        soundEffect.setCategory(category);
        soundEffect.setPitch(pitch);
        soundEffect.setVolume(volume);
        soundEffect.setSound(sound);
        final Location location = getLocation(proxiedPlayer.getUniqueId());
        soundEffect.setX(location.getX());
        soundEffect.setY(location.getY());
        soundEffect.setZ(location.getZ());
        proxiedPlayer.unsafe().sendPacket(soundEffect);
    }

    public static Location getLocation(final UUID uuid) {
        return UUID_LOCATION_MAP.get(uuid);
    }

    public static void setLocation(final UUID uuid, final Location location) {
        UUID_LOCATION_MAP.put(uuid, location);
    }

    public static void uncache(final UUID uuid) {
        UUID_LOCATION_MAP.remove(uuid);
    }

}
