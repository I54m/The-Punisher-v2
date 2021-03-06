package com.i54m.protocol.inventory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.i54m.protocol.items.InventoryAction;
import com.i54m.protocol.items.ItemStack;
import com.i54m.protocol.items.ItemType;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class Inventory {

    private final Map<Integer, ItemStack> items = Maps.newHashMap();
    private InventoryType type;
    private BaseComponent[] title;
    private boolean homebrew = true;

    public Inventory(final InventoryType type, final BaseComponent... title) {
        this.type = type;
        this.title = title;
    }

    public InventoryType getType() {
        return type;
    }

    public void setType(final InventoryType type) {
        this.type = type;
    }

    public BaseComponent[] getTitle() {
        return title;
    }

    public void setTitle(final BaseComponent... title) {
        this.title = title;
    }

    public Map<Integer, ItemStack> getItems() {
        return items;
    }

    public void setItems(final Map<Integer, ItemStack> itemsIndexed) {
        for (final Integer slot : itemsIndexed.keySet()) {
            final ItemStack stack = itemsIndexed.get(slot);
            if (stack != null && stack.getType() != ItemType.NO_DATA)
                items.put(slot, stack);
        }
    }

    public Map<Integer, ItemStack> getItemsIndexed(final int protocolVersion) {
        return items;
    }

    public boolean setItem(final int slot, final ItemStack stack) {
        if (getItem(slot) != null) {
            if (getItem(slot).isHomebrew() && !stack.isHomebrew()) {
                return false;
            }
        }
        items.put(slot, stack);
        return true;
    }

    public boolean removeItem(final int slot) {
        items.remove(slot);
        return true;
    }

    public ItemStack getItem(final int slot) {
        return items.get(slot);
    }

    public boolean isHomebrew() {
        return homebrew;
    }

    public void setHomebrew(final boolean homebrew) {
        this.homebrew = homebrew;
    }

    public void apply(final InventoryAction action) {
        Preconditions.checkNotNull(action, "The action cannot be null!");
        for (final int slot : action.getChanges().keySet()) {
            final ItemStack stack = action.getChanges().get(slot);
            if (stack == null) {
                removeItem(slot);
            } else {
                setItem(slot, stack);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Inventory inventory = (Inventory) o;
        return Objects.equals(items, inventory.items) &&
                type == inventory.type &&
                Arrays.equals(title, inventory.title);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(items, type);
        result = 31 * result + Arrays.hashCode(title);
        return result;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "items=" + items +
                ", type=" + type +
                ", title=" + Arrays.toString(title) +
                '}';
    }
}
