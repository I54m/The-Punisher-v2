package com.i54mpenguin.protocol.inventory.adapter;

import com.i54mpenguin.protocol.api.event.PacketReceiveEvent;
import com.i54mpenguin.protocol.api.handler.PacketAdapter;
import com.i54mpenguin.protocol.api.protocol.Stream;
import com.i54mpenguin.protocol.inventory.Inventory;
import com.i54mpenguin.protocol.inventory.InventoryModule;
import com.i54mpenguin.protocol.inventory.event.InventoryCloseEvent;
import com.i54mpenguin.protocol.inventory.packet.CloseWindow;
import net.md_5.bungee.api.ProxyServer;

public class CloseWindowAdapter extends PacketAdapter<CloseWindow> {

    public CloseWindowAdapter(final Stream stream) {
        super(stream, CloseWindow.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<CloseWindow> event) {
        if(event.getPlayer() == null)
            return;
        final Inventory inv = InventoryModule.getInventory(event.getPlayer().getUniqueId(), event.getPacket().getWindowId());
        if(inv == null)
            return;
        InventoryModule.registerInventory(event.getPlayer().getUniqueId(), event.getPacket().getWindowId(), null);
        ProxyServer.getInstance().getPluginManager().callEvent(new InventoryCloseEvent(event.getPlayer(), inv, null, event.getPacket().getWindowId()));
    }
}
