package com.i54m.protocol.api.handler;

import com.i54m.protocol.api.event.PacketReceiveEvent;
import com.i54m.protocol.api.event.PacketSendEvent;
import com.i54m.protocol.api.protocol.Stream;
import net.md_5.bungee.protocol.DefinedPacket;

/**
 * A PacketListener interface is used to listen for packets of a specific type. It uses {@link net.md_5.bungee.event.EventPriority} to determine the call order when multiple listeners
 * of the same type are registered.
 * @param <T> the packet type
 */
public interface PacketListener<T extends DefinedPacket> {

    /**
     * Called when a desired packet arrives at the stream
     * @param event the event containing the information
     */
    void receive(PacketReceiveEvent<? extends DefinedPacket> event);

    /**
     * Called when a desired packet wants to leave the stream
     * @param event the event containing the information
     */
    void send(PacketSendEvent<? extends DefinedPacket> event);

    Stream getStream();
    Class<T> getPacketClass();
    byte getPriority();

}
