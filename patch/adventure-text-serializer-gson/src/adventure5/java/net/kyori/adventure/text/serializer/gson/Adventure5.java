/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
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
package net.kyori.adventure.text.serializer.gson;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.NBTComponent;
import net.kyori.adventure.text.ObjectComponent;

/** Direct references let shading and obfuscation remap the optional Adventure 5 methods. */
public final class Adventure5 {
    private static final boolean AVAILABLE;

    static {
        boolean available;
        try {
            Component.entityNBT().plain(false);
            available = true;
        } catch (NoSuchMethodError ignored) {
            available = false;
        }
        AVAILABLE = available;
    }

    private Adventure5() {
    }

    public static boolean plain(Component component) {
        return AVAILABLE && ((NBTComponent<?>) component).plain();
    }

    public static Component plain(Component component, boolean plain) {
        return AVAILABLE ? ((NBTComponent<?>) component).plain(plain) : component;
    }

    public static Component fallback(Component component) {
        return AVAILABLE ? ((ObjectComponent) component).fallback() : null;
    }

    public static Component fallback(Component component, Component fallback) {
        return AVAILABLE ? ((ObjectComponent) component).fallback(fallback) : component;
    }
}
