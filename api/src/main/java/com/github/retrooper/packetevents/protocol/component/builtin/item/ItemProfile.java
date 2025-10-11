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

package com.github.retrooper.packetevents.protocol.component.builtin.item;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.PlayerModelType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ItemProfile {

    private @Nullable String name;
    private @Nullable UUID id;
    private List<Property> properties;
    /**
     * @versions 1.21.9+
     */
    private SkinPatch skinPatch;

    public ItemProfile(
            @Nullable String name,
            @Nullable UUID id,
            List<Property> properties
    ) {
        this(name, id, properties, SkinPatch.EMPTY);
    }

    public ItemProfile(
            @Nullable String name,
            @Nullable UUID id,
            List<Property> properties,
            SkinPatch skinPatch
    ) {
        this.name = name;
        this.id = id;
        this.properties = properties;
        this.skinPatch = skinPatch;
    }

    public static ItemProfile read(PacketWrapper<?> wrapper) {
        String name;
        UUID id;
        boolean partial = wrapper.getServerVersion().isOlderThan(ServerVersion.V_1_21_9) || !wrapper.readBoolean();
        if (!partial) {
            id = wrapper.readUUID();
            name = wrapper.readString(16);
        } else {
            name = wrapper.readOptional(ew -> ew.readString(16));
            id = wrapper.readOptional(PacketWrapper::readUUID);
        }
        List<Property> properties = wrapper.readList(Property::read);
        SkinPatch skinPatch = wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_21_9)
                ? SkinPatch.read(wrapper) : SkinPatch.EMPTY;
        return new ItemProfile(name, id, properties, skinPatch);
    }

    public static void write(PacketWrapper<?> wrapper, ItemProfile profile) {
        boolean partial;
        if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_21_9)) {
            partial = profile.name == null || profile.id == null;
            wrapper.writeBoolean(!partial);
        } else {
            // always partial profile (which is way simpler, why did they change this?)
            partial = true;
        }
        if (!partial) {
            wrapper.writeUUID(profile.id);
            wrapper.writeString(profile.name, 16);
        } else {
            wrapper.writeOptional(profile.name, (ew, name) -> ew.writeString(name, 16));
            wrapper.writeOptional(profile.id, PacketWrapper::writeUUID);
        }
        wrapper.writeList(profile.properties, Property::write);
        if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_21_9)) {
            SkinPatch.write(wrapper, profile.skinPatch);
        }
    }

    public @Nullable String getName() {
        return this.name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable UUID getId() {
        return this.id;
    }

    public void setId(@Nullable UUID id) {
        this.id = id;
    }

    public void addProperty(Property property) {
        this.properties.add(property);
    }

    public List<Property> getProperties() {
        return this.properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    /**
     * @versions 1.21.9+
     */
    public SkinPatch getSkinPatch() {
        return this.skinPatch;
    }

    /**
     * @versions 1.21.9+
     */
    public void setSkinPatch(SkinPatch skinPatch) {
        this.skinPatch = skinPatch;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemProfile)) return false;
        ItemProfile that = (ItemProfile) obj;
        if (!Objects.equals(this.name, that.name)) return false;
        if (!Objects.equals(this.id, that.id)) return false;
        if (!this.properties.equals(that.properties)) return false;
        return this.skinPatch.equals(that.skinPatch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.id, this.properties, this.skinPatch);
    }

    public static class Property {

        private String name;
        private String value;
        private @Nullable String signature;

        public Property(String name, String value, @Nullable String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }

        public static Property read(PacketWrapper<?> wrapper) {
            String name = wrapper.readString(64);
            String value = wrapper.readString(32767);
            String signature = wrapper.readOptional(ew -> ew.readString(1024));
            return new Property(name, value, signature);
        }

        public static void write(PacketWrapper<?> wrapper, Property property) {
            wrapper.writeString(property.name, 64);
            wrapper.writeString(property.value, 32767);
            wrapper.writeOptional(property.signature,
                    (ew, signature) -> ew.writeString(signature, 1024));
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public @Nullable String getSignature() {
            return this.signature;
        }

        public void setSignature(@Nullable String signature) {
            this.signature = signature;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Property)) return false;
            Property property = (Property) obj;
            if (!this.name.equals(property.name)) return false;
            if (!this.value.equals(property.value)) return false;
            return Objects.equals(this.signature, property.signature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.value, this.signature);
        }
    }

    public static class SkinPatch {

        public static final SkinPatch EMPTY = new SkinPatch(null, null, null, null);

        private final @Nullable ResourceLocation body;
        private final @Nullable ResourceLocation cape;
        private final @Nullable ResourceLocation elytra;
        private final @Nullable PlayerModelType model;

        public SkinPatch(
                @Nullable ResourceLocation body,
                @Nullable ResourceLocation cape,
                @Nullable ResourceLocation elytra,
                @Nullable PlayerModelType model
        ) {
            this.body = body;
            this.cape = cape;
            this.elytra = elytra;
            this.model = model;
        }

        public static SkinPatch read(PacketWrapper<?> wrapper) {
            ResourceLocation body = wrapper.readOptional(ResourceLocation::read);
            ResourceLocation cape = wrapper.readOptional(ResourceLocation::read);
            ResourceLocation elytra = wrapper.readOptional(ResourceLocation::read);
            PlayerModelType model = wrapper.readOptional(ew -> ew.readEnum(PlayerModelType.class));
            return new SkinPatch(body, cape, elytra, model);
        }

        public static void write(PacketWrapper<?> wrapper, SkinPatch patch) {
            wrapper.writeOptional(patch.body, ResourceLocation::write);
            wrapper.writeOptional(patch.cape, ResourceLocation::write);
            wrapper.writeOptional(patch.elytra, ResourceLocation::write);
            wrapper.writeOptional(patch.model, PacketWrapper::writeEnum);
        }

        public @Nullable ResourceLocation getBody() {
            return this.body;
        }

        public @Nullable ResourceLocation getCape() {
            return this.cape;
        }

        public @Nullable ResourceLocation getElytra() {
            return this.elytra;
        }

        public @Nullable PlayerModelType getModel() {
            return this.model;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || this.getClass() != obj.getClass()) return false;
            SkinPatch skinPatch = (SkinPatch) obj;
            if (!Objects.equals(this.body, skinPatch.body)) return false;
            if (!Objects.equals(this.cape, skinPatch.cape)) return false;
            if (!Objects.equals(this.elytra, skinPatch.elytra)) return false;
            return this.model == skinPatch.model;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.body, this.cape, this.elytra, this.model);
        }
    }
}
