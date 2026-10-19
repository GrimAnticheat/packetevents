package com.github.retrooper.packetevents.test.nbt;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.util.adventure.AdventureNbtUtil;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.TagStringIO;

/** Remapped together with the bridge and Adventure NBT, then invoked in an isolated loader. */
public final class RelocatedNbtProbe {
    public static void run() throws Exception {
        String[] values = {"1b", "2s", "3", "9223372036854775807L", "1.5f", "2.5d", "\"hello\"",
                "[B;1b,2b]", "[I;1,2]", "[L;1L,2L]", "[1,2]", "{}", "{nested:{list:[{x:1},{x:2}]}}"};
        for (String value : values) {
            BinaryTag expected = TagStringIO.tagStringIO().asTag(value);
            NBT encoded = AdventureNbtUtil.fromAdventure(expected);
            if (!expected.equals(AdventureNbtUtil.toAdventure(encoded))) {
                throw new AssertionError("NBT round trip failed: " + value);
            }
        }
    }
}
