package com.github.retrooper.packetevents.protocol.world.chunk.storage;

public class ZeroBitStorage extends BaseStorage {

    private final int bitsPerEntry;
    private final int size;
    private BitStorage storage;

    public ZeroBitStorage(int bitsPerEntry, int size) {
        this.bitsPerEntry = bitsPerEntry;
        this.size = size;
    }

    @Override
    public long[] getData() {
        return this.materialize().getData();
    }

    @Override
    public int getBitsPerEntry() {
        return this.bitsPerEntry;
    }

    @Override
    int getSize() {
        return this.size;
    }

    @Override
    public int get(int index) {
        if (this.storage != null) {
            return this.storage.get(index);
        }
        this.checkIndex(index);
        return 0;
    }

    @Override
    public void set(int index, int value) {
        if (value == 0 && this.storage == null) {
            this.checkIndex(index);
            return;
        }
        this.materialize().set(index, value);
    }

    public boolean isMaterialized() {
        return this.storage != null;
    }

    private BitStorage materialize() {
        if (this.storage == null) {
            this.storage = new BitStorage(this.bitsPerEntry, this.size);
        }
        return this.storage;
    }

    private void checkIndex(int index) {
        if (index < 0 || index > this.size - 1L) {
            throw new IllegalStateException("Illegal index: " + index + " < 0 || " + index + " > " + this.size + " - 1");
        }
    }
}
