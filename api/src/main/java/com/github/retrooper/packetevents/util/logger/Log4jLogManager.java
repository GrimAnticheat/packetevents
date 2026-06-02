package com.github.retrooper.packetevents.util.logger;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.util.LogManager;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.util.reflection.Reflection;
import net.kyori.adventure.text.ComponentLike;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.logging.Level;

/**
 * Fallback for older Minecraft/Fabric runtimes which expose Log4j but not SLF4J.
 */
@NullMarked
@ApiStatus.Internal
public final class Log4jLogManager extends LogManager {

    private static final boolean EXISTS = Reflection.getClassByNameWithoutException("org.apache.logging.log4j.Logger") != null
            && Reflection.getClassByNameWithoutException("org.apache.logging.log4j.LogManager") != null;

    private final Logger logger;

    public Log4jLogManager(PacketEventsAPI<?> packetevents) {
        super(packetevents);
        this.logger = org.apache.logging.log4j.LogManager.getLogger(LOGGER_NAME);
    }

    public static boolean exists() {
        return EXISTS;
    }

    @Override
    public void log(Level level, ComponentLike component, @Nullable Throwable error) {
        String message = AdventureSerializer.stringify(component.asComponent());
        if (level == Level.FINEST || level == Level.FINER) {
            this.logger.trace(message, error);
        } else if (level == Level.FINE) {
            this.logger.debug(message, error);
        } else if (level == Level.INFO) {
            this.logger.info(message, error);
        } else if (level == Level.WARNING) {
            this.logger.warn(message, error);
        } else if (level == Level.SEVERE) {
            this.logger.error(message, error);
        } else {
            throw new UnsupportedOperationException(level + " is unsupported (" + component + ")");
        }
    }
}
