package io.github.retrooper.packetevents.mc261;

import com.github.retrooper.packetevents.manager.registry.ItemRegistry;
import com.github.retrooper.packetevents.manager.registry.RegistryManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;
import io.github.retrooper.packetevents.manager.AbstractFabricPlayerManager;
import io.github.retrooper.packetevents.manager.registry.FabricRegistryManager;
import io.github.retrooper.packetevents.mc261.factory.fabric.Fabric261PlayerManager;
import io.github.retrooper.packetevents.util.LazyHolder;
import org.jetbrains.annotations.Nullable;

public class Fabric261ChainLoadEntrypoint implements ChainLoadEntryPoint {

    private final LazyHolder<AbstractFabricPlayerManager> playerManager =
            LazyHolder.simple(() -> new Fabric261PlayerManager(FabricPacketEventsAPI.getServerAPI()));

    // 26.X's BuiltInRegistries layout differs from yarn and a real Item ↔ ItemType
    // lookup needs its own implementation. Until that lands, register a null-returning
    // stub so FabricPacketEventsAPI.getRegistryManager() doesn't dereference a null
    // LazyHolder — the intermediary chain can't fill this slot because fabric-
    // intermediary's mods are gated <26.
    private final LazyHolder<RegistryManager> registryManager =
            LazyHolder.simple(() -> new FabricRegistryManager(new ItemRegistry() {
                @Override
                public @Nullable ItemType getByName(String name) { return null; }
                @Override
                public @Nullable ItemType getById(int id) { return null; }
            }));

    @Override
    public void initialize(ChainLoadData chainLoadData) {
        chainLoadData.setPlayerManagerIfNull(playerManager);
        chainLoadData.setRegistryManagerIfNull(registryManager);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
