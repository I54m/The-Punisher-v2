package com.i54m.protocol.inventory.adapter;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.inventory.Inventory;
import com.i54m.protocol.inventory.InventoryModule;
import com.i54m.protocol.inventory.event.InventoryCloseEvent;
import com.i54m.protocol.inventory.packet.CloseWindow;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.DefinedPacket;

public class CloseWindowAdapter extends PacketAdapter<CloseWindow> {

    public CloseWindowAdapter(final Stream stream) {
        super(stream, CloseWindow.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<? extends DefinedPacket> event) {
        if (!(event.getPacket() instanceof CloseWindow)) return;
        if (event.getPlayer() == null)
            return;
        final int windowID =  ((CloseWindow) event.getPacket()).getWindowId();
        final Inventory inv = InventoryModule.getInventory(event.getPlayer().getUniqueId(), windowID);
        if (inv == null)
            return;
        InventoryModule.registerInventory(event.getPlayer().getUniqueId(), windowID, null);
        ProxyServer.getInstance().getPluginManager().callEvent(new InventoryCloseEvent(event.getPlayer(), inv, null, windowID));
    }
}
