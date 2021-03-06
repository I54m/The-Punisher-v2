package com.i54m.protocol.inventory.adapter;

import com.i54m.protocol.api.ClickType;
import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.handler.PacketAdapter;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.api.util.ReflectionUtil;
import com.i54m.protocol.inventory.Inventory;
import com.i54m.protocol.inventory.InventoryModule;
import com.i54m.protocol.inventory.InventoryType;
import com.i54m.protocol.inventory.event.InventoryClickEvent;
import com.i54m.protocol.inventory.packet.ClickWindow;
import com.i54m.protocol.inventory.packet.WindowConfirmation;
import com.i54m.protocol.items.*;
import com.i54m.protocol.items.packet.SetSlot;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.DefinedPacket;

import static com.i54m.protocol.api.util.ProtocolVersions.MINECRAFT_1_8;

public class ClickWindowAdapter extends PacketAdapter<ClickWindow> {

    public ClickWindowAdapter() {
        super(Stream.UPSTREAM, ClickWindow.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<? extends DefinedPacket> event) {
        if (!(event.getPacket() instanceof ClickWindow)) return;
        final ClickWindow clickWindow = (ClickWindow) event.getPacket();
        if (event.getPlayer() == null)
            return;
        final Inventory inventory = InventoryModule.getInventory(event.getPlayer().getUniqueId(), clickWindow.getWindowId());
        if (inventory == null)
            return;

        short slot = clickWindow.getSlot();
        if (inventory.getType() == InventoryType.BREWING_STAND && ReflectionUtil.getProtocolVersion(event.getPlayer()) == MINECRAFT_1_8) {
            // Insert missing slot 4 for 1.8 clients into slot calculation
            if (slot >= 4)
                slot++;
        }

        final InventoryClickEvent clickEvent = new InventoryClickEvent(event.getPlayer(), inventory, slot, clickWindow.getWindowId(), clickWindow.getClickType(), clickWindow.getActionNumber());
        ProxyServer.getInstance().getPluginManager().callEvent(clickEvent);

        if (clickEvent.isCancelled() || inventory.isHomebrew()) {
            event.setCancelled(true);
            if (InventoryModule.getInventory(event.getPlayer().getUniqueId(), clickWindow.getWindowId()) != null) {
                if (inventory.getType() != InventoryType.PLAYER)
                    InventoryModule.sendInventory(event.getPlayer(), inventory);
                else
                    InventoryManager.getInventory(event.getPlayer().getUniqueId()).update();
                if (clickEvent.getClickType().name().startsWith("NUMBER_BUTTON")) {
                    event.getPlayer().unsafe().sendPacket(new WindowConfirmation((byte) clickWindow.getWindowId(), (short) clickWindow.getActionNumber(), false));
                    event.getPlayer().unsafe().sendPacket(new SetSlot((byte) 0, (short) (clickEvent.getClickType().getButton() + 36), new ItemStack(ItemType.NO_DATA)));
                } else if (clickEvent.getClickType().name().startsWith("SHIFT_")) {
                    event.getPlayer().unsafe().sendPacket(new WindowConfirmation((byte) clickWindow.getWindowId(), (short) clickWindow.getActionNumber(), false));
                    event.getPlayer().unsafe().sendPacket(new SetSlot((byte) 0, (short) 44, new ItemStack(ItemType.NO_DATA)));
                } else {
                    event.getPlayer().unsafe().sendPacket(new SetSlot((byte) -1, (short) -1, new ItemStack(ItemType.NO_DATA)));
                }
            }
            return;
        }
        if (clickWindow.getActionNumber() != clickEvent.getActionNumber()) {
            clickWindow.setActionNumber(clickEvent.getActionNumber());
            event.markForRewrite();
        }
        if (clickWindow.getWindowId() != clickEvent.getWindowId()) {
            clickWindow.setWindowId(clickEvent.getWindowId());
            event.markForRewrite();
        }
        if (clickWindow.getClickType() != clickEvent.getClickType()) {
            clickWindow.setClickType(clickEvent.getClickType());
            event.markForRewrite();
        }
        if (slot != clickEvent.getSlot()) {
            clickWindow.setSlot((short) clickEvent.getSlot());
            event.markForRewrite();
        }

        if (InventoryModule.isSpigotInventoryTracking()) {
            handleClick(clickEvent, event);
        }
    }

    private void handleClick(final InventoryClickEvent clickEvent, final PacketReceiveEvent<? extends DefinedPacket> event) {
        final PlayerInventory playerInventory = InventoryManager.getInventory(event.getPlayer().getUniqueId(), event.getServerInfo().getName());
        final InventoryAction action = new InventoryAction();
        action.setPreviousCursor(playerInventory.getOnCursor());

        if (clickEvent.getClickType() == ClickType.LEFT_CLICK) {
            if (playerInventory.getOnCursor() == null) {
                if (clickEvent.getClickedItem() != null) {
                    action.setNewCursor(clickEvent.getClickedItem().deepClone());
                    action.removeItem(clickEvent.getSlot());
                }
            } else {
                if (clickEvent.getClickedItem() == null) {
                    action.setItem(clickEvent.getSlot(), playerInventory.getOnCursor().deepClone());
                    action.setNewCursor(null);
                } else {
                    if (clickEvent.getClickedItem().canBeStacked(playerInventory.getOnCursor())) {
                        if (clickEvent.getClickedItem().getAmount() + playerInventory.getOnCursor().getAmount() > clickEvent.getClickedItem().getType().getMaxStackSize()) {
                            final int tillFull = clickEvent.getClickedItem().getType().getMaxStackSize() - clickEvent.getClickedItem().getAmount();
                            final ItemStack newCursor = playerInventory.getOnCursor().deepClone();
                            newCursor.setAmount((byte) (newCursor.getAmount() - tillFull));
                            action.setNewCursor(newCursor);
                            final ItemStack stack = clickEvent.getClickedItem().deepClone();
                            stack.setAmount((byte) stack.getType().getMaxStackSize());
                            action.setItem(clickEvent.getSlot(), stack);
                        } else {
                            final ItemStack stack = clickEvent.getClickedItem().deepClone();
                            stack.setAmount((byte) (stack.getAmount() + playerInventory.getOnCursor().getAmount()));
                            action.setItem(clickEvent.getSlot(), stack);
                            action.setNewCursor(null);
                        }
                    } else {
                        action.setNewCursor(clickEvent.getClickedItem().deepClone());
                        action.setItem(clickEvent.getSlot(), playerInventory.getOnCursor().deepClone());
                    }
                }
            }
        } else if (clickEvent.getClickType() == ClickType.RIGHT_CLICK) {
            if (playerInventory.getOnCursor() == null) {
                if (clickEvent.getClickedItem() != null) {
                    final ItemStack inv = clickEvent.getClickedItem().deepClone();
                    final ItemStack cursor = inv.deepClone();
                    final double half = (double) cursor.getAmount() / 2;
                    cursor.setAmount((byte) Math.round(half));
                    inv.setAmount((byte) Math.floor(half));
                    action.setNewCursor(cursor);
                    action.setItem(clickEvent.getSlot(), inv);
                }
            } else {
                if (clickEvent.getClickedItem() == null) {
                    final ItemStack newItem = playerInventory.getOnCursor().deepClone();
                    newItem.setAmount((byte) 1);
                    action.setItem(clickEvent.getSlot(), newItem);

                    final ItemStack cursor = playerInventory.getOnCursor().deepClone();
                    cursor.setAmount((byte) (cursor.getAmount() - 1));
                    if (cursor.getAmount() <= 0) {
                        action.setNewCursor(null);
                    } else {
                        action.setNewCursor(cursor);
                    }
                } else {
                    final ItemStack cursor = playerInventory.getOnCursor().deepClone();
                    final ItemStack current = clickEvent.getClickedItem().deepClone();
                    if (current.canBeStacked(cursor)) {
                        if (current.getAmount() + 1 < current.getType().getMaxStackSize()) {
                            cursor.setAmount((byte) (cursor.getAmount() - 1));
                            current.setAmount((byte) (current.getAmount() + 1));
                            action.setItem(clickEvent.getSlot(), current);
                        }
                        if (cursor.getAmount() <= 0) {
                            action.setNewCursor(null);
                        } else {
                            action.setNewCursor(cursor);
                        }
                    } else {
                        action.setItem(clickEvent.getSlot(), cursor);
                        action.setNewCursor(current);
                    }
                }
            }
        } else if (clickEvent.getClickType() == ClickType.CREATIVE_MIDDLE_CLICK) {
//            if (WorldModule.getGamemode(event.getPlayer().getUniqueId()) != null && WorldModule.getGamemode(event.getPlayer().getUniqueId()) == Gamemode.CREATIVE) {
//                final ItemStack newCursor = clickEvent.getClickedItem().deepClone();
//                newCursor.setAmount((byte) newCursor.getType().getMaxStackSize());
//                action.setNewCursor(newCursor);
//            } else {
            action.setNewCursor(playerInventory.getOnCursor());
//            }
        } else if (clickEvent.getClickType() == ClickType.DROP) {
            if (clickEvent.getSlot() == -999) {
                action.setNewCursor(null);
            } else {
                final ItemStack inv = clickEvent.getClickedItem().deepClone();
                inv.setAmount((byte) (inv.getAmount() - 1));
                if (inv.getAmount() <= 0) {
                    action.removeItem(clickEvent.getSlot());
                } else {
                    action.setItem(clickEvent.getSlot(), inv);
                }
            }
        } else if (clickEvent.getClickType() == ClickType.DROP_ALL) {
            action.setNewCursor(null);
        } else if (clickEvent.getClickType() == ClickType.DRAG_START_LEFT || clickEvent.getClickType() == ClickType.DRAG_START_CREATIVE_MIDDLE || clickEvent.getClickType() == ClickType.DRAG_START_RIGHT) {
            action.beginDragging();
            action.setNewCursor(playerInventory.getOnCursor());
        } else if (clickEvent.getClickType() == ClickType.DRAG_STOP_LEFT || clickEvent.getClickType() == ClickType.DRAG_STOP_CREATIVE_MIDDLE || clickEvent.getClickType() == ClickType.DRAG_STOP_RIGHT) {
            action.endDragging(clickEvent.getClickType());
            action.setNewCursor(playerInventory.getOnCursor());
        } else if (clickEvent.getClickType() == ClickType.DRAG_ADD_SLOT_LEFT || clickEvent.getClickType() == ClickType.DRAG_ADD_SLOT_CREATIVE_MIDDLE || clickEvent.getClickType() == ClickType.DRAG_ADD_SLOT_RIGHT) {
            action.setDragSlot(clickEvent.getSlot());
            action.setNewCursor(playerInventory.getOnCursor());
        } else if (clickEvent.getClickType() == ClickType.SHIFT_LEFT_CLICK) {

        }
        InventoryModule.registerInventoryAction(event.getPlayer().getUniqueId(), clickEvent.getWindowId(), clickEvent.getActionNumber(), action);
    }
}
