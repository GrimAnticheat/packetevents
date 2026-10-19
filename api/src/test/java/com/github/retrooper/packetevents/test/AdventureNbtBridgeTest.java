package com.github.retrooper.packetevents.test;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.netty.NettyManager;
import io.github.retrooper.packetevents.impl.netty.NettyManagerImpl;
import com.github.retrooper.packetevents.util.adventure.AdventureNbtUtil;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdventureNbtBridgeTest {
    @BeforeEach
    void setup() {
        PacketEvents.setAPI(new PacketEventsAPI<Object>() {
            private final NettyManager netty = new NettyManagerImpl();
            public boolean isLoaded() { return true; }
            public void init() { }
            public boolean isInitialized() { return true; }
            public boolean isTerminated() { return false; }
            public Object getPlugin() { return null; }
            public ServerManager getServerManager() { return null; }
            public ProtocolManager getProtocolManager() { return null; }
            public PlayerManager getPlayerManager() { return null; }
            public NettyManager getNettyManager() { return netty; }
            public ChannelInjector getInjector() { return null; }
        });
    }

    @AfterEach
    void cleanup() {
        PacketEvents.setAPI(null);
    }
    @Test
    void roundTripsEveryTagType() throws Exception {
        String[] values = {"1b", "2s", "3", "9223372036854775807L", "1.5f", "2.5d", "\"hello\"",
                "[B;1b,2b]", "[I;1,2]", "[L;1L,2L]", "[1,2]", "{}", "{nested:{list:[{x:1},{x:2}]}}"};
        for (String value : values) {
            BinaryTag expected = TagStringIO.tagStringIO().asTag(value);
            NBT encoded = AdventureNbtUtil.fromAdventure(expected);
            assertEquals(expected, AdventureNbtUtil.toAdventure(encoded), value);
        }
    }
}
