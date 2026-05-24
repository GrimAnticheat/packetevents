package io.github.retrooper.packetevents.mc261;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;

// Chain participant for MC 26.1+. PacketEventsMod (in fabric-common) discovers
// this via the peMainChainLoad entrypoint key and dispatches by descending
// protocol version. initialize() is intentionally empty — wiring a 26.X-native
// AbstractFabricPlayerManager implementation into chainLoadData needs Mojang
// class references that we can't compile against the empty intermediary:0.0.0
// mapping. Filling this in is the next step once Loom can resolve Mojang names.
public class Fabric261ChainLoadEntrypoint implements ChainLoadEntryPoint {

    @Override
    public void initialize(ChainLoadData chainLoadData) {
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
