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
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import io.github.retrooper.packetevents.util.FabricInjectionUtil;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal @ChannelHandler.Sharable
public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final PacketSide side;
    public User user;
    public Object player;
    private final boolean preViaVersion;

    public PacketDecoder(PacketSide side, User user, boolean preViaVersion) {
        this.side = side.getOpposite();
        this.user = user;
        this.preViaVersion = preViaVersion;
    }

    public PacketDecoder(PacketSide side, User user) {
        this(side, user, false);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!msg.isReadable()) {
            return;
        }
        // We still call preVia listeners if ViaVersion is not available
        if (!preViaVersion && PacketEvents.getAPI().getSettings().isPreViaInjection() && !ViaVersionUtil.isAvailable(user)) {
            PacketEventsImplHelper.handleServerBoundPacket(ctx.channel(), user, player, msg, false);
        }
        PacketEventsImplHelper.handlePacket(ctx.channel(), this.user, this.player,
                msg, !preViaVersion, this.side);
        if (msg.isReadable()) {
            out.add(msg.retain());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        boolean kryptonReorder = false;
        switch (evt.toString()) {
            case "COMPRESSION_THRESHOLD_UPDATED":
            case "COMPRESSION_ENABLED":
                kryptonReorder = true;
        }
        if (evt.getClass().getName().equals("com.viaversion.fabric.common.handler.PipelineReorderEvent") || kryptonReorder) {
            FabricInjectionUtil.reorderHandlers(ctx.pipeline(), side.getOpposite());
        }
        super.userEventTriggered(ctx, evt);
    }
}
