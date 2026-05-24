package io.github.retrooper.packetevents.mc261;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.factory.fabric.FabricOfficialPlayerManager;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;
import io.github.retrooper.packetevents.manager.AbstractFabricPlayerManager;
import io.github.retrooper.packetevents.util.LazyHolder;

// Wires the 26.X-mapped player manager into the ChainLoadData. Registry-manager
// registration is deferred — 26.X's BuiltInRegistries layout differs from yarn and
// requires its own ItemRegistry implementation to be useful; we intentionally leave
// that slot empty so an older chain entrypoint can fill it as a stub fallback.
public class Fabric261ChainLoadEntrypoint implements ChainLoadEntryPoint {

    private final LazyHolder<AbstractFabricPlayerManager> playerManager =
            LazyHolder.simple(() -> new FabricOfficialPlayerManager(FabricPacketEventsAPI.getServerAPI()));

    @Override
    public void initialize(ChainLoadData chainLoadData) {
        chainLoadData.setPlayerManagerIfNull(playerManager);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
