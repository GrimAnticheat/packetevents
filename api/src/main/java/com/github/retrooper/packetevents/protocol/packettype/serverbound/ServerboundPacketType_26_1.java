/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2025 retrooper and contributors
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

package com.github.retrooper.packetevents.protocol.packettype.serverbound;

// Packet ID order extracted from 26.1.2 GameProtocols.class bytecode
// (javap -v GameProtocols | grep Serverbound, deduplicated, 0-indexed).
// CLIENT_SETTINGS, COOKIE_RESPONSE, PLUGIN_MESSAGE moved to CONFIGURATION
// in 26.X and are NOT in the PLAY state registration.
public enum ServerboundPacketType_26_1 {

    TELEPORT_CONFIRM,            // 0  AcceptTeleportation
    ATTACK,                      // 1  Attack (new in 26.X)
    QUERY_BLOCK_NBT,             // 2  BlockEntityTagQuery
    SELECT_BUNDLE_ITEM,          // 3  SelectBundleItem
    SET_DIFFICULTY,              // 4  ChangeDifficulty
    CHANGE_GAME_MODE,            // 5  ChangeGameMode
    CHAT_ACK,                    // 6  ChatAck
    CHAT_COMMAND_UNSIGNED,       // 7  ChatCommand
    CHAT_COMMAND,                // 8  ChatCommandSigned
    CHAT_MESSAGE,                // 9  Chat
    CHAT_SESSION_UPDATE,         // 10 ChatSessionUpdate
    CHUNK_BATCH_ACK,             // 11 ChunkBatchReceived
    CLIENT_STATUS,               // 12 ClientCommand
    CLIENT_TICK_END,             // 13 ClientTickEnd
    TAB_COMPLETE,                // 14 CommandSuggestion
    CONFIGURATION_ACK,           // 15 ConfigurationAcknowledged
    CLICK_WINDOW_BUTTON,         // 16 ContainerButtonClick
    CLICK_WINDOW,                // 17 ContainerClick
    CLOSE_WINDOW,                // 18 ContainerClose
    SLOT_STATE_CHANGE,           // 19 ContainerSlotStateChanged
    DEBUG_SUBSCRIPTION_REQUEST,  // 20 DebugSubscriptionRequest (new in 26.X)
    EDIT_BOOK,                   // 21 EditBook
    QUERY_ENTITY_NBT,            // 22 EntityTagQuery
    INTERACT_ENTITY,             // 23 Interact
    GENERATE_STRUCTURE,          // 24 JigsawGenerate
    LOCK_DIFFICULTY,             // 25 LockDifficulty
    PLAYER_POSITION,             // 26 MovePlayer$Pos
    PLAYER_POSITION_AND_ROTATION,// 27 MovePlayer$PosRot
    PLAYER_ROTATION,             // 28 MovePlayer$Rot
    PLAYER_FLYING,               // 29 MovePlayer$StatusOnly
    VEHICLE_MOVE,                // 30 MoveVehicle
    STEER_BOAT,                  // 31 PaddleBoat
    PICK_ITEM_FROM_BLOCK,        // 32 PickItemFromBlock
    PICK_ITEM_FROM_ENTITY,       // 33 PickItemFromEntity
    CRAFT_RECIPE_REQUEST,        // 34 PlaceRecipe
    PLAYER_ABILITIES,            // 35 PlayerAbilities
    PLAYER_DIGGING,              // 36 PlayerAction
    ENTITY_ACTION,               // 37 PlayerCommand
    PLAYER_INPUT,                // 38 PlayerInput
    PLAYER_LOADED,               // 39 PlayerLoaded
    SET_RECIPE_BOOK_STATE,       // 40 RecipeBookChangeSettings
    SET_DISPLAYED_RECIPE,        // 41 RecipeBookSeenRecipe
    NAME_ITEM,                   // 42 RenameItem
    ADVANCEMENT_TAB,             // 43 SeenAdvancements
    SELECT_TRADE,                // 44 SelectTrade
    SET_BEACON_EFFECT,           // 45 SetBeacon
    HELD_ITEM_CHANGE,            // 46 SetCarriedItem
    UPDATE_COMMAND_BLOCK,        // 47 SetCommandBlock
    UPDATE_COMMAND_BLOCK_MINECART,// 48 SetCommandMinecart
    CREATIVE_INVENTORY_ACTION,   // 49 SetCreativeModeSlot
    SET_GAME_RULE,               // 50 SetGameRule (new in 26.X)
    UPDATE_JIGSAW_BLOCK,         // 51 SetJigsawBlock
    UPDATE_STRUCTURE_BLOCK,      // 52 SetStructureBlock
    SET_TEST_BLOCK,              // 53 SetTestBlock (new in 26.X)
    UPDATE_SIGN,                 // 54 SignUpdate
    SPECTATE_ENTITY,             // 55 SpectateEntity (new name?)
    ANIMATION,                   // 56 Swing
    SPECTATE,                    // 57 TeleportToEntity
    TEST_INSTANCE_BLOCK_ACTION,  // 58 TestInstanceBlockAction (new in 26.X)
    PLAYER_BLOCK_PLACEMENT,      // 59 UseItemOn
    USE_ITEM,                    // 60 UseItem
}
