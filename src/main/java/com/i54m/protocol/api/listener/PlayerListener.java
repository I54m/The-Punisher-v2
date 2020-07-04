package com.i54m.protocol.api.listener;

import com.i54m.protocol.api.netty.DecoderChannelHandler;
import com.i54m.protocol.api.netty.EncoderChannelHandler;
import com.i54m.protocol.api.protocol.Stream;
import com.i54m.protocol.api.util.ReflectionUtil;
import com.i54m.protocol.items.InventoryManager;
import com.i54m.protocol.world.WorldModule;
import com.i54m.punisher.PunisherPlugin;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.AbstractPacketHandler;

public class PlayerListener implements Listener {

    private final PunisherPlugin plugin;

    public PlayerListener(final PunisherPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(final PreLoginEvent e) {
        if(!PunisherPlugin.isLoaded())
            return;
        plugin.getNettyPipelineInjector().injectBefore(e.getConnection(), "inbound-boss", "protocol-decoder", new DecoderChannelHandler((AbstractPacketHandler) e.getConnection(), Stream.UPSTREAM));
        plugin.getNettyPipelineInjector().injectAfter(e.getConnection(), "protocol-decoder", "protocol-encoder", new EncoderChannelHandler((AbstractPacketHandler) e.getConnection()));
    }

    @EventHandler
    public void onServerDisconnect(final ServerDisconnectEvent e) {
        InventoryManager.unmapServer(e.getPlayer().getUniqueId(), e.getTarget().getName());
    }

    @EventHandler
    public void onServerSwitch(final ServerConnectedEvent e) {
        if(!PunisherPlugin.isLoaded())
            return;
        plugin.getNettyPipelineInjector().injectBefore(e.getServer(), "inbound-boss", "protocol-decoder", new DecoderChannelHandler(ReflectionUtil.getDownstreamBridge(e.getServer()), Stream.DOWNSTREAM));
        plugin.getNettyPipelineInjector().injectAfter(e.getServer(), "protocol-decoder", "protocol-encoder", new EncoderChannelHandler(ReflectionUtil.getDownstreamBridge(e.getServer())));
    }

    @EventHandler
    public void onQuit(final PlayerDisconnectEvent e) {
        InventoryManager.unmap(e.getPlayer().getUniqueId());
        WorldModule.uncache(e.getPlayer().getUniqueId());
    }

}
