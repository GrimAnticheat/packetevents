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

package com.github.retrooper.packetevents.netty.buffer;

import com.github.retrooper.packetevents.PacketEvents;
import org.jetbrains.annotations.ApiStatus;

import java.nio.charset.Charset;

public class ByteBufHelper {
    private static volatile ByteBufOperator operator;

    @ApiStatus.Internal
    public static void clearOperatorCache() {
        operator = null;
    }

    private static ByteBufOperator operator() {
        ByteBufOperator current = operator;
        if (current == null) {
            current = PacketEvents.getAPI().getNettyManager().getByteBufOperator();
            operator = current;
        }
        return current;
    }

    public static int capacity(Object buffer) {
        return operator().capacity(buffer);
    }

    public static Object capacity(Object buffer, int capacity) {
        return operator().capacity(buffer, capacity);
    }

    public static int readerIndex(Object buffer) {
        return operator().readerIndex(buffer);
    }

    public static Object readerIndex(Object buffer, int readerIndex) {
        return operator().readerIndex(buffer, readerIndex);
    }

    public static int writerIndex(Object buffer) {
        return operator().writerIndex(buffer);
    }

    public static Object writerIndex(Object buffer, int writerIndex) {
        return operator().writerIndex(buffer, writerIndex);
    }

    public static int readableBytes(Object buffer) {
        return operator().readableBytes(buffer);
    }

    public static int writableBytes(Object buffer) {
        return operator().writableBytes(buffer);
    }

    public static Object clear(Object buffer) {
        return operator().clear(buffer);
    }

    public static String toString(Object buffer, int index, int length, Charset charset) {
        return operator().toString(buffer, index, length, charset);
    }

    public static byte readByte(Object buffer) {
        return operator().readByte(buffer);
    }

    public static void writeByte(Object buffer, int value) {
        operator().writeByte(buffer, value);
    }

    public static boolean readBoolean(Object buffer) {
        return operator().readBoolean(buffer);
    }

    public static void writeBoolean(Object buffer, boolean value) {
        operator().writeBoolean(buffer, value);
    }

    public static short readUnsignedByte(Object buffer) {
        return operator().readUnsignedByte(buffer);
    }

    public static char readChar(Object buffer) {
        return operator().readChar(buffer);
    }

    public static void writeChar(Object buffer, int value) {
        operator().writeChar(buffer, value);
    }

    public static short readShort(Object buffer) {
        return operator().readShort(buffer);
    }

    public static int readUnsignedShort(Object buffer) {
        return operator().readUnsignedShort(buffer);
    }

    public static void writeShort(Object buffer, int value) {
        operator().writeShort(buffer, value);
    }

    public static void writeShortLE(Object buffer, int value) {
        operator().writeShortLE(buffer, value);
    }

    public static int readMedium(Object buffer) {
        return operator().readMedium(buffer);
    }

    public static void writeMedium(Object buffer, int value) {
        operator().writeMedium(buffer, value);
    }

    public static int readInt(Object buffer) {
        return operator().readInt(buffer);
    }

    public static void writeInt(Object buffer, int value) {
        operator().writeInt(buffer, value);
    }

    public static long readUnsignedInt(Object buffer) {
        return operator().readUnsignedInt(buffer);
    }

    public static long readLong(Object buffer) {
        return operator().readLong(buffer);
    }

    public static void writeLong(Object buffer, long value) {
        operator().writeLong(buffer, value);
    }

    public static float readFloat(Object buffer) {
        return operator().readFloat(buffer);
    }

    public static void writeFloat(Object buffer, float value) {
        operator().writeFloat(buffer, value);
    }

    public static double readDouble(Object buffer) {
        return operator().readDouble(buffer);
    }

    public static void writeDouble(Object buffer, double value) {
        operator().writeDouble(buffer, value);
    }

    public static Object getBytes(Object buffer, int index, byte[] destination) {
        return operator().getBytes(buffer, index, destination);
    }

    public static short getUnsignedByte(Object buffer, int index) {
        return operator().getUnsignedByte(buffer, index);
    }

    public static boolean isReadable(Object buffer) {
        return operator().isReadable(buffer);
    }

    public static Object copy(Object buffer) {
        return operator().copy(buffer);
    }

    public static Object duplicate(Object buffer) {
        return operator().duplicate(buffer);
    }

    public static boolean hasArray(Object buffer) {
        return operator().hasArray(buffer);
    }

    public static byte[] array(Object buffer) {
        return operator().array(buffer);
    }

    public static Object retain(Object buffer) {
        return operator().retain(buffer);
    }

    public static Object retainedDuplicate(Object buffer) {
        return operator().retainedDuplicate(buffer);
    }

    public static Object readSlice(Object buffer, int length) {
        return operator().readSlice(buffer, length);
    }

    public static Object readBytes(Object buffer, byte[] destination, int destinationIndex, int length) {
        return operator().readBytes(buffer, destination, destinationIndex, length);
    }

    public static Object readBytes(Object buffer, int length) {
        return operator().readBytes(buffer, length);
    }

    public static Object writeBytes(Object buffer, Object src) {
        return operator().writeBytes(buffer, src);
    }

    public static void readBytes(Object buffer, byte[] bytes) {
        operator().readBytes(buffer, bytes);
    }

    public static void writeBytes(Object buffer, byte[] bytes) {
        operator().writeBytes(buffer, bytes);
    }

    public static void writeBytes(Object buffer, byte[] bytes, int offset, int length) {
        operator().writeBytes(buffer, bytes, offset, length);
    }

    public static boolean release(Object buffer) {
        return operator().release(buffer);
    }

    public static int refCnt(Object buffer) {
        return operator().refCnt(buffer);
    }

    public static Object skipBytes(Object buffer, int length) {
        return operator().skipBytes(buffer, length);
    }

    public static Object markReaderIndex(Object buffer) {
        return operator().markReaderIndex(buffer);
    }

    public static Object resetReaderIndex(Object buffer) {
        return operator().resetReaderIndex(buffer);
    }

    public static Object markWriterIndex(Object buffer) {
        return operator().markWriterIndex(buffer);
    }

    public static Object resetWriterIndex(Object buffer) {
        return operator().resetWriterIndex(buffer);
    }

    public static Object allocateNewBuffer(Object buffer) {
        return operator().allocateNewBuffer(buffer);
    }

    public static int getByteSize(int value) {
        for (int i = 1; i < 5; ++i) {
            if ((value & -1 << i * 7) == 0) {
                return i;
            }
        }
        return 5;
    }

    public static int readVarInt(Object buffer) {
        int value = 0;
        int length = 0;
        byte currentByte;
        do {
            currentByte = readByte(buffer);
            value |= (currentByte & 0x7F) << (length * 7);
            length++;
            if (length > 5) {
                throw new RuntimeException("VarInt is too large. Must be smaller than 5 bytes.");
            }
        } while ((currentByte & 0x80) == 0x80);
        return value;
    }

    public static void writeVarInt(Object buffer, int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                writeByte(buffer, value);
                return;
            }
            writeByte(buffer, (value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    public static byte[] copyBytes(Object buffer) {
        byte[] bytes = new byte[readableBytes(buffer)];
        getBytes(buffer, readerIndex(buffer), bytes);
        return bytes;
    }
}
