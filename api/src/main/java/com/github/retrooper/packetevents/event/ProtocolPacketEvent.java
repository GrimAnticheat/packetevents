/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.event;

import dev.axionize.bolt.api.player.PacketUser;
import dev.axionize.bolt.api.protocol.ConnectionState;
import dev.axionize.bolt.api.types.Packet;
import dev.axionize.bolt.api.capture.PacketBufferWriteListener;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.exception.InvalidDisconnectPacketSend;
import com.github.retrooper.packetevents.exception.PacketProcessException;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.axionize.bolt.api.types.PacketTypes;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public abstract class ProtocolPacketEvent extends PacketEvent implements PlayerEvent, CancellableEvent, UserEvent, dev.axionize.bolt.api.types.event.PacketEvent {

    private final int packetID;
    private final PacketTypeCommon packetType;
    private ServerVersion serverVersion;
    private final Object channel;
    private final ConnectionState connectionState;
    private final PacketUser user;

    private @UnknownNullability Object player;
    private Object byteBuf;

    private boolean cancel;
    private @Nullable PacketWrapper<?> lastUsedWrapper;
    private @Nullable List<Runnable> postTasks = null;
    private @Nullable PacketBufferWriteListener bufferWriteListener;

    private boolean cloned;
    private boolean needsReEncode = PacketEvents.getAPI().getSettings().reEncodeByDefault();

    public ProtocolPacketEvent(
            PacketSide packetSide, Object channel, PacketUser user,
            @UnknownNullability Object player, Object byteBuf,
            boolean autoProtocolTranslation
    ) throws PacketProcessException {
        this.channel = channel;
        this.user = user;
        this.player = player;

        boolean isServerBound = packetSide == PacketSide.CLIENT;
        if(autoProtocolTranslation || user.getClientVersion() == null) {
            this.serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
            this.connectionState = isServerBound ? user.getPostViaDecoderState() : user.getPostViaEncoderState();
        } else {
            // Live path: User#getClientVersion narrows to PE's enum which knows toServerVersion.
            // Non-User PacketUser (synthetic in-process channel): fall back to the ambient server
            // version — these channels feed bytes already in server-internal form, no translation
            // needed.
            if (user instanceof User) {
                this.serverVersion = ((User) user).getClientVersion().toServerVersion();
            } else {
                this.serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
            }
            this.connectionState = isServerBound ? user.getPreViaDecoderState() : user.getPreViaEncoderState();
        }

        this.byteBuf = byteBuf;
        int size = ByteBufHelper.readableBytes(byteBuf);
        if (size == 0) {
            throw new PacketProcessException("Trying to process a packet, but it has no content. (Size=0)");
        }
        try {
            this.packetID = ByteBufHelper.readVarInt(byteBuf);
        } catch (Exception e) {
            throw new PacketProcessException("Failed to read the Packet ID of a packet. (Size: " + size + ")");
        }

        ClientVersion version = serverVersion.toClientVersion();

        PacketTypeCommon packetType;
        if (isServerBound) {
            packetType = PacketType.getClientSideById(this.connectionState, version, this.packetID);
        } else {
            packetType = PacketType.getServerSideById(this.connectionState, version, this.packetID);
        }

        if (packetType == null) {
            // mojang messed up and keeps sending disconnect packets in the wrong protocol state
            if (PacketType.getById(packetSide, com.github.retrooper.packetevents.protocol.ConnectionState.PLAY, version, packetID) == PacketType.Play.Server.DISCONNECT) {
                throw new InvalidDisconnectPacketSend();
            }
            throw new PacketProcessException("Failed to map the Packet ID " + packetID + " to a PacketType constant. Bound: " + packetSide.getOpposite() + ", Connection state: " + this.connectionState + ", Server version: " + serverVersion.getReleaseName());
        }
        this.packetType = packetType;
    }

    public ProtocolPacketEvent(
            int packetID, PacketTypeCommon packetType, ServerVersion serverVersion,
            Object channel, PacketUser user,
            @UnknownNullability Object player, Object byteBuf
    ) {
        this.channel = channel;
        this.user = user;
        this.player = player;
        this.serverVersion = serverVersion;
        this.byteBuf = byteBuf;
        this.packetID = packetID;
        this.packetType = packetType;

        this.connectionState = (packetType != null && packetType.getSide() == PacketSide.SERVER)
                ? user.getPlayerEncoderState() : user.getPlayerDecoderState();
        this.cloned = true;
    }

    public void markForReEncode(boolean needsReEncode) {
        if (needsReEncode && !this.needsReEncode && bufferWriteListener != null) {
            bufferWriteListener.onPacketBufferWrite();
        }
        this.needsReEncode = needsReEncode;
    }

    @Override
    public void setBufferWriteListener(@Nullable PacketBufferWriteListener capture) {
        this.bufferWriteListener = capture;
    }

    @Override
    public @Nullable PacketBufferWriteListener getBufferWriteListener() {
        return bufferWriteListener;
    }

    public boolean needsReEncode() {
        return needsReEncode;
    }

    public boolean isClone() {
        return cloned;
    }

    public Object getChannel() {
        return channel;
    }

    public SocketAddress getAddress() {
        return ChannelHelper.remoteAddress(this.channel);
    }

    /**
     * <strong>WARNING:</strong> This doesn't support local addresses or unix sockets.
     */
    public InetSocketAddress getSocketAddress() {
        return (InetSocketAddress) this.getAddress();
    }

    @Override
    public User getUser() {
        return (User) user;
    }

    @Override
    @NullUnmarked
    public <T> @UnknownNullability T getPlayer() {
        return (T) player;
    }

    @Deprecated
    @ApiStatus.Internal
    public void setPlayer(Object player) {
        this.player = player;
    }

    /**
     * Gets the connection state this packet has been sent/received in.
     */
    public com.github.retrooper.packetevents.protocol.ConnectionState getConnectionState() {
        return com.github.retrooper.packetevents.protocol.ConnectionState.getById(connectionState.ordinal());
    }

    @Deprecated
    public ClientVersion getClientVersion() {
        return user instanceof User ? ((User) user).getClientVersion() : null;
    }

    @Deprecated
    public void setClientVersion(ClientVersion clientVersion) {
        PacketEvents.getAPI().getLogManager().debug("Setting client version with deprecated method " + clientVersion.getReleaseName());
        user.setClientVersion(clientVersion);
    }

    public ServerVersion getServerVersion() {
        return serverVersion;
    }

    @Deprecated
    public void setServerVersion(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
    }

    public Object getByteBuf() {
        return byteBuf;
    }

    public void setByteBuf(Object byteBuf) {
        this.byteBuf = byteBuf;
    }

    public int getPacketId() {
        return packetID;
    }

    public PacketTypeCommon getPacketType() {
        return packetType;
    }

    @Deprecated
    public String getPacketName() {
        return ((Enum<?>) packetType).name();
    }

    @Override
    public boolean isCancelled() {
        return this.cancel;
    }

    @Override
    public void setCancelled(boolean val) {
        this.cancel = val;
    }

    @Nullable
    public PacketWrapper<?> getLastUsedWrapper() {
        return lastUsedWrapper;
    }

    public void setLastUsedWrapper(@Nullable PacketWrapper<?> lastUsedWrapper) {
        this.lastUsedWrapper = lastUsedWrapper;
    }

    // Packet API Begin
    // TODO (Packet Rewrite) (if-only-if) work with non PacketWrapper impls
    public void setLastUsedWrapper(@Nullable Packet lastUsedWrapper) {
        this.lastUsedWrapper = (PacketWrapper<?>) lastUsedWrapper;
    }
    // Packet API End

    public List<Runnable> getPostTasks() {
        if (postTasks == null) {
            postTasks = new ArrayList<>();
        }
        return postTasks;
    }

    public boolean hasPostTasks() {
        return postTasks != null && !postTasks.isEmpty();
    }

    @Override
    public ProtocolPacketEvent clone() {
        return this instanceof PacketReceiveEvent ? ((PacketReceiveEvent) this).clone()
                : ((PacketSendEvent) this).clone();
    }

    public void cleanUp() {
        if (isClone()) {
            ByteBufHelper.release(byteBuf);
        }
    }

    public Object getFullBufferClone() {
        byte[] data = ByteBufHelper.copyBytes(getByteBuf());
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        ByteBufHelper.writeVarInt(buffer, getPacketId());
        ByteBufHelper.writeBytes(buffer, data);
        return buffer;
    }

    @Override
    public ConnectionState getConnectionStatus() {
        return connectionState;
    }
}
