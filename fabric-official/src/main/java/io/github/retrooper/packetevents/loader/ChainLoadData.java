package io.github.retrooper.packetevents.loader;

import com.github.retrooper.packetevents.manager.registry.RegistryManager;
import io.github.retrooper.packetevents.util.LazyHolder;

// fabric-official's ChainLoadData is intentionally a thinner mirror of fabric-intermediary's.
// PlayerManager wiring is omitted because AbstractFabricPlayerManager carries MC type
// references that need 26.X-mapped sources to compile cleanly; restore those slots once
// fabric-official can compile a per-version PlayerManager implementation.
public class ChainLoadData {

    private LazyHolder<RegistryManager> registryManagerLazyHolder = null;

    public void setRegistryManagerIfNull(LazyHolder<RegistryManager> registryManagerLazyHolder) {
        if (this.registryManagerLazyHolder == null) {
            this.registryManagerLazyHolder = registryManagerLazyHolder;
        }
    }

    public LazyHolder<RegistryManager> getRegistryManagerLazyHolder() {
        return this.registryManagerLazyHolder;
    }
}
