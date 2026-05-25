/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.protocol.world.chunk.reader.impl;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.NetworkChunkData;
import com.github.retrooper.packetevents.protocol.world.chunk.NibbleArray3d;
import com.github.retrooper.packetevents.protocol.world.chunk.ShortArray3d;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_8.Chunk_v1_8;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.ChunkReader;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.BitSet;

public class ChunkReader_v1_8 implements ChunkReader {

    private static final int SECTION_COUNT = 16;
    private static final int BLOCKS_PER_SECTION = 4096;
    private static final int BLOCK_BYTES = BLOCKS_PER_SECTION * 2;
    private static final int LIGHT_BYTES = 2048;
    private static final int BIOME_BYTES = 256;

    @Override
    public BaseChunk[] read(
            DimensionType dimensionType, BitSet chunkMask, BitSet secondaryChunkMask, boolean fullChunk,
            boolean hasBlockLight, boolean hasSkyLight, int chunkSize, int arrayLength, PacketWrapper<?> wrapper
    ) {
        Chunk_v1_8[] chunks = new Chunk_v1_8[SECTION_COUNT];
        int chunkCount = 0;
        for (int ind = 0; ind < SECTION_COUNT; ind++) {
            if (chunkMask.get(ind)) {
                chunkCount++;
            }
        }
        int expectedWithoutSky = (BLOCK_BYTES + LIGHT_BYTES) * chunkCount + (fullChunk ? BIOME_BYTES : 0);
        boolean sky = dataLengthHasSkyLight(arrayLength, expectedWithoutSky, hasSkyLight);

        for (int ind = 0; ind < SECTION_COUNT; ind++) {
            if (chunkMask.get(ind)) {
                chunks[ind] = new Chunk_v1_8(sky || hasBlockLight);
                ShortArray3d blocks = chunks[ind].getBlocks();
                int read = ByteBufHelper.readShortsLE(wrapper.buffer, blocks.getData(), 0, blocks.getData().length);
                if (read < blocks.getData().length) {
                    throw new IllegalStateException("Could not read 1.8 chunk block data");
                }
            }
        }

        for (int ind = 0; ind < SECTION_COUNT; ind++) {
            if (chunkMask.get(ind)) {
                NibbleArray3d blocklight = chunks[ind].getBlockLight();
                ByteBufHelper.readBytes(wrapper.buffer, blocklight.getData());
            }
        }

        if (sky || hasBlockLight) {
            for (int ind = 0; ind < SECTION_COUNT; ind++) {
                if (chunkMask.get(ind)) {
                    NibbleArray3d skylight = chunks[ind].getSkyLight();
                    ByteBufHelper.readBytes(wrapper.buffer, skylight.getData());
                }
            }
        }

        return chunks;
    }

    private static boolean dataLengthHasSkyLight(int dataLength, int expectedWithoutSky, boolean hasSkyLight) {
        // If we have more data than blocks, blocklight and optional biomes, there must be skylight data as well.
        return hasSkyLight && dataLength > expectedWithoutSky;
    }

    public static void writeColumn(PacketWrapper<?> wrapper, Chunk_v1_8[] chunks, byte[] biomes) {
        NetworkChunkData info = prepareChunkData(chunks, biomes);
        wrapper.writeShort(info.getMask());
        wrapper.writeVarInt(info.getDataLength());
        writePayload(wrapper, chunks, biomes, info);
    }

    public static void writePayload(PacketWrapper<?> wrapper, Chunk_v1_8[] chunks, byte[] biomes, NetworkChunkData info) {
        int requiredCapacity = ByteBufHelper.writerIndex(wrapper.buffer) + info.getDataLength();
        if (requiredCapacity > ByteBufHelper.capacity(wrapper.buffer)) {
            ByteBufHelper.capacity(wrapper.buffer, requiredCapacity);
        }

        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind)) {
                Chunk_v1_8 chunk = chunks[ind];
                short[] blocks = chunk.getBlocks().getData();
                ByteBufHelper.writeShortsLE(wrapper.buffer, blocks, 0, blocks.length);
            }
        }

        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind)) {
                Chunk_v1_8 chunk = chunks[ind];
                byte[] blocklight = chunk.getBlockLight().getData();
                ByteBufHelper.writeBytes(wrapper.buffer, blocklight);
            }
        }

        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind)) {
                Chunk_v1_8 chunk = chunks[ind];
                if (chunk.getSkyLight() == null) {
                    continue;
                }
                byte[] skylight = chunk.getSkyLight().getData();
                ByteBufHelper.writeBytes(wrapper.buffer, skylight);
            }
        }

        if (info.isFullChunk()) {
            ByteBufHelper.writeBytes(wrapper.buffer, biomes);
        }
    }

    public static NetworkChunkData prepareChunkData(Chunk_v1_8[] chunks, byte[] biomes) {
        boolean fullChunk = biomes != null;
        int mask = 0;
        boolean sky = false;
        int length = fullChunk ? biomes.length : 0;
        for (int ind = 0; ind < chunks.length; ind++) {
            Chunk_v1_8 chunk = chunks[ind];
            if (chunk != null && (!fullChunk || !chunk.isEmpty())) {
                mask |= 1 << ind;
                length += chunk.getBlocks().getData().length * 2;
                length += chunk.getBlockLight().getData().length;
                if (chunk.getSkyLight() != null) {
                    length += chunk.getSkyLight().getData().length;
                    sky = true;
                }
            }
        }
        return new NetworkChunkData(mask, fullChunk, sky, length);
    }

}

