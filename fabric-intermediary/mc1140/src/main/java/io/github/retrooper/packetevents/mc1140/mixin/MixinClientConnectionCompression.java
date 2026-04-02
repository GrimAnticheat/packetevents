package io.github.retrooper.packetevents.mc1140.mixin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.PacketSide;
import io.github.retrooper.packetevents.util.FabricInjectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
@Restriction(
    require = {
        @Condition(value = "minecraft", versionPredicates = {"<1.20.5"})
    }
)
public class MixinClientConnectionCompression {
    @Shadow public Channel channel;
    @Shadow public NetworkSide side;

    @Inject(method = "setMinCompressedSize*", at = @At("TAIL"))
    private void onSetMinCompressedSize(CallbackInfo ci) {
        if (this.channel == null || !this.channel.isActive()) return;
        
        ChannelPipeline pipeline = this.channel.pipeline();

        PacketSide pipelineSide = switch (side) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };

        FabricInjectionUtil.reorderHandlers(pipeline, pipelineSide);
    }
}