package com.i54m.protocol.items.adapter;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.items.*;
import com.i54m.protocol.items.packet.SetSlot;
import net.md_5.bungee.protocol.DefinedPacket;

public class SetSlotItemsAdapter extends PacketAdapter<SetSlot> {

    public SetSlotItemsAdapter() {
        super(Stream.DOWNSTREAM, SetSlot.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<? extends DefinedPacket> event) {
        if (!(event.getPacket() instanceof SetSlot)) return;
        final SetSlot packet = (SetSlot) event.getPacket();
        if (packet.getWindowId() != 0)
            return;
        final PlayerInventory playerInventory = InventoryManager.getCombinedSendInventory(event.getPlayer().getUniqueId(), event.getServerInfo().getName());
        final ItemStack stack = packet.getItemStack();
        if (stack == null || stack.getType() == ItemType.NO_DATA) {
            if (playerInventory.getItem(packet.getSlot()) != null && !playerInventory.getItem(packet.getSlot()).isHomebrew() && ItemsModule.isSpigotInventoryTracking())
                playerInventory.removeItem(packet.getSlot());
        } else if (ItemsModule.isSpigotInventoryTracking()) {
            playerInventory.setItem(packet.getSlot(), stack);
        }
        if (ItemsModule.isSpigotInventoryTracking()) {
            packet.setItemStack(playerInventory.getItem(packet.getSlot()));
            event.markForRewrite();
        } else {
            final ItemStack toWrite = playerInventory.getItem(packet.getSlot());
            if (toWrite != null && toWrite.isHomebrew()) {
                packet.setItemStack(toWrite);
                event.markForRewrite();
            }
        }
    }

}
