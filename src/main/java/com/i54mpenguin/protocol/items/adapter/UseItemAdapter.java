package com.i54mpenguin.protocol.items.adapter;

import com.i54mpenguin.protocol.api.event.PacketReceiveEvent;
import com.i54mpenguin.protocol.api.handler.PacketAdapter;
import com.i54mpenguin.protocol.api.protocol.Stream;
import com.i54mpenguin.protocol.items.InventoryManager;
import com.i54mpenguin.protocol.items.ItemStack;
import com.i54mpenguin.protocol.items.ItemType;
import com.i54mpenguin.protocol.items.PlayerInventory;
import com.i54mpenguin.protocol.items.event.PlayerInteractEvent;
import com.i54mpenguin.protocol.items.packet.UseItem;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
@Deprecated
public class UseItemAdapter extends PacketAdapter<UseItem> {

    public UseItemAdapter() {
        super(Stream.UPSTREAM, UseItem.class);
    }

    @Override
    public void receive(final PacketReceiveEvent<UseItem> event) {
        final ProxiedPlayer p = event.getPlayer();
        final PlayerInventory playerInventory = InventoryManager.getCombinedSendInventory(p.getUniqueId(), event.getServerInfo().getName());
        final ItemStack inHand = playerInventory.getItem(playerInventory.getHeldItem()+36);
        if(inHand == null || inHand.getType() == ItemType.NO_DATA)
            return;
        final PlayerInteractEvent event1 = new PlayerInteractEvent(p, inHand, null, event.getPacket().getHand());
        ProxyServer.getInstance().getPluginManager().callEvent(event1);
        if(event.getPacket().getHand() != event1.getHand()) {
            event.getPacket().setHand(event1.getHand());
            event.markForRewrite();
        }
        if(inHand.isHomebrew() || event1.isCancelled()) {
            event.setCancelled(true);
        }
    }
}
