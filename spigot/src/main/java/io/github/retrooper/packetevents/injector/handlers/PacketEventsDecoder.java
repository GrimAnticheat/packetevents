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

package io.github.retrooper.packetevents.injector.handlers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.exception.PacketProcessException;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.ExceptionUtil;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import io.github.retrooper.packetevents.injector.connection.ServerConnectionInitializer;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;

public class PacketEventsDecoder extends MessageToMessageDecoder<ByteBuf> {
    public User user;
    public Player player;
    public boolean hasBeenRelocated;
    public boolean preViaVersion;

    public PacketEventsDecoder(User user, boolean preViaVersion) {
        this.user = user;
        this.preViaVersion = preViaVersion;
    }

    public PacketEventsDecoder(PacketEventsDecoder decoder) {
        user = decoder.user;
        player = decoder.player;
        hasBeenRelocated = decoder.hasBeenRelocated;
        preViaVersion = decoder.preViaVersion;
    }

    public void read(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) throws Exception {
        try {
            // We still call preVia listeners if ViaVersion is not available
            if (!preViaVersion && PacketEvents.getAPI().getSettings().isPreViaInjection() && !ViaVersionUtil.isAvailable()) {
                PacketEventsImplHelper.handleServerBoundPacket(ctx.channel(), user, player, input, preViaVersion);
            }

            PacketEventsImplHelper.handleServerBoundPacket(ctx.channel(), user, player, input, !preViaVersion);

            out.add(ByteBufHelper.retain(input));
        } catch (Throwable e) {
            throw new PacketProcessException(e);
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        if (buffer.isReadable()) {
            read(ctx, buffer, out);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!ExceptionUtil.isException(cause, PacketProcessException.class)) {
            super.exceptionCaught(ctx, cause);
            return;
        }

        if (!SpigotReflectionUtil.isMinecraftServerInstanceDebugging()) {
            if (user != null && user.getDecoderState() != ConnectionState.HANDSHAKING) {
                if (PacketEvents.getAPI().getSettings().isFullStackTraceEnabled()) {
                    PacketEvents.getAPI().getLogger().log(Level.WARNING, cause, () -> "An error occurred while processing a packet from " + user.getProfile().getName() + " (preVia: " + preViaVersion + ")");
                } else {
                    PacketEvents.getAPI().getLogManager().warn(cause.getMessage());
                }
            }
        }

        if (PacketEvents.getAPI().getSettings().isKickOnPacketExceptionEnabled()) {
            try {
                if (user != null) {
                    user.sendPacket(new WrapperPlayServerDisconnect(Component.text("Invalid packet")));
                }
            } catch (Exception ignored) { // There may (?) be an exception if the player is in the wrong state...
                // Do nothing.
            }
            ctx.channel().close();
            if (player != null) {
                FoliaScheduler.getEntityScheduler().runDelayed(player, (Plugin) PacketEvents.getAPI().getPlugin(), (o) -> player.kickPlayer("Invalid packet"), null, 1);
            }

            if (user != null) {
                PacketEvents.getAPI().getLogManager().warn("Disconnected " + user.getProfile().getName() + " due to invalid packet!");
                cause.printStackTrace();
            }
        }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object event) throws Exception {
        if (PacketEventsEncoder.COMPRESSION_ENABLED_EVENT == null || event != PacketEventsEncoder.COMPRESSION_ENABLED_EVENT) {
            super.userEventTriggered(ctx, event);
            return;
        }

        // Via changes the order of handlers in this event, so we must respond to Via changing their stuff
        if (!preViaVersion) {
            // 1.20.4 has a bug where userEventTriggered is called twice, so Via relocates twice uselessly and we must do so
            ServerConnectionInitializer.relocateHandlers(ctx.channel(), user, false, true);
            if (PacketEvents.getAPI().getSettings().isPreViaInjection() && ViaVersionUtil.isAvailable())
                ServerConnectionInitializer.relocateHandlers(ctx.channel(), user, true, true);
        }
        super.userEventTriggered(ctx, event);
    }

}
