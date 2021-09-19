/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
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

package io.github.retrooper.packetevents.processor;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.PacketListener;
import io.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketSendEvent;
import io.github.retrooper.packetevents.manager.player.ClientVersion;
import io.github.retrooper.packetevents.protocol.ConnectionState;
import io.github.retrooper.packetevents.protocol.PacketType;
import io.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import io.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import io.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import io.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class InternalPacketListener implements PacketListener {
    //Make this specific event be at MONITOR priority
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            //Transition into the GAME connection state
            PacketEvents.get().getInjector().changeConnectionState(event.getChannel().rawChannel(), ConnectionState.PLAY);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        switch (event.getConnectionState()) {
            case HANDSHAKING:
                if (event.getPacketType() == PacketType.Handshaking.Client.HANDSHAKE) {
                    WrapperHandshakingClientHandshake handshake = new WrapperHandshakingClientHandshake(event);
                    ClientVersion clientVersion = handshake.getClientVersion();

                    //Update client version for this event call
                    event.setClientVersion(clientVersion);

                    //Map netty channel with the client version.
                    Object rawChannel = event.getChannel().rawChannel();
                    PacketEvents.get().getPlayerManager().clientVersions.put(rawChannel, clientVersion);

                    //Transition into the LOGIN OR STATUS connection state
                    PacketEvents.get().getInjector().changeConnectionState(rawChannel, handshake.getNextConnectionState());
                }
                break;
            case LOGIN:
                if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
                    WrapperLoginClientLoginStart start = new WrapperLoginClientLoginStart(event);
                    //Map the player usernames with their netty channels
                    PacketEvents.get().getPlayerManager().channels.put(start.getUsername(), event.getChannel().rawChannel());
                }
                break;

            case PLAY:
                if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                    WrapperPlayClientClickWindow clickWindow = new WrapperPlayClientClickWindow(event);
                    int id = clickWindow.getWindowID();
                    int button = clickWindow.getButton();
                    WrapperPlayClientClickWindow.WindowClickType windowClickType = clickWindow.getWindowClickType();
                    Map<Integer, ItemStack> slots = clickWindow.getSlots().orElse(new HashMap<>());
                    String msg = "ID: " + id + ", BUTTON: " + button + ", WINDOW CLICK TYPE: " + windowClickType.name();
                    System.out.println(msg);
                    event.getPlayer().sendMessage(msg);
                    for (Integer slot : slots.keySet()) {
                        ItemStack stack = slots.get(slot);
                        String mapMsg = "We've got a " + stack.getType().name() + " at slot " + slot;
                        System.out.println(mapMsg);
                        event.getPlayer().sendMessage(mapMsg);
                    }
                }
                break;
        }
    }
}
