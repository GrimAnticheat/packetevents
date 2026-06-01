/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * Licensed under the GNU General Public License v3.0 (see the LICENSE file in the
 * project root or <http://www.gnu.org/licenses/>).
 */

package io.github.retrooper.packetevents.mc261.factory.fabric;

import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.manager.AbstractFabricPlayerManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class Fabric261PlayerManager extends AbstractFabricPlayerManager {

    public Fabric261PlayerManager(PacketEventsAPI<?> packetEventsAPI) {
        super(packetEventsAPI);
    }

    @Override
    public int getPing(@NotNull Object player) {
        if (player instanceof ServerPlayer sp) {
            return sp.connection.latency();
        }
        throw new UnsupportedOperationException("Unsupported player implementation: " + player);
    }

    @Override
    public Object getChannel(@NotNull Object player) {
        if (player instanceof ServerPlayer sp) {
            return sp.connection.connection.channel;
        }
        throw new UnsupportedOperationException("Unsupported player implementation: " + player);
    }

    @Override
    public void disconnectPlayer(@NotNull Object player, @NotNull String message) {
        ((ServerPlayer) player).connection.disconnect(Component.literal(message));
    }

    @Override
    public void kickOnException(@NotNull Object player, @NotNull String message) {
        ServerPlayer sp = (ServerPlayer) player;
        sp.level().getServer().execute(() -> disconnectPlayer(sp, message));
    }
}
