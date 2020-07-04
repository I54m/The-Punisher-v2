package com.i54m.protocol.world.adapter;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.world.Location;
import com.i54m.protocol.world.WorldModule;
import com.i54m.protocol.world.packet.PlayerPosition;

public class PlayerPositionAdapter extends PacketAdapter<PlayerPosition> {

    public PlayerPositionAdapter() {
        super(Stream.UPSTREAM, PlayerPosition.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<PlayerPosition> event) {
        if(event.getPlayer() == null)
            return;
        final PlayerPosition packet = event.getPacket();
        final Location location = WorldModule.getLocation(event.getPlayer().getUniqueId());
        if(location == null) {
            WorldModule.setLocation(event.getPlayer().getUniqueId(), packet.getLocation());
            return;
        }
        location.setX(packet.getLocation().getX());
        location.setY(packet.getLocation().getY());
        location.setZ(packet.getLocation().getZ());
    }

}
