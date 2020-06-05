package com.i54mpenguin.protocol.items.packet;

import com.google.common.collect.Maps;
import com.i54mpenguin.protocol.api.protocol.AbstractPacket;
import com.i54mpenguin.protocol.items.ItemStack;
import com.i54mpenguin.protocol.items.ItemType;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.ProtocolConstants.Direction;

import java.util.*;

import static com.i54mpenguin.protocol.api.util.ProtocolVersions.*;

public class WindowItems extends AbstractPacket {

    public static final HashMap<Integer, Integer> MAPPING = Maps.newHashMap();

    private short windowId;
    private Map<Integer, ItemStack> items = new HashMap<>();

    static {
        MAPPING.put(MINECRAFT_1_8, 0x30);
        MAPPING.put(MINECRAFT_1_9, 0x14);
        MAPPING.put(MINECRAFT_1_9_1, 0x14);
        MAPPING.put(MINECRAFT_1_9_2, 0x14);
        MAPPING.put(MINECRAFT_1_9_3, 0x14);
        MAPPING.put(MINECRAFT_1_10, 0x14);
        MAPPING.put(MINECRAFT_1_11, 0x14);
        MAPPING.put(MINECRAFT_1_11_1, 0x14);
        MAPPING.put(MINECRAFT_1_12, 0x14);
        MAPPING.put(MINECRAFT_1_12_1, 0x14);
        MAPPING.put(MINECRAFT_1_12_2, 0x14);
        MAPPING.put(MINECRAFT_1_13, 0x15);
        MAPPING.put(MINECRAFT_1_13_1, 0x15);
        MAPPING.put(MINECRAFT_1_13_2, 0x15);
        MAPPING.put(MINECRAFT_1_14, 0x14);
        MAPPING.put(MINECRAFT_1_14_1, 0x14);
        MAPPING.put(MINECRAFT_1_14_2, 0x14);
        MAPPING.put(MINECRAFT_1_14_3, 0x14);
        MAPPING.put(MINECRAFT_1_14_4, 0x14);
        MAPPING.put(MINECRAFT_1_15, 0x15);
        MAPPING.put(MINECRAFT_1_15_1, 0x15);
        MAPPING.put(MINECRAFT_1_15_2, 0x15);
    }

    public WindowItems(final short windowId, final Map<Integer, ItemStack> items) {
        this.windowId = windowId;
        this.items = items;
    }

    public WindowItems() {
    }

    @Override
    public void write(final ByteBuf buf, final Direction direction, final int protocolVersion) {
        buf.writeByte(windowId & 0xFF);
        buf.writeShort(items.size());
        for(ItemStack item : items.values()) {
            if(item == null)
                item = ItemStack.NO_DATA;
            item.write(buf, protocolVersion);
        }
    }

    @Override
    public void read(final ByteBuf buf, final Direction direction, final int protocolVersion) {
        windowId = buf.readUnsignedByte();
        final short count = buf.readShort();
        for (int i = 0; i < count; i++) {
            final ItemStack read = ItemStack.read(buf, protocolVersion);
            items.put(i, read);
        }
    }

    public Map<Integer, ItemStack> getItems() {
        return items;
    }

    public ItemStack getItemStackAtSlot(final int slot) {
        return items.get(slot);
    }

    public boolean setItemStackAtSlot(final int slot, final ItemStack stack) {
        final ItemStack curr = items.get(slot);
        if((curr == null || curr.getType() == ItemType.NO_DATA) && (stack == null || stack.getType() == ItemType.NO_DATA))
            return false;
        if(curr == null)
            return false;
        if(curr.equals(stack))
            return false;
        items.put(slot, stack);
        return true;
    }

    public short getWindowId() {
        return windowId;
    }

    public void setItems(final Map<Integer, ItemStack> items) {
        this.items = items;
    }

    public void setWindowId(final short windowId) {
        this.windowId = windowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WindowItems that = (WindowItems) o;
        return windowId == that.windowId &&
                Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowId, items);
    }

    @Override
    public String toString() {
        return "WindowItems{" +
                "windowId=" + windowId +
                ", items=" + items +
                '}';
    }
}
