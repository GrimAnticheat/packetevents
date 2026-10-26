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

package com.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketEvent;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.exception.InvalidHandshakeException;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.axionize.bolt.api.capture.PacketBufferWriteListener;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Level;

public final class PacketEventsImplHelper {

    private PacketEventsImplHelper() {
    }

    public static @Nullable ProtocolPacketEvent handlePacket(
            Object channel, dev.axionize.bolt.api.player.PacketUser user, Object player, Object buffer,
            boolean autoProtocolTranslation, PacketSide side
    ) throws Exception {
        if (side == PacketSide.SERVER) {
            return handleClientBoundPacket(channel, user, player, buffer, autoProtocolTranslation);
        } else {
            return handleServerBoundPacket(channel, user, player, buffer, autoProtocolTranslation);
        }
    }


    public static @Nullable PacketSendEvent handleClientBoundPacket(
            Object channel, dev.axionize.bolt.api.player.PacketUser user, Object player, Object buffer,
        boolean autoProtocolTranslation
    ) throws Exception {
        if (!ByteBufHelper.isReadable(buffer)) {
            PacketBufferWriteListener listener =
                    user.getBoltDispatcher().takePendingBufferWriteListener(buffer);
            if (listener != null) listener.complete(false, false);
            return null;
        }

        int preProcessIndex = ByteBufHelper.readerIndex(buffer);
        PacketBufferWriteListener bufferWriteListener =
                user.getBoltDispatcher().takePendingBufferWriteListener(buffer);
        PacketSendEvent packetSendEvent;
        try {
            packetSendEvent = EventCreationUtil.createSendEvent(channel, user, player, buffer, autoProtocolTranslation); // TODO ASM-ify this and return null if no one cares about the event
        } catch (Throwable failure) {
            if (bufferWriteListener != null) bufferWriteListener.complete(false, false);
            throw failure;
        }
        packetSendEvent.setBufferWriteListener(bufferWriteListener);
        int processIndex = ByteBufHelper.readerIndex(buffer);

        packetSendEvent.setPostCallListenerAction(() -> {
            ByteBufHelper.readerIndex(buffer, processIndex);
        });

        try {
            if (autoProtocolTranslation) {
                PacketEvents.getAPI().getEventManager().dispatchSendPost(packetSendEvent.getPacketType().getInternalId(), packetSendEvent, user.getBoltDispatcher());
            } else {
                PacketEvents.getAPI().getEventManager().dispatchSendPre(packetSendEvent.getPacketType().getInternalId(), packetSendEvent, user.getBoltDispatcher());
            }
        } catch (Exception t) {
            // ignore handshake exceptions
            if (t.getClass() != InvalidHandshakeException.class && (t.getCause() == null || t.getCause().getClass() != InvalidHandshakeException.class)) {
                PacketEvents.getAPI().getLogger().log(Level.WARNING, "PacketEvents caught an unhandled exception while calling your listener.", t);
            }
        }

        processBufferOutcome(buffer, packetSendEvent, preProcessIndex);

        if (packetSendEvent.hasPostTasks()) {
            for (Runnable task : packetSendEvent.getPostTasks()) {
                task.run();
            }
        }

        return packetSendEvent;
    }

    public static @Nullable PacketSendEvent handleClientBoundPacketPre(
            Object channel, dev.axionize.bolt.api.player.PacketUser user, Object player, Object buffer
    ) throws Exception {
        return handleClientBoundPacket(channel, user, player, buffer, false);
    }

    public static @Nullable PacketSendEvent handleClientBoundPacketPost(
            Object channel, dev.axionize.bolt.api.player.PacketUser user, Object player, Object buffer
    ) throws Exception {
        return handleClientBoundPacket(channel, user, player, buffer, true);
    }

    public static @Nullable PacketReceiveEvent handleServerBoundPacket(
            Object channel, dev.axionize.bolt.api.player.PacketUser user, Object player, Object buffer,
        boolean autoProtocolTranslation
    ) throws Exception {
        if (!ByteBufHelper.isReadable(buffer)) {
            PacketBufferWriteListener listener =
                    user.getBoltDispatcher().takePendingBufferWriteListener(buffer);
            if (listener != null) listener.complete(false, false);
            return null;
        }

        int preProcessIndex = ByteBufHelper.readerIndex(buffer);
        PacketBufferWriteListener bufferWriteListener =
                user.getBoltDispatcher().takePendingBufferWriteListener(buffer);
        PacketReceiveEvent packetReceiveEvent;
        try {
            packetReceiveEvent = EventCreationUtil.createReceiveEvent(channel, user, player, buffer, autoProtocolTranslation);
        } catch (Throwable failure) {
            if (bufferWriteListener != null) bufferWriteListener.complete(false, false);
            throw failure;
        }
        packetReceiveEvent.setBufferWriteListener(bufferWriteListener);
        int processIndex = ByteBufHelper.readerIndex(buffer);

        packetReceiveEvent.setPostCallListenerAction(() -> {
            ByteBufHelper.readerIndex(buffer, processIndex);
        });

        try {
            if (autoProtocolTranslation) {
                PacketEvents.getAPI().getEventManager().dispatchReceivePost(packetReceiveEvent.getPacketType().getInternalId(), packetReceiveEvent, user.getBoltDispatcher());
            } else {
                PacketEvents.getAPI().getEventManager().dispatchReceivePre(packetReceiveEvent.getPacketType().getInternalId(), packetReceiveEvent, user.getBoltDispatcher());
            }
        } catch (Exception t) {
            // ignore handshake exceptions
            if (t.getClass() != InvalidHandshakeException.class && (t.getCause() == null || t.getCause().getClass() != InvalidHandshakeException.class)) {
                PacketEvents.getAPI().getLogger().log(Level.WARNING, "PacketEvents caught an unhandled exception while calling your listener.", t);
            }
        }

        processBufferOutcome(buffer, packetReceiveEvent, preProcessIndex);

        if (packetReceiveEvent.hasPostTasks()) {
            for (Runnable task : packetReceiveEvent.getPostTasks()) {
                task.run();
            }
        }
        return packetReceiveEvent;
    }

    public static @Nullable PacketReceiveEvent handleServerBoundPacketPre(
            Object channel, dev.axionize.bolt.api.player.PacketUser user, Object player, Object buffer) throws Exception {
        return handleServerBoundPacket(channel, user, player, buffer, false);
    }

    public static @Nullable PacketReceiveEvent handleServerBoundPacketPost(
            Object channel, dev.axionize.bolt.api.player.PacketUser user, Object player, Object buffer) throws Exception {
        return handleServerBoundPacket(channel, user, player, buffer, true);
    }

    private static void processBufferOutcome(Object buffer, ProtocolPacketEvent event, int preProcessIndex) {
        if (!event.isCancelled()) {
            if (event.getLastUsedWrapper() != null) {
                ByteBufHelper.clear(buffer);
                event.getLastUsedWrapper().writeVarInt(event.getPacketId());
                event.getLastUsedWrapper().write();
            } else {
                ByteBufHelper.readerIndex(buffer, preProcessIndex);
            }
        } else {
            ByteBufHelper.clear(buffer);
        }
        if (event.getBufferWriteListener() != null) {
            event.getBufferWriteListener().complete(event.isCancelled(), event.needsReEncode());
        }
    }

    public static void handleDisconnection(Object channel, @Nullable UUID uuid) {
        synchronized (channel) {
            ProtocolManager protocolManager = PacketEvents.getAPI().getProtocolManager();
            User user = protocolManager.getUser(channel);

            if (user != null) {
                UserDisconnectEvent disconnectEvent = new UserDisconnectEvent(user);
                PacketEvents.getAPI().getEventManager().callUserDisconnect(disconnectEvent, null);
                PacketEvents.getAPI().getEventManager().unregisterUser(user);
                protocolManager.removeUser(user.getChannel());
            }

            if (uuid == null) {
                protocolManager.removeChannel(channel);
            } else {
                protocolManager.removeChannelById(uuid);
            }
        }
    }
}
