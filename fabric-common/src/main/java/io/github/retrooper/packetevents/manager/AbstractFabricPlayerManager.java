package io.github.retrooper.packetevents.manager;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.github.retrooper.packetevents.impl.netty.manager.player.PlayerManagerAbstract;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFabricPlayerManager extends PlayerManagerAbstract {

    private final PacketEventsAPI<?> packetEventsAPI;

    public AbstractFabricPlayerManager(PacketEventsAPI<?> packetEventsAPI) {
        this.packetEventsAPI = packetEventsAPI;
    }

    @Override
    public ConnectionState getConnectionState(@NotNull Object player) throws IllegalStateException {
        return getUser(player).getConnectionState();
    }

    @Override
    public void sendPacket(@NotNull Object player, @NotNull Object byteBuf) {
        packetEventsAPI.getProtocolManager().sendPacket(getChannel(player), byteBuf);
    }
    @Override
    public void sendPacket(@NotNull Object player, @NotNull PacketWrapper<?> wrapper) {
        packetEventsAPI.getProtocolManager().sendPacket(getChannel(player), wrapper);
    }

    @Override
    public void sendPacketSilently(@NotNull Object player, @NotNull Object byteBuf) {
        packetEventsAPI.getProtocolManager().sendPacketSilently(getChannel(player), byteBuf);
    }

    @Override
    public void sendPacketSilently(@NotNull Object player, @NotNull PacketWrapper<?> wrapper) {
        packetEventsAPI.getProtocolManager().sendPacketSilently(getChannel(player), wrapper);
    }

    @Override
    public void writePacket(@NotNull Object player, @NotNull Object byteBuf) {
        packetEventsAPI.getProtocolManager().writePacket(getChannel(player), byteBuf);
    }

    @Override
    public void writePacket(@NotNull Object player, @NotNull PacketWrapper<?> wrapper) {
        packetEventsAPI.getProtocolManager().writePacket(getChannel(player), wrapper);
    }

    @Override
    public void writePacketSilently(@NotNull Object player, @NotNull Object byteBuf) {
        packetEventsAPI.getProtocolManager().writePacketSilently(getChannel(player), byteBuf);
    }

    @Override
    public void writePacketSilently(@NotNull Object player, @NotNull PacketWrapper<?> wrapper) {
        packetEventsAPI.getProtocolManager().writePacketSilently(getChannel(player), wrapper);
    }

    @Override
    public void receivePacket(Object player, Object byteBuf) {
        packetEventsAPI.getProtocolManager().receivePacket(getChannel(player), byteBuf);
    }

    @Override
    public void receivePacket(Object player, PacketWrapper<?> wrapper) {
        packetEventsAPI.getProtocolManager().receivePacket(getChannel(player), wrapper);
    }

    @Override
    public void receivePacketSilently(Object player, Object byteBuf) {
        packetEventsAPI.getProtocolManager().receivePacketSilently(getChannel(player), byteBuf);
    }

    @Override
    public void receivePacketSilently(Object player, PacketWrapper<?> wrapper) {
        packetEventsAPI.getProtocolManager().receivePacketSilently(getChannel(player), wrapper);
    }

    /**
     * Disconnect a player with the given message. The runtime object is the platform's
     * server-player type (yarn {@code ServerPlayerEntity} on fabric-intermediary, Mojang
     * {@code ServerPlayer} on fabric-official) — common code should never inspect it.
     */
    public abstract void disconnectPlayer(@NotNull Object serverPlayer, @NotNull String message);

    /**
     * Called from the netty pipeline when a packet processing exception should kick the
     * player. Concrete impls own the server-thread hop and the platform-typed cast; the
     * default forwards directly to {@link #disconnectPlayer(Object, String)}, which is
     * safe for hand-rolled tests but real impls should schedule onto the server thread.
     */
    public void kickOnException(@NotNull Object serverPlayer, @NotNull String message) {
        disconnectPlayer(serverPlayer, message);
    }
}
