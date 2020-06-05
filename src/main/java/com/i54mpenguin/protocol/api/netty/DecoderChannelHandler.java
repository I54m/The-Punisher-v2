package com.i54mpenguin.protocol.api.netty;

import com.i54mpenguin.protocol.api.CancelSendSignal;
import com.i54mpenguin.protocol.api.protocol.ProtocolAPI;
import com.i54mpenguin.protocol.api.protocol.Stream;
import com.i54mpenguin.protocol.api.util.ReflectionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.unix.Errors.NativeIoException;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.protocol.*;
import net.md_5.bungee.protocol.ProtocolConstants.Direction;

import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map.Entry;

public class DecoderChannelHandler extends MessageToMessageDecoder<PacketWrapper> {

    private final AbstractPacketHandler abstractPacketHandler;
    private final Connection connection;
    private final Stream stream;
    private Direction direction;
    private Protocol protocol;
    private int protocolVersion;

    public DecoderChannelHandler(final AbstractPacketHandler abstractPacketHandler, final Stream stream) {
        this.abstractPacketHandler = abstractPacketHandler;
        connection = ReflectionUtil.getConnection(abstractPacketHandler, ReflectionUtil.serverConnectorClass.isInstance(abstractPacketHandler));
        this.stream = stream;
        try {
            if (abstractPacketHandler.getClass().getSimpleName().equals("ServerConnector")) {
                direction = Direction.TO_CLIENT;
                final Object ch = ReflectionUtil.serverConnectorChannelWrapperField.get(abstractPacketHandler);
                final Channel channel = (Channel) ReflectionUtil.channelWrapperChannelField.get(ch);
                final MinecraftDecoder minecraftDecoder = channel.pipeline().get(MinecraftDecoder.class);
                protocolVersion = (int) ReflectionUtil.protocolVersionField.get(minecraftDecoder);
                protocol = (Protocol) ReflectionUtil.protocolField.get(minecraftDecoder);
            } else {
                if (abstractPacketHandler.getClass().getSimpleName().equals("InitialHandler")) {
                    final Object ch = ReflectionUtil.initialHandlerChannelWrapperField.get(abstractPacketHandler);
                    final Channel channel = (Channel) ReflectionUtil.channelWrapperChannelField.get(ch);
                    final MinecraftDecoder minecraftDecoder = channel.pipeline().get(MinecraftDecoder.class);
                    protocolVersion = (int) ReflectionUtil.protocolVersionField.get(minecraftDecoder);
                    protocol = (Protocol) ReflectionUtil.protocolField.get(minecraftDecoder);
                } else {
                    final Object ch = ReflectionUtil.userConnectionChannelWrapperField.get(abstractPacketHandler);
                    final Channel channel = (Channel) ReflectionUtil.channelWrapperChannelField.get(ch);
                    final MinecraftDecoder minecraftDecoder = channel.pipeline().get(MinecraftDecoder.class);
                    protocolVersion = (int) ReflectionUtil.protocolVersionField.get(minecraftDecoder);
                    protocol = (Protocol) ReflectionUtil.protocolField.get(minecraftDecoder);
                }
                direction = Direction.TO_SERVER;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final PacketWrapper msg, final List<Object> out) throws Exception {
        if (msg != null) {
            if (msg.packet != null) {

//                // Traffic analysis
//                final TrafficData data = ProtocolAPI.getTrafficManager().getData(ReflectionUtil.getConnectionName(connection), connection);
//                if(stream == Stream.UPSTREAM) {
//                    data.setUpstreamInputCurrentMinute(data.getUpstreamInputCurrentMinute()+msg.buf.readableBytes());
//                    data.setUpstreamInput(data.getUpstreamInput()+msg.buf.readableBytes());
//                } else {
//                    data.setDownstreamInputCurrentMinute(data.getDownstreamInputCurrentMinute()+msg.buf.readableBytes());
//                    data.setDownstreamInput(data.getDownstreamInput()+msg.buf.readableBytes());
//                    data.setDownstreamBridgeName(ReflectionUtil.getServerName(connection));
//                }

                // Packet handling & rewrite
                final Entry<DefinedPacket, Boolean> entry = ProtocolAPI.getEventManager().handleInboundPacket(msg.packet, abstractPacketHandler);
                if(entry == null)
                    return;
                final DefinedPacket packet = entry.getKey();
                if(packet == null)
                    return;
                if(entry.getValue()) {
                    try {
                        // Try packet rewrite
                        final ByteBuf buf = Unpooled.directBuffer();
                        DefinedPacket.writeVarInt(ProtocolAPI.getPacketRegistration().getPacketID(protocol, direction, protocolVersion, packet.getClass()), buf);
                        packet.write(buf, direction, protocolVersion);
                        msg.buf.resetReaderIndex();
                        buf.resetReaderIndex();
                        ReflectionUtil.bufferField.set(msg, buf);
                    } catch (final UnsupportedOperationException ignored) {
                    } // Packet cannot be written
                }
                ReflectionUtil.packetField.set(msg, packet);
                out.add(msg);
            } else {
                out.add(msg);
            }
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause.getClass().equals(CancelSendSignal.INSTANCE.getClass()))
            throw ((Error) cause);
        if(cause instanceof ClosedChannelException) {
            return;
        } else if(cause instanceof NativeIoException) {
            return; // Suppress this annoying exception
        }
        // TODO: 5/06/2020 exception handler
//        if(ProtocolPlugin.isExceptionCausedByProtocol(cause)) {
//            PunisherPlugin.getInstance().getLogger().log(Level.SEVERE, "[Protocol] === EXCEPTION CAUGHT IN DECODER ===");
//            PunisherPlugin.getInstance().getLogger().log(Level.SEVERE, "[Protocol] Stream: "+stream.name());
//            PunisherPlugin.getInstance().getLogger().log(Level.SEVERE, "[Protocol] Connection: "+connection.toString());
//            PunisherPlugin.getInstance().getLogger().log(Level.SEVERE, "[Protocol] Protocol version: "+protocolVersion);
//            cause.printStackTrace();
//        } else {
//            super.exceptionCaught(ctx, cause); // We don't argue with foreign exceptions anymore.
//        }
    }

}
