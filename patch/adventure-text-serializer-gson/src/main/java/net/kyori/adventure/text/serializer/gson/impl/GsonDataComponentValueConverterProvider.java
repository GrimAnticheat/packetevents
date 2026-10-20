package net.kyori.adventure.text.serializer.gson.impl;

import com.google.gson.JsonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.DataComponentValueConverterRegistry;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * Service-loader bridge for runtimes which provide Adventure's unrelocated Gson service descriptor.
 * PE's full serializer implementation remains relocated. This class only bridges the optional
 * removed-value conversion and keeps mixed Adventure 4/5 Fabric classpaths loadable.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class GsonDataComponentValueConverterProvider implements DataComponentValueConverterRegistry.Provider {
    private static final Key ID = Key.key("adventure", "gson");

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public Iterable<DataComponentValueConverterRegistry.Conversion<?, ?>> conversions() {
        try {
            Class<Object> removed = (Class<Object>) Class.forName("net.kyori.adventure.text.event.DataComponentValue$Removed");
            Class<Object> gsonValue = (Class<Object>) Class.forName("net.kyori.adventure.text.serializer.gson.GsonDataComponentValue");
            Method factory = gsonValue.getMethod("gsonDataComponentValue", com.google.gson.JsonElement.class);
            java.util.function.BiFunction<Key, Object, Object> converter = (key, value) -> {
                try {
                    return factory.invoke(null, JsonNull.INSTANCE);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Cannot create Gson data component value", exception);
                }
            };
            DataComponentValueConverterRegistry.Conversion conversion =
                    DataComponentValueConverterRegistry.Conversion.convert(removed, gsonValue, converter);
            return Collections.singletonList(conversion);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Adventure 4 runtimes may not expose data component Gson values.
            return Collections.emptyList();
        }
    }
}
