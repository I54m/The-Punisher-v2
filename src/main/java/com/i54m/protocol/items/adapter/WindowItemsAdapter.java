package com.i54m.protocol.items.adapter;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.items.*;
import com.i54m.protocol.items.packet.WindowItems;

public class WindowItemsAdapter extends PacketAdapter<WindowItems> {

    public WindowItemsAdapter() {
        super(Stream.DOWNSTREAM, WindowItems.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<WindowItems> event) {
        final WindowItems packet = event.getPacket();
        if(packet.getWindowId() != 0)
            return;
        final PlayerInventory playerInventory = InventoryManager.getCombinedSendInventory(event.getPlayer().getUniqueId(), event.getServerInfo().getName());
        for (int i = 0; i < packet.getItems().size(); i++) {
            final ItemStack stack = packet.getItemStackAtSlot(i);
            if(stack == null || stack.getType() == ItemType.NO_DATA) {
                if(playerInventory.getItem(i) != null && !playerInventory.getItem(i).isHomebrew() && ItemsModule.isSpigotInventoryTracking()) {
                    playerInventory.removeItem(i);
                }
            } else if(ItemsModule.isSpigotInventoryTracking()) {
                playerInventory.setItem(i, stack);
            }
            if(ItemsModule.isSpigotInventoryTracking()) {
                if(packet.setItemStackAtSlot(i, playerInventory.getItem(i))) {
                    event.markForRewrite();
                }
            } else {
                final ItemStack toWrite = playerInventory.getItem(i);
                if(toWrite != null && toWrite.isHomebrew()) {
                    if(packet.setItemStackAtSlot(i, toWrite)) {
                        event.markForRewrite();
                    }
                }
            }
        }
    }
}
