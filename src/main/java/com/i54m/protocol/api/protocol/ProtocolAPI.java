package com.i54m.protocol.api.protocol;

import com.i54m.protocol.api.event.EventManager;

/**
 * This class contains all relevant methods to use Protocolize.
 */
public final class ProtocolAPI {

    private static final PacketRegistration PACKET_REGISTRATION = new PacketRegistration();
    private static final EventManager EVENT_MANAGER = new EventManager();
//    private static final TrafficManager TRAFFIC_MANAGER = new TrafficManager();

    private ProtocolAPI() {}

    /**
     * The packet registration is used to register packets
     * @see PacketRegistration
     * @return instance of {@link PacketRegistration}
     */
    public static PacketRegistration getPacketRegistration() {
        return PACKET_REGISTRATION;
    }

    /**
     * The event manager is used to register {@link com.i54m.protocol.api.handler.PacketListener}s
     * @see EventManager
     * @see com.i54m.protocol.api.handler.PacketListener
     * @return instance of {@link EventManager}
     */
    public static EventManager getEventManager() {
        return EVENT_MANAGER;
    }

//    /**
//     * The traffic manager is used to get information about a connection and it's traffic data.
//     * @return instance of {@link TrafficManager}
//     */
//    public static TrafficManager getTrafficManager() {
//        return TRAFFIC_MANAGER;
//    }

}
