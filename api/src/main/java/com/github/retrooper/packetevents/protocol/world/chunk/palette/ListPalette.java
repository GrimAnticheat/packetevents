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

/*
 * This class was taken from MCProtocolLib.
 *
 * https://github.com/Steveice10/MCProtocolLib
 */

package com.github.retrooper.packetevents.protocol.world.chunk.palette;

import com.github.retrooper.packetevents.protocol.stream.NetStreamInput;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.Arrays;

/**
 * A palette backed by a List.
 */
//TODO Equals & hashcode
public class ListPalette implements Palette {

    private final int bits;
    private static final int INDEX_THRESHOLD = 32;
    private final int[] data;
    private int nextId;
    private long[] stateIndex;
    private int stateIndexMask;

    public ListPalette(int bitsPerEntry) {
        this.bits = bitsPerEntry;
        this.data = new int[1 << bitsPerEntry];
        this.nextId = 0;
    }

    @Deprecated
    public ListPalette(int bitsPerEntry, NetStreamInput in) {
        this(bitsPerEntry);

        int paletteLength = in.readVarInt();
        for (int i = 0; i < paletteLength; i++) {
            this.data[i] = in.readVarInt();
        }
        this.nextId = paletteLength;
    }

    public ListPalette(int bitsPerEntry, PacketWrapper<?> wrapper) {
        this(bitsPerEntry);

        int paletteLength = wrapper.readVarInt();
        for (int i = 0; i < paletteLength; i++) {
            this.data[i] = wrapper.readVarInt();
        }
        this.nextId = paletteLength;
    }

    public ListPalette(int bitsPerEntry, int[] data) {
        this.bits = bitsPerEntry;
        final int expectedSize = (1 << this.bits);
        if (data.length > expectedSize) {
            throw new IllegalArgumentException("Data length exceeds the max size the bits can hold");
        } else {
            this.data = Arrays.copyOf(data, expectedSize);
        }
        this.nextId = data.length;
    }

    @Override
    public int size() {
        return this.nextId;
    }

    @Override
    public int stateToId(int state) {
        int id = findIndexed(state);
        if (id < 0) {
            for (int i = 0; i < this.nextId; i++) {
                if (this.data[i] == state) {
                    id = i;
                    break;
                }
            }
        }
        if (id == -1 && this.size() < this.data.length) {
            id = this.nextId++;
            this.data[id] = state;
            if (this.stateIndex != null) {
                indexState(state, id);
            } else if (this.nextId >= INDEX_THRESHOLD) {
                buildStateIndex();
            }
        }

        return id;
    }

    private int findIndexed(int state) {
        long[] index = this.stateIndex;
        if (index == null) return -1;
        int slot = mix(state) & this.stateIndexMask;
        while (true) {
            long entry = index[slot];
            if ((int) entry == 0) return -1;
            if ((int) (entry >> 32) == state) return (int) entry - 1;
            slot = (slot + 1) & this.stateIndexMask;
        }
    }

    private void buildStateIndex() {
        int capacity = 1;
        while (capacity < this.nextId << 1) capacity <<= 1;
        this.stateIndex = new long[capacity];
        this.stateIndexMask = capacity - 1;
        for (int id = 0; id < this.nextId; id++) indexState(this.data[id], id);
    }

    private void indexState(int state, int id) {
        int slot = mix(state) & this.stateIndexMask;
        while (true) {
            long entry = this.stateIndex[slot];
            if ((int) entry == 0) {
                this.stateIndex[slot] = ((long) state << 32) | (id + 1L);
                return;
            }
            if ((int) (entry >> 32) == state) return;
            slot = (slot + 1) & this.stateIndexMask;
        }
    }

    private static int mix(int value) {
        value *= 0x9E3779B9;
        return value ^ (value >>> 16);
    }

    @Override
    public int idToState(int id) {
        return this.data[id];
    }

    @Override
    public int getBits() {
        return this.bits;
    }
}
