/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.retrooper.packetevents;

import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// fabric-official's preLaunch is intentionally a thin scaffold. It discovers and runs
// peMainChainLoad / peClientChainLoad entrypoints contributed by mcXXX subprojects so
// the chain-loader contract is honoured on 26.X. Setting up FabricPacketEventsAPI is
// deferred to a follow-up that lands per-version PlayerManager / injector implementations
// compiled against Mojang-named 26.X sources.
public class PacketEventsMod implements PreLaunchEntrypoint, ModInitializer {

    public static final String MOD_ID = "packetevents";
    public static PacketEventsMod INSTANCE;

    private static final Logger LOGGER = Logger.getLogger("packetevents-fabric-official");

    @Override
    public void onPreLaunch() {
        INSTANCE = this;
        FabricLoader loader = FabricLoader.getInstance();

        List<ChainLoadEntryPoint> mainChainLoadEntryPoints =
                loader.getEntrypoints("peMainChainLoad", ChainLoadEntryPoint.class);
        mainChainLoadEntryPoints.sort((a, b) ->
                b.getNativeVersion().getProtocolVersion() - a.getNativeVersion().getProtocolVersion());

        List<ChainLoadEntryPoint> allEntryPoints;
        switch (loader.getEnvironmentType()) {
            case CLIENT -> {
                List<ChainLoadEntryPoint> clientChainLoadEntryPoints =
                        loader.getEntrypoints("peClientChainLoad", ChainLoadEntryPoint.class);
                clientChainLoadEntryPoints.sort((a, b) ->
                        b.getNativeVersion().getProtocolVersion() - a.getNativeVersion().getProtocolVersion());
                clientChainLoadEntryPoints.addAll(mainChainLoadEntryPoints);
                allEntryPoints = clientChainLoadEntryPoints;
            }
            case SERVER -> allEntryPoints = mainChainLoadEntryPoints;
            default -> throw new IllegalStateException("Unexpected value: " + loader.getEnvironmentType());
        }

        ChainLoadData chainLoadData = new ChainLoadData();
        for (ChainLoadEntryPoint entryPoint : allEntryPoints) {
            try {
                entryPoint.initialize(chainLoadData);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Error initializing chain entrypoint for version " + entryPoint.getNativeVersion(), e);
            }
        }

        LOGGER.info("PacketEvents (fabric-official) preLaunch complete on "
                + loader.getEnvironmentType() + "; chain entrypoints invoked: " + allEntryPoints.size()
                + ". FabricPacketEventsAPI setup is deferred until 26.X Mojang-named sources land.");
    }

    @Override
    public void onInitialize() {
        // No-op: FabricPacketEventsAPI is not yet instantiated on the 26.X branch.
    }
}
