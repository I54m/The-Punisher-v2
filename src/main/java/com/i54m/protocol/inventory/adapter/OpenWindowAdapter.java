package com.i54m.protocol.inventory.adapter;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.inventory.Inventory;
import com.i54m.protocol.inventory.InventoryModule;
import com.i54m.protocol.inventory.event.InventoryOpenEvent;
import com.i54m.protocol.inventory.packet.OpenWindow;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class OpenWindowAdapter extends PacketAdapter<OpenWindow> {

    public OpenWindowAdapter() {
        super(Stream.DOWNSTREAM, OpenWindow.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<OpenWindow> event) {
        final ProxiedPlayer p = event.getPlayer();
        final OpenWindow packet = event.getPacket();

        Inventory inventory = new Inventory(packet.getInventoryType(), packet.getTitle());
        final Inventory original = inventory;
        inventory.setHomebrew(false);
        int windowId = packet.getWindowId();

        final InventoryOpenEvent openEvent = new InventoryOpenEvent(p, inventory, windowId);
        ProxyServer.getInstance().getPluginManager().callEvent(openEvent);
        if(openEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        inventory = openEvent.getInventory();
        windowId = openEvent.getWindowId();
        if(!inventory.equals(original) || packet.getWindowId() != windowId) {
            event.markForRewrite();
            packet.setWindowId(windowId);
            packet.setInventoryType(inventory.getType());
            packet.setTitle(inventory.getTitle());
        }

        InventoryModule.registerInventory(p.getUniqueId(), windowId, inventory);
    }

}
