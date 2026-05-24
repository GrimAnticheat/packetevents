/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * Licensed under the GNU General Public License v3.0 (see the LICENSE file in the
 * project root or <http://www.gnu.org/licenses/>).
 */

package io.github.retrooper.packetevents.mc261.mixin;

import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.util.FabricInjectionUtil;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mojang-named twin of fabric-intermediary's PlayerManagerMixin (mc1140). 26.X
// renamed PlayerManager → PlayerList and onPlayerConnect → placeNewPlayer.
//
// Two injects:
//   HEAD of placeNewPlayer: associate the netty Channel of the incoming Connection
//     with the ServerPlayer object — required so subsequent PlayerManager.getUser
//     lookups can resolve the User → ServerPlayer mapping.
//   AFTER PlayerList.broadcastAll(Packet) call inside placeNewPlayer: the player
//     has fully entered PLAY state, so fire UserLoginEvent. Without this, Grim's
//     check engine never sees the player join and no checks run.
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void packetevents$onPlayerConnect(Connection connection, ServerPlayer player,
                                              net.minecraft.server.network.CommonListenerCookie cookie,
                                              CallbackInfo ci) {
        FabricPacketEventsAPI.getServerAPI().getInjector().setPlayer(connection.channel, player);
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void packetevents$onPlayerLogin(Connection connection, ServerPlayer player,
                                            net.minecraft.server.network.CommonListenerCookie cookie,
                                            CallbackInfo ci) {
        FabricInjectionUtil.fireUserLoginEvent(player);
    }
}
