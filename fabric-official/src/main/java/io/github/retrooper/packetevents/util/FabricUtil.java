package io.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.PacketSide;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

public final class FabricUtil {

    private FabricUtil() {
    }

    public static boolean isOurConnection(Connection connection) {
        return isOurConnection(connection.getReceiving());
    }

    public static boolean isOurConnection(PacketFlow flow) {
        PacketSide connectionSide = switch (flow) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        return api != null && api.getInjector().getPacketSide() == connectionSide;
    }
}
