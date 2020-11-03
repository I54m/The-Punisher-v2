package com.i54m.protocol.api.netty;

import com.i54m.protocol.api.CancelSendSignal;
import com.i54m.protocol.api.protocol.ProtocolAPI;
import com.i54m.punisher.exceptions.ProtocolException;
import com.i54m.punisher.handlers.ErrorHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.KeepAlive;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EncoderChannelHandler extends MessageToMessageEncoder<DefinedPacket> {

    private final AbstractPacketHandler abstractPacketHandler;
    private final ErrorHandler errorHandler = ErrorHandler.getINSTANCE();

    public EncoderChannelHandler(final AbstractPacketHandler abstractPacketHandler) {
        this.abstractPacketHandler = abstractPacketHandler;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, DefinedPacket msg, final List<Object> out) throws Exception {
        msg = ProtocolAPI.getEventManager().handleOutboundPacket(msg, abstractPacketHandler);
        if (msg != null)
            out.add(msg);
        else
            out.add(new KeepAlive(ThreadLocalRandom.current().nextLong())); // We need to produce at least one message
    }

    @SuppressWarnings("deprecation")
    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause.getClass().equals(CancelSendSignal.INSTANCE.getClass()))
            throw ((Error) cause);
        if (errorHandler.isExceptionCausedByProtocol(cause))
            errorHandler.log(new ProtocolException("Encoder", cause));
        else
            super.exceptionCaught(ctx, cause);
    }

}
