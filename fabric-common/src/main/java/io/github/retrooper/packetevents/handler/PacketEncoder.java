/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
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

package io.github.retrooper.packetevents.handler;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.exception.CancelPacketException;
import com.github.retrooper.packetevents.exception.InvalidDisconnectPacketSend;
import com.github.retrooper.packetevents.exception.PacketProcessException;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.ExceptionUtil;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal @ChannelHandler.Sharable
public class PacketEncoder extends ChannelOutboundHandlerAdapter {

    private static final boolean NETTY_4_1_0;

    static {
        boolean netty410 = false;
        try {
            ChannelPromise.class.getDeclaredMethod("unvoid");
            netty410 = true;
        } catch (NoSuchMethodException ignored) {
        }
        NETTY_4_1_0 = netty410;
    }

    private final PacketSide side;
    public User user;
    public Object player;
    private ChannelPromise promise;
    private final boolean preViaVersion;

    public PacketEncoder(PacketSide side, User user, boolean preViaVersion) {
        this.side = side;
        this.user = user;
        this.preViaVersion = preViaVersion;
    }

    public PacketEncoder(PacketSide side, User user) {
        this(side, user, false);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof ByteBuf in)) {
            ctx.write(msg, promise);
            return;
        }

        // Handle promise management
        ChannelPromise oldPromise = this.promise != null && !this.promise.isSuccess() ? this.promise : null;
        if (NETTY_4_1_0) {
            promise = promise.unvoid();
        }
        promise.addListener(p -> this.promise = oldPromise);
        this.promise = promise;

        handlePacket(ctx, in, promise);

        if (!ByteBufHelper.isReadable(in)) {
            throw CancelPacketException.INSTANCE;
        } else {
            ctx.write(in, promise);
        }
    }

    private @Nullable ProtocolPacketEvent handlePacket(ChannelHandlerContext ctx, ByteBuf buffer, ChannelPromise promise) throws Exception {
        if (!preViaVersion && PacketEvents.getAPI().getSettings().isPreViaInjection() && !ViaVersionUtil.isAvailable(user)) {
            // Intentionally ignore the pre-Via return; the authoritative event is produced by the main pass below.
            PacketEventsImplHelper.handlePacket(ctx.channel(), user, player, buffer, preViaVersion, this.side);
        }

        ProtocolPacketEvent protocolPacketEvent = PacketEventsImplHelper.handlePacket(
                ctx.channel(), this.user, this.player, buffer, !preViaVersion, this.side
        );

        if (protocolPacketEvent instanceof PacketSendEvent packetSendEvent && packetSendEvent.hasTasksAfterSend()) {
            promise.addListener((p) -> {
                for (Runnable task : packetSendEvent.getTasksAfterSend()) {
                    task.run();
                }
            });
        }
        return protocolPacketEvent;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ExceptionUtil.isException(cause, CancelPacketException.class)) return;
        if (ExceptionUtil.isException(cause, InvalidDisconnectPacketSend.class)) return;

        boolean didWeCauseThis = ExceptionUtil.isException(cause, PacketProcessException.class);
        if (didWeCauseThis && (user == null || user.getEncoderState() != ConnectionState.HANDSHAKING)) {
            if (PacketEvents.getAPI().getSettings().isKickOnPacketExceptionEnabled()) {
                try {
                    if (user != null && player != null) {
                        WrapperPlayServerDisconnect disconnectPacket = new WrapperPlayServerDisconnect(
                                net.kyori.adventure.text.Component.text("Invalid packet")
                        );
                        user.sendPacket(disconnectPacket);
                    }
                } catch (Exception ignored) {}
                ctx.channel().close();
                if (player != null) {
                    FabricPacketEventsAPI.getServerAPI().getPlayerManager().kickOnException(player, "Invalid packet");
                }
            }
        }
        super.exceptionCaught(ctx, cause);
    }
}
