/*
 * MIT License
 *
 * Copyright (c) 2020 retrooper
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.retrooper.packetevents.event.manager;

import io.github.retrooper.packetevents.event.PacketEvent;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.eventtypes.CancellableEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EventManagerDynamic {
    /**
     * Map storing all dynamic packet event listeners.
     * The key is the dynamic packet listener event priority, the value is a list of the dynamic packet listeners having that priority.
     * Priorities count for the whole listener, not just one event method in the listener.
     */
    private final Map<Byte, List<PacketListenerDynamic>> map = new HashMap<>();

    /**
     * Call the PacketEvent.
     * This method processes the event on all the registered dynamic packet event listeners.
     * The {@link PacketEventPriority#LOWEST} prioritized listeners will be processing first,
     * the {@link PacketEventPriority#MONITOR} will be processing last and can
     * be the final decider whether the event has been cancelled or not.
     * This call event also calls the legacy event manager call event.
     * @see EventManagerLegacy#callEvent(PacketEvent, byte)
     * @param event {@link PacketEvent}
     */
    public void callEvent(final PacketEvent event) {
        boolean isCancelled = false;
        if (event instanceof CancellableEvent) {
            isCancelled = ((CancellableEvent) event).isCancelled();
        }
        byte maxReachedEventPriority = PacketEventPriority.LOWEST.getPriorityValue();
        //LOWEST.getPriorityValue()
        for (byte i = maxReachedEventPriority; i <= PacketEventPriority.MONITOR.getPriorityValue(); i++) {
            List<PacketListenerDynamic> cached = map.get(i);
            if (cached != null) {
                maxReachedEventPriority = i;
                for (PacketListenerDynamic listener : cached) {
                    try {
                        event.callPacketEvent(listener);
                        event.call(listener);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (event instanceof CancellableEvent) {
                        CancellableEvent ce = (CancellableEvent) event;
                        isCancelled = ce.isCancelled();
                    }
                }
            }
        }
        if (event instanceof CancellableEvent) {
            CancellableEvent ce = (CancellableEvent) event;
            ce.setCancelled(isCancelled);
        }

        PEEventManager.EVENT_MANAGER_LEGACY.callEvent(event, maxReachedEventPriority);
    }

    /**
     * Register the dynamic packet event listener.
     * @param listener {@link PacketListenerDynamic}
     */
    public void registerListener(PacketListenerDynamic listener) {
        List<PacketListenerDynamic> listeners = map.computeIfAbsent(listener.getPriority().getPriorityValue(), k -> new ArrayList<>());
        listeners.add(listener);
    }
    /**
     * Register multiple dynamic packet event listeners with one method.
     * @param listeners {@link PacketListenerDynamic}
     */
    public void registerListeners(PacketListenerDynamic... listeners) {
        for (PacketListenerDynamic listener : listeners) {
            registerListener(listener);
        }
    }
    /**
     * Unregister the dynamic packet event listener.
     * @param listener {@link PacketListenerDynamic}
     */
    public void unregisterListener(PacketListenerDynamic listener) {
        List<PacketListenerDynamic> listeners = map.get(listener.getPriority().getPriorityValue());
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
    }

    /**
     * Unregister multiple dynamic packet event listeners with one method.
     * @param listeners {@link PacketListenerDynamic}
     */
    public void unregisterListeners(PacketListenerDynamic... listeners) {
        for (PacketListenerDynamic listener : listeners) {
            unregisterListener(listener);
        }
    }

    /**
     * Unregister all dynamic packet event listeners.
     */
    public void unregisterAllListeners() {
        map.clear();
    }
}