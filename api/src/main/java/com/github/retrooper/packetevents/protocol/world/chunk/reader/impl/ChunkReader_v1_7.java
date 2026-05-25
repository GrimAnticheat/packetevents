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
import com.github.retrooper.packetevents.protocol.world.chunk.ByteArray3d;
import com.github.retrooper.packetevents.protocol.world.chunk.NetworkChunkData;
import com.github.retrooper.packetevents.protocol.world.chunk.NibbleArray3d;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_7.Chunk_v1_7;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.ChunkReader;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.BitSet;
import java.util.zip.Deflater;

public class ChunkReader_v1_7 implements ChunkReader {

    private static final int SECTION_COUNT = 16;
    private static final int BLOCK_BYTES = 4096;
    private static final int NIBBLE_BYTES = 2048;
    private static final int BIOME_BYTES = 256;

    @Override
    public BaseChunk[] read(
            DimensionType dimensionType, BitSet chunkMask, BitSet secondaryChunkMask, boolean fullChunk,
            boolean hasBlockLight, boolean hasSkyLight, int chunkSize, int arrayLength, PacketWrapper<?> wrapper
    ) {
        int expectedWithoutSky = getDataLength(chunkMask, secondaryChunkMask, fullChunk, false);
        boolean sky = dataLengthHasSkyLight(arrayLength, expectedWithoutSky, hasSkyLight);

        return readPayload(chunkMask, secondaryChunkMask, sky, wrapper);
    }

    public static BaseChunk[] readPayload(BitSet chunkMask, BitSet secondaryChunkMask, boolean hasSkyLight, PacketWrapper<?> wrapper) {
        Chunk_v1_7[] chunks = new Chunk_v1_7[SECTION_COUNT];
        // Fun fact, a mojang dev (forgot who) wanted to do the flattening in 1.8
        // So the extended block data was likely how mojang wanted to get around the 255 block id limit
        // Before they decided to quite using magic values and instead went with the new 1.13 solution
        //
        // That's probably why extended block data exists, although yeah it was never used.
        for (int ind = 0; ind < SECTION_COUNT; ind++) {
            if (chunkMask.get(ind)) {
                chunks[ind] = new Chunk_v1_7(hasSkyLight, secondaryChunkMask.get(ind));
                ByteArray3d blocks = chunks[ind].getBlocks();
                ByteBufHelper.readBytes(wrapper.buffer, blocks.getData());
            }
        }

        for (int ind = 0; ind < SECTION_COUNT; ind++) {
            if (chunkMask.get(ind)) {
                NibbleArray3d metadata = chunks[ind].getMetadata();
                ByteBufHelper.readBytes(wrapper.buffer, metadata.getData());
            }
        }

        for (int ind = 0; ind < SECTION_COUNT; ind++) {
            if (chunkMask.get(ind)) {
                NibbleArray3d blocklight = chunks[ind].getBlockLight();
                ByteBufHelper.readBytes(wrapper.buffer, blocklight.getData());
            }
        }

        if (hasSkyLight) {
            for (int ind = 0; ind < SECTION_COUNT; ind++) {
                if (chunkMask.get(ind)) {
                    NibbleArray3d skylight = chunks[ind].getSkyLight();
                    ByteBufHelper.readBytes(wrapper.buffer, skylight.getData());
                }
            }
        }

        for (int ind = 0; ind < SECTION_COUNT; ind++) {
            if (secondaryChunkMask.get(ind)) {
                NibbleArray3d extended = chunks[ind].getExtendedBlocks();
                ByteBufHelper.readBytes(wrapper.buffer, extended.getData());
            }
        }

        return chunks;
    }

    private static boolean dataLengthHasSkyLight(int dataLength, int expectedWithoutSky, boolean hasSkyLight) {
        // If we have more data than blocks, metadata, blocklight, extended data and optional biomes,
        // there must be skylight data as well.
        return hasSkyLight && dataLength > expectedWithoutSky;
    }

    public static void writeColumn(PacketWrapper<?> wrapper, Chunk_v1_7[] chunks, byte[] biomes) {
        NetworkChunkData data = prepareChunkData(chunks, biomes);
        byte[] payload = new byte[data.getDataLength()];
        writePayload(payload, 0, chunks, biomes, data);

        Deflater deflater = new Deflater(-1);

        byte[] deflated = new byte[data.getDataLength()];
        int len;
        try {
            deflater.setInput(payload, 0, data.getDataLength());
            deflater.finish();
            len = deflater.deflate(deflated);
        } finally {
            deflater.end();
        }

        wrapper.writeShort(data.getMask());
        wrapper.writeShort(data.getExtendedChunkMask());
        wrapper.writeInt(len);
        ByteBufHelper.writeBytes(wrapper.buffer, deflated, 0, len);
    }

    public static void writePayload(byte[] target, int offset, Chunk_v1_7[] chunks, byte[] biomes, NetworkChunkData info) {
        int pos = offset;
        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind)) {
                byte[] blocks = chunks[ind].getBlocks().getData();
                System.arraycopy(blocks, 0, target, pos, blocks.length);
                pos += blocks.length;
            }
        }

        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind)) {
                byte[] metadata = chunks[ind].getMetadata().getData();
                System.arraycopy(metadata, 0, target, pos, metadata.length);
                pos += metadata.length;
            }
        }

        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind)) {
                byte[] blocklight = chunks[ind].getBlockLight().getData();
                System.arraycopy(blocklight, 0, target, pos, blocklight.length);
                pos += blocklight.length;
            }
        }

        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind) && chunks[ind].getSkyLight() != null) {
                byte[] skylight = chunks[ind].getSkyLight().getData();
                System.arraycopy(skylight, 0, target, pos, skylight.length);
                pos += skylight.length;
            }
        }

        for (int ind = 0; ind < chunks.length; ind++) {
            if (info.isIncluded(ind) && chunks[ind].getExtendedBlocks() != null) {
                byte[] extended = chunks[ind].getExtendedBlocks().getData();
                System.arraycopy(extended, 0, target, pos, extended.length);
                pos += extended.length;
            }
        }

        if (info.isFullChunk()) {
            System.arraycopy(biomes, 0, target, pos, biomes.length);
        }
    }

    public static NetworkChunkData prepareChunkData(Chunk_v1_7[] chunks, byte[] biomes) {
        boolean fullChunk = biomes != null;
        int chunkMask = 0;
        int extendedChunkMask = 0;
        boolean sky = false;
        int length = fullChunk ? BIOME_BYTES : 0;
        for (int ind = 0; ind < chunks.length; ++ind) {
            Chunk_v1_7 chunk = chunks[ind];
            if (chunk != null && (!fullChunk || !chunk.isEmpty())) {
                chunkMask |= 1 << ind;
                length += BLOCK_BYTES + NIBBLE_BYTES + NIBBLE_BYTES;
                if (chunk.getSkyLight() != null) {
                    length += NIBBLE_BYTES;
                    sky = true;
                }
                if (chunk.getExtendedBlocks() != null) {
                    extendedChunkMask |= 1 << ind;
                    length += NIBBLE_BYTES;
                }
            }
        }
        return new NetworkChunkData(chunkMask, extendedChunkMask, fullChunk, sky, length);
    }

    public static int getDataLength(BitSet chunkMask, BitSet extendedChunkMask, boolean fullChunk, boolean skyLight) {
        int chunks = chunkMask.cardinality();
        int extended = extendedChunkMask.cardinality();
        int length = chunks * (BLOCK_BYTES + NIBBLE_BYTES + NIBBLE_BYTES) + extended * NIBBLE_BYTES;
        if (skyLight) {
            length += chunks * NIBBLE_BYTES;
        }
        if (fullChunk) {
            length += BIOME_BYTES;
        }
        return length;
    }

}
