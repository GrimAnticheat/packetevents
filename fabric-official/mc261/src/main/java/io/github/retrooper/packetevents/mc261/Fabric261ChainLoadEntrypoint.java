package io.github.retrooper.packetevents.mc261;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;

// Marker entrypoint that announces 26.1 chain participation without touching MC types.
// A real registry implementation needs Mojang-named MC class references
// (net.minecraft.world.item.Item, net.minecraft.core.registries.BuiltInRegistries,
// net.minecraft.resources.Identifier) and lands in a follow-up once the build can
// compile against the pre-deobfuscated 26.X jar.
public class Fabric261ChainLoadEntrypoint implements ChainLoadEntryPoint {

    @Override
    public void initialize(ChainLoadData chainLoadData) {
        // intentionally no-op
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
