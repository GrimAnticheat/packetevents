/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * Licensed under the GNU General Public License v3.0 (see the LICENSE file in the
 * project root or <http://www.gnu.org/licenses/>).
 */

package io.github.retrooper.packetevents.factory.fabric;

import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.manager.AbstractFabricPlayerManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

// Concrete player manager for the 26.X (Mojang-named) branch. Mirror of the
// fabric-intermediary mc-version managers but bound against the official names that
// the deobfuscated 26.1.2 jar already uses.
public class FabricOfficialPlayerManager extends AbstractFabricPlayerManager {

    public FabricOfficialPlayerManager(PacketEventsAPI<?> packetEventsAPI) {
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
            // Connection.channel access requires the AW entry shipped beside this class.
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
        // ServerPlayer doesn't expose getServer() directly in 26.X mappings; the
        // ServerLevel reference does.
        sp.level().getServer().execute(() -> disconnectPlayer(sp, message));
    }
}
