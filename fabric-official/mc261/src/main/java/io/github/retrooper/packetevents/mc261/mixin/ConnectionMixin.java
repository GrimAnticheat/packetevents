/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * Licensed under the GNU General Public License v3.0 (see the LICENSE file in the
 * project root or <http://www.gnu.org/licenses/>).
 */

package io.github.retrooper.packetevents.mc261.mixin;

import com.github.retrooper.packetevents.protocol.PacketSide;
import io.github.retrooper.packetevents.util.FabricInjectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Injects PE's encoder/decoder into the netty pipeline. Three hooks:
//
//   configureSerialization (static, TAIL) — first pipeline setup at channel init.
//   setupInboundProtocol (instance, TAIL) — fires on each state transition
//     (LOGIN → CONFIGURATION → PLAY). MC replaces the decoder handler, which
//     removes PE's pe-decoder. Re-inject here so PE captures PLAY packets.
//   setupOutboundProtocol (instance, TAIL) — same for the outbound encoder.
//
// Without the setupInbound/Outbound hooks PE only captures LOGIN + CONFIGURATION
// packets and the downstream consumer (Grim) never sees PLAY-state movement data.
@Mixin(value = Connection.class, priority = 1500)
public abstract class ConnectionMixin {

    @Shadow @Final private PacketFlow receiving;
    @Shadow private Channel channel;

    @Inject(
            method = "configureSerialization(Lio/netty/channel/ChannelPipeline;Lnet/minecraft/network/protocol/PacketFlow;ZLnet/minecraft/network/BandwidthDebugMonitor;)V",
            at = @At("TAIL")
    )
    private static void packetevents$injectAtPipelineBuilder(
            ChannelPipeline pipeline,
            PacketFlow flow,
            boolean memoryOnly,
            BandwidthDebugMonitor monitor,
            CallbackInfo ci
    ) {
        PacketSide side = switch (flow) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };
        FabricInjectionUtil.injectAtPipelineBuilder(pipeline, side);
    }

    @Inject(method = "setupInboundProtocol", at = @At("TAIL"))
    private <T extends PacketListener> void packetevents$reinjectOnInboundSwitch(
            ProtocolInfo<T> info, T listener, CallbackInfo ci
    ) {
        packetevents$reinjectPipelineHandlers();
    }

    @Inject(method = "setupOutboundProtocol", at = @At("TAIL"))
    private void packetevents$reinjectOnOutboundSwitch(
            ProtocolInfo<?> info, CallbackInfo ci
    ) {
        packetevents$reinjectPipelineHandlers();
    }

    private void packetevents$reinjectPipelineHandlers() {
        PacketSide side = switch (this.receiving) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };
        FabricInjectionUtil.reinjectPipelineHandlers(this.channel, side);
    }
}
