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

package com.github.retrooper.packetevents.wrapper.play.server;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.NetworkChunkData;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_7.Chunk_v1_7;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_8.Chunk_v1_8;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.impl.ChunkReader_v1_7;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.impl.ChunkReader_v1_8;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.io.IOException;
import java.util.BitSet;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// Credit to MCProtocolLib for this wrapper
public class WrapperPlayServerChunkDataBulk extends PacketWrapper<WrapperPlayServerChunkDataBulk> {
    private int[] x;
    private int[] z;
    private BaseChunk[][] chunks;
    private byte[][] biomeData;

    public WrapperPlayServerChunkDataBulk(PacketSendEvent event) {
        super(event);
    }

    //TODO Constructor?

    @Override
    public void read() {
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_8)) {
            read_1_8();
        } else {
            read_1_7();
        }
    }

    private void read_1_8() {
        boolean skylight = readBoolean();
        int columns = readVarInt();
        this.x = new int[columns];
        this.z = new int[columns];
        this.chunks = new BaseChunk[columns][];
        this.biomeData = new byte[columns][];
        int[] masks = new int[columns];
        int[] payloadLengths = new int[columns];
        for (int column = 0; column < columns; column++) {
            this.x[column] = readInt();
            this.z[column] = readInt();
            int mask = readUnsignedShort();
            int chunks = Integer.bitCount(mask);
            masks[column] = mask;
            payloadLengths[column] = (chunks * ((4096 * 2) + 2048)) + (skylight ? chunks * 2048 : 0);
        }
        for (int column = 0; column < columns; column++) {
            BitSet mask = BitSet.valueOf(new long[]{masks[column]});
            // pass wrapper through at the position where the data should be located
            BaseChunk[] chunkData = new ChunkReader_v1_8().read(this.user.getDimensionType(), mask,
                    null, true, false, skylight,
                    16, payloadLengths[column] + 256, this);
            this.chunks[column] = chunkData;
            this.biomeData[column] = this.readBytes(16 * 16);
        }
    }

    private void read_1_7() {
        // Read packet base data.
        short columns = readShort();
        int deflatedLength = readInt();
        boolean skylight = readBoolean();
        byte[] deflatedBytes = readBytes(deflatedLength);

        this.x = new int[columns];
        this.z = new int[columns];
        this.chunks = new BaseChunk[columns][];
        this.biomeData = new byte[columns][];
        BitSet[] chunkMasks = new BitSet[columns];
        BitSet[] extendedChunkMasks = new BitSet[columns];
        int[] payloadLengths = new int[columns];
        int inflatedLength = 0;

        for (int count = 0; count < columns; count++) {
            this.x[count] = readInt();
            this.z[count] = readInt();
            chunkMasks[count] = BitSet.valueOf(new long[]{readUnsignedShort()});
            extendedChunkMasks[count] = BitSet.valueOf(new long[]{readUnsignedShort()});
            payloadLengths[count] = ChunkReader_v1_7.getDataLength(chunkMasks[count], extendedChunkMasks[count], true, skylight);
            inflatedLength += payloadLengths[count];
        }

        byte[] inflated = new byte[inflatedLength];
        Inflater inflater = new Inflater();
        inflater.setInput(deflatedBytes, 0, deflatedLength);
        try {
            inflater.inflate(inflated);
        } catch (DataFormatException e) {
            new IOException("Bad compressed data format").printStackTrace();
            return;
        } finally {
            inflater.end();
        }

        Object originalBuffer = this.buffer;
        Object inflatedBuf = UnpooledByteBufAllocationHelper.wrappedBuffer(inflated);
        this.buffer = inflatedBuf;

        try {
            for (int count = 0; count < columns; count++) {
                BaseChunk[] chunkData = new ChunkReader_v1_7().read(this.user.getDimensionType(), chunkMasks[count],
                        extendedChunkMasks[count], true, false, skylight,
                        16, payloadLengths[count], this);
                byte[] biomeDataBytes = this.readBytes(16 * 16);

                this.chunks[count] = chunkData;
                this.biomeData[count] = biomeDataBytes;
            }
        } finally {
            this.buffer = originalBuffer;
            ByteBufHelper.release(inflatedBuf);
        }
    }

    @Override
    public void write() {
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_8)) {
            write_1_8();
        } else {
            write_1_7();
        }
    }

    @Override
    public void copy(WrapperPlayServerChunkDataBulk wrapper) {
        this.x = wrapper.x;
        this.z = wrapper.z;
        this.chunks = wrapper.chunks;
        this.biomeData = wrapper.biomeData;
    }

    private void write_1_8() {
        boolean skylight = false;
        NetworkChunkData[] dataInfo = new NetworkChunkData[this.chunks.length];
        for (int column = 0; column < this.chunks.length; column++) {
            Chunk_v1_8[] columnChunks = (Chunk_v1_8[]) this.chunks[column];
            dataInfo[column] = ChunkReader_v1_8.prepareChunkData(columnChunks, this.biomeData[column]);
            if (dataInfo[column].hasSkyLight()) {
                skylight = true;
            }
        }

        writeBoolean(skylight);
        writeVarInt(this.chunks.length);
        for (int column = 0; column < this.x.length; column++) {
            writeInt(this.x[column]);
            writeInt(this.z[column]);
            writeShort(dataInfo[column].getMask());
        }

        for (int column = 0; column < this.x.length; column++) {
            ChunkReader_v1_8.writePayload(this, (Chunk_v1_8[]) this.chunks[column], this.biomeData[column], dataInfo[column]);
        }
    }

    private void write_1_7() {
        // Prepare chunk data arrays.
        int[] chunkMask = new int[this.chunks.length];
        int[] extendedChunkMask = new int[this.chunks.length];
        NetworkChunkData[] dataInfo = new NetworkChunkData[this.chunks.length];
        boolean skylight = false;
        int length = 0;

        for (int count = 0; count < this.chunks.length; ++count) {
            BaseChunk[] column = this.chunks[count];
            dataInfo[count] = ChunkReader_v1_7.prepareChunkData((Chunk_v1_7[]) column, this.biomeData[count]);
            if (dataInfo[count].hasSkyLight()) {
                skylight = true;
            }
            length += dataInfo[count].getDataLength();
            chunkMask[count] = dataInfo[count].getMask();
            extendedChunkMask[count] = dataInfo[count].getExtendedChunkMask();
        }

        byte[] bytes = new byte[length];
        int pos = 0;
        for (int count = 0; count < this.chunks.length; ++count) {
            ChunkReader_v1_7.writePayload(bytes, pos, (Chunk_v1_7[]) this.chunks[count], this.biomeData[count], dataInfo[count]);
            pos += dataInfo[count].getDataLength();
        }

        // Deflate chunk data.
        Deflater deflater = new Deflater(-1);
        byte[] deflatedData = new byte[pos];
        int deflatedLength = pos;
        try {
            deflater.setInput(bytes, 0, pos);
            deflater.finish();
            deflatedLength = deflater.deflate(deflatedData);
        } finally {
            deflater.end();
        }

        // Write data to the network.
        writeShort(this.chunks.length);
        writeInt(deflatedLength);
        writeBoolean(skylight);
        ByteBufHelper.writeBytes(this.buffer, deflatedData, 0, deflatedLength);

        for (int count = 0; count < this.chunks.length; ++count) {
            writeInt(this.x[count]);
            writeInt(this.z[count]);
            writeShort((short) (chunkMask[count] & 65535));
            writeShort((short) (extendedChunkMask[count] & 65535));
        }
    }

    public int[] getX() {
        return x;
    }

    public int[] getZ() {
        return z;
    }

    public BaseChunk[][] getChunks() {
        return chunks;
    }

    public byte[][] getBiomeData() {
        return biomeData;
    }
}
