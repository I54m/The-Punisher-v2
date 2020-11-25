package com.i54m.protocol.items;

import com.i54m.protocol.api.protocol.ProtocolAPI;
import com.i54m.protocol.items.adapter.SetSlotItemsAdapter;
import com.i54m.protocol.items.adapter.WindowItemsAdapter;
import com.i54m.protocol.items.packet.SetSlot;
import com.i54m.protocol.items.packet.WindowItems;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;

public class ItemsModule {

    private static boolean spigotInventoryTracking = false;

    private ItemsModule() {
    }

    public static void initModule() {
        // TO_CLIENT
        ProtocolAPI.getPacketRegistration().registerPacket(Protocol.GAME, ProtocolConstants.Direction.TO_CLIENT, SetSlot.class, SetSlot.MAPPING);
        ProtocolAPI.getPacketRegistration().registerPacket(Protocol.GAME, ProtocolConstants.Direction.TO_CLIENT, WindowItems.class, WindowItems.MAPPING);

        // ADAPTERS
        ProtocolAPI.getEventManager().registerListener(new WindowItemsAdapter());
        ProtocolAPI.getEventManager().registerListener(new SetSlotItemsAdapter());
    }

    public static boolean isSpigotInventoryTracking() {
        return spigotInventoryTracking;
    }

    public static void setSpigotInventoryTracking(final boolean spigotInventoryTracking) {
        ItemsModule.spigotInventoryTracking = spigotInventoryTracking;
    }

}
