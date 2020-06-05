package com.i54mpenguin.protocol.items.adapter;

import com.i54mpenguin.protocol.api.event.PacketReceiveEvent;
import com.i54mpenguin.protocol.api.handler.PacketAdapter;
import com.i54mpenguin.protocol.api.protocol.Stream;
import com.i54mpenguin.protocol.items.InventoryManager;
import com.i54mpenguin.protocol.items.PlayerInventory;
import com.i54mpenguin.protocol.items.packet.HeldItemChange;
@Deprecated
public class HeldItemChangeAdapter extends PacketAdapter<HeldItemChange> {

    public HeldItemChangeAdapter(final Stream stream) {
        super(stream, HeldItemChange.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<HeldItemChange> event) {
        if(event.getServerInfo() == null)
            return;
        final PlayerInventory inventory = InventoryManager.getInventory(event.getPlayer().getUniqueId(), event.getServerInfo().getName());
        inventory.setHeldItem(event.getPacket().getNewSlot());
    }
}
