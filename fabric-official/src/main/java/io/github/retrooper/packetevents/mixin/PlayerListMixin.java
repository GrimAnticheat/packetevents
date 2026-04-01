package io.github.retrooper.packetevents.mixin;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.util.FabricInjectionUtil;
import io.github.retrooper.packetevents.util.FabricUtil;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(
            method = "placeNewPlayer",
            at = @At("HEAD")
    )
    private void preNewPlayerPlace(
            Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci
    ) {
        if (FabricUtil.isOurConnection(connection)) {
            PacketEvents.getAPI().getInjector().setPlayer(connection.channel, player);
        }
    }

    @Inject(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onPlayerLogin(
            Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci
    ) {
        if (!FabricUtil.isOurConnection(connection)) {
            return;
        }
        FabricInjectionUtil.fireUserLoginEvent(player);
    }

    @Inject(
            method = "respawn",
            at = @At("RETURN")
    )
    private void postRespawn(CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = cir.getReturnValue();
        if (FabricUtil.isOurConnection(player.connection.connection)) {
            PacketEvents.getAPI().getInjector().setPlayer(player.connection.connection.channel, player);
        }
    }
}
