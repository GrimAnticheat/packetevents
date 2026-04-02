package io.github.retrooper.packetevents;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.factory.fabric.FabricPlayerManager;
import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;
import io.github.retrooper.packetevents.manager.AbstractFabricPlayerManager;
import io.github.retrooper.packetevents.util.LazyHolder;

public class FabricOfficialChainLoadEntrypoint implements ChainLoadEntryPoint {

    protected LazyHolder<AbstractFabricPlayerManager> playerManagerHolder =
            LazyHolder.simple(() -> new FabricPlayerManager());

    @Override
    public void initialize(ChainLoadData chainLoadData) {
        chainLoadData.setPlayerManagerIfNull(playerManagerHolder);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
