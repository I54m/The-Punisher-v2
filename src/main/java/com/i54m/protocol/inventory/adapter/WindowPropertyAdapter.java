package com.i54m.protocol.inventory.adapter;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.api.util.ReflectionUtil;
import com.i54m.protocol.inventory.Inventory;
import com.i54m.protocol.inventory.InventoryModule;
import com.i54m.protocol.inventory.util.InventoryUtil;
import com.i54m.protocol.items.InventoryManager;
import com.i54m.protocol.items.ItemStack;
import com.i54m.protocol.items.ItemType;
import com.i54m.protocol.items.PlayerInventory;
import com.i54m.protocol.items.packet.WindowItems;
import net.md_5.bungee.api.ProxyServer;

public class WindowPropertyAdapter extends PacketAdapter<WindowItems> {

    public WindowPropertyAdapter() {
        super(Stream.DOWNSTREAM, WindowItems.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<WindowItems> event) {
        final WindowItems packet = event.getPacket();
        if(packet.getWindowId() == 0)
            return;
        final Inventory inventory = InventoryModule.getInventory(event.getPlayer().getUniqueId(), packet.getWindowId());
        if(inventory == null) {
            if(InventoryModule.isSpigotInventoryTracking())
                ProxyServer.getInstance().getLogger().warning("[Protocol] Try to set items in unknown inventory! player = "+event.getPlayer().getName()+" packet = "+packet);
            return;
        }
        final PlayerInventory playerInventory = InventoryManager.getCombinedSendInventory(event.getPlayer().getUniqueId(), event.getServerInfo().getName());
        int protocolVersion;
        try {
            protocolVersion = ReflectionUtil.getProtocolVersion(event.getPlayer());
        } catch (final Exception e) {
            protocolVersion = 47;
        }
        for (int i = 0; i < packet.getItems().size(); i++) {
            if(InventoryUtil.isLowerInventory(i, inventory, protocolVersion)) {
                // LOWER PLAYER INVENTORY
                final int playerSlot = i - inventory.getType().getTypicalSize(protocolVersion) + 9;
                final ItemStack stack = packet.getItemStackAtSlot(i);
                final ItemStack item = playerInventory.getItem(playerSlot);
                if(stack == null || stack.getType() == ItemType.NO_DATA) {
                    if(item != null && !item.isHomebrew() && InventoryModule.isSpigotInventoryTracking()) {
                        playerInventory.removeItem(playerSlot);
                    }
                } else if(InventoryModule.isSpigotInventoryTracking()) {
                    playerInventory.setItem(playerSlot, stack);
                }
                if(InventoryModule.isSpigotInventoryTracking()) {
                    if(packet.setItemStackAtSlot(i, item)) {
                        event.markForRewrite();
                    }
                } else {
                    if(item != null && item.isHomebrew()) {
                        if(packet.setItemStackAtSlot(i, item)) {
                            event.markForRewrite();
                        }
                    }
                }
            } else {
                // CONTAINER
                final ItemStack stack = packet.getItemStackAtSlot(i);
                if(stack == null || stack.getType() == ItemType.NO_DATA) {
                    if(inventory.getItem(i) != null && !inventory.getItem(i).isHomebrew() && InventoryModule.isSpigotInventoryTracking()) {
                        inventory.removeItem(i);
                    }
                } else if(InventoryModule.isSpigotInventoryTracking()) {
                    inventory.setItem(i, stack);
                }
                if(InventoryModule.isSpigotInventoryTracking()) {
                    if(packet.setItemStackAtSlot(i, inventory.getItem(i))) {
                        event.markForRewrite();
                    }
                } else {
                    final ItemStack toWrite = inventory.getItem(i);
                    if(toWrite != null && toWrite.isHomebrew()) {
                        if(packet.setItemStackAtSlot(i, toWrite)) {
                            event.markForRewrite();
                        }
                    }
                }
            }
        }
    }

}
