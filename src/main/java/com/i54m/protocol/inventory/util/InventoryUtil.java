package com.i54m.protocol.inventory.util;

import com.i54m.protocol.inventory.Inventory;
import com.i54m.protocol.inventory.InventoryType;

public final class InventoryUtil {

    private InventoryUtil() {}

    public static boolean isLowerInventory(final int i, final Inventory inventory, final int protocolVersion) {
        final InventoryType type = inventory.getType();
        if(type == null)
            return false;
        return i >= type.getTypicalSize(protocolVersion);
    }

}
