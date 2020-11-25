package com.i54m.protocol.world.adapter;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.world.Location;
import com.i54m.protocol.world.WorldModule;
import com.i54m.protocol.world.packet.PlayerPosition;
import net.md_5.bungee.protocol.DefinedPacket;

public class PlayerPositionAdapter extends PacketAdapter<PlayerPosition> {

    public PlayerPositionAdapter() {
        super(Stream.UPSTREAM, PlayerPosition.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<? extends DefinedPacket> event) {
        if (!(event.getPacket() instanceof PlayerPosition)) return;
        if (event.getPlayer() == null)
            return;
        final PlayerPosition packet = (PlayerPosition) event.getPacket();
        final Location location = WorldModule.getLocation(event.getPlayer().getUniqueId());
        if (location == null) {
            WorldModule.setLocation(event.getPlayer().getUniqueId(), packet.getLocation());
            return;
        }
        location.setX(packet.getLocation().getX());
        location.setY(packet.getLocation().getY());
        location.setZ(packet.getLocation().getZ());
    }

}
