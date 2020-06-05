package com.i54mpenguin.protocol.api.netty;

import com.i54mpenguin.protocol.api.CancelSendSignal;
import com.i54mpenguin.protocol.api.protocol.ProtocolAPI;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.KeepAlive;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EncoderChannelHandler extends MessageToMessageEncoder<DefinedPacket> {

    private final AbstractPacketHandler abstractPacketHandler;

    public EncoderChannelHandler(final AbstractPacketHandler abstractPacketHandler) {
        this.abstractPacketHandler = abstractPacketHandler;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, DefinedPacket msg, final List<Object> out) throws Exception {
        msg = ProtocolAPI.getEventManager().handleOutboundPacket(msg, abstractPacketHandler);
        if(msg != null)
            out.add(msg);
        else
            out.add(new KeepAlive(ThreadLocalRandom.current().nextLong())); // We need to produce at least one message
    }

    @SuppressWarnings("deprecation")
    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if(cause.getClass().equals(CancelSendSignal.INSTANCE.getClass()))
            throw ((Error)cause);
        // TODO: 5/06/2020 exception handler
//        if(ProtocolPlugin.isExceptionCausedByProtocol(cause)) {
//            PunisherPlugin.getInstance().getLogger().log(Level.SEVERE, "[Protocol] Exception caught in encoder.", cause);
//        } else {
//            super.exceptionCaught(ctx, cause);
//        }
    }

}
