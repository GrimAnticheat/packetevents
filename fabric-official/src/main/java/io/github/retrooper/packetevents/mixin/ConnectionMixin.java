/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * Licensed under the GNU General Public License v3.0 (see the LICENSE file in the
 * project root or <http://www.gnu.org/licenses/>).
 */

package io.github.retrooper.packetevents.mixin;

import com.github.retrooper.packetevents.protocol.PacketSide;
import io.github.retrooper.packetevents.util.FabricInjectionUtil;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Injects PE's encoder/decoder into the netty pipeline right after MC builds it for
// every connection (client + server). Priority 1500 keeps us after ViaVersion. The
// fabric-intermediary side has the same hook against the yarn-named ClientConnection
// equivalent; this is the Mojang-named twin.
@Mixin(value = Connection.class, priority = 1500)
public abstract class ConnectionMixin {

    @Inject(
            method = "configureSerialization(Lio/netty/channel/ChannelPipeline;Lnet/minecraft/network/protocol/PacketFlow;ZLnet/minecraft/network/BandwidthDebugMonitor;)V",
            at = @At("TAIL")
    )
    private static void packetevents$injectAtPipelineBuilder(
            ChannelPipeline pipeline,
            PacketFlow flow,
            boolean memoryOnly,
            Object monitor,
            CallbackInfo ci
    ) {
        PacketSide side = switch (flow) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };
        FabricInjectionUtil.injectAtPipelineBuilder(pipeline, side);
    }
}
