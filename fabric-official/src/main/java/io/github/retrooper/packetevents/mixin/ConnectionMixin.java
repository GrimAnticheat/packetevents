package io.github.retrooper.packetevents.mixin;

import com.github.retrooper.packetevents.protocol.PacketSide;
import io.github.retrooper.packetevents.util.FabricInjectionUtil;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(
            method = "configureSerialization",
            at = @At("TAIL")
    )
    private static void configureSerialization(
            ChannelPipeline pipeline, PacketFlow flow, boolean memoryOnly,
            BandwidthDebugMonitor bandwidthDebugMonitor, CallbackInfo ci
    ) {
        PacketSide side = switch (flow) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };
        FabricInjectionUtil.injectAtPipelineBuilder(pipeline, side);
    }
}
