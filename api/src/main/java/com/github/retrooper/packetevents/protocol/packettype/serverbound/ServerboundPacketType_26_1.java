/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2025 retrooper and contributors
 *
 * Licensed under the GNU General Public License v3.0 (see the LICENSE file in
 * the project root or <http://www.gnu.org/licenses/>).
 */

package com.github.retrooper.packetevents.protocol.packettype.serverbound;

// Packet ID order extracted from 26.1.2 GameProtocols.class bytecode
// instruction sequence (javap -c -p | grep "getstatic.*Serverbound").
// Includes cross-package packets (common, cookie, ping) that are also
// registered in the PLAY state alongside game-package ones.
public enum ServerboundPacketType_26_1 {

    TELEPORT_CONFIRM,            // 0  AcceptTeleportation
    ATTACK,                      // 1  Attack
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
    CLIENT_SETTINGS,             // 14 ClientInformation (common package)
    TAB_COMPLETE,                // 15 CommandSuggestion
    CONFIGURATION_ACK,           // 16 ConfigurationAcknowledged
    CLICK_WINDOW_BUTTON,         // 17 ContainerButtonClick
    CLICK_WINDOW,                // 18 ContainerClick
    CLOSE_WINDOW,                // 19 ContainerClose
    SLOT_STATE_CHANGE,           // 20 ContainerSlotStateChanged
    COOKIE_RESPONSE,             // 21 CookieResponse (cookie package)
    PLUGIN_MESSAGE,              // 22 CustomPayload (common package)
    DEBUG_SUBSCRIPTION_REQUEST,  // 23 DebugSubscriptionRequest
    EDIT_BOOK,                   // 24 EditBook
    QUERY_ENTITY_NBT,            // 25 EntityTagQuery
    INTERACT_ENTITY,             // 26 Interact
    GENERATE_STRUCTURE,          // 27 JigsawGenerate
    KEEP_ALIVE,                  // 28 KeepAlive (common package)
    LOCK_DIFFICULTY,             // 29 LockDifficulty
    PLAYER_POSITION,             // 30 MovePlayer$Pos
    PLAYER_POSITION_AND_ROTATION,// 31 MovePlayer$PosRot
    PLAYER_ROTATION,             // 32 MovePlayer$Rot
    PLAYER_FLYING,               // 33 MovePlayer$StatusOnly
    VEHICLE_MOVE,                // 34 MoveVehicle
    STEER_BOAT,                  // 35 PaddleBoat
    PICK_ITEM_FROM_BLOCK,        // 36 PickItemFromBlock
    PICK_ITEM_FROM_ENTITY,       // 37 PickItemFromEntity
    DEBUG_PING,                  // 38 PingRequest (ping package)
    CRAFT_RECIPE_REQUEST,        // 39 PlaceRecipe
    PLAYER_ABILITIES,            // 40 PlayerAbilities
    PLAYER_DIGGING,              // 41 PlayerAction
    ENTITY_ACTION,               // 42 PlayerCommand
    PLAYER_INPUT,                // 43 PlayerInput
    PLAYER_LOADED,               // 44 PlayerLoaded
    PONG,                        // 45 Pong (common package)
    SET_RECIPE_BOOK_STATE,       // 46 RecipeBookChangeSettings
    SET_DISPLAYED_RECIPE,        // 47 RecipeBookSeenRecipe
    NAME_ITEM,                   // 48 RenameItem
    RESOURCE_PACK_STATUS,        // 49 ResourcePack (common package)
    ADVANCEMENT_TAB,             // 50 SeenAdvancements
    SELECT_TRADE,                // 51 SelectTrade
    SET_BEACON_EFFECT,           // 52 SetBeacon
    HELD_ITEM_CHANGE,            // 53 SetCarriedItem
    UPDATE_COMMAND_BLOCK,        // 54 SetCommandBlock
    UPDATE_COMMAND_BLOCK_MINECART,// 55 SetCommandMinecart
    CREATIVE_INVENTORY_ACTION,   // 56 SetCreativeModeSlot
    SET_GAME_RULE,               // 57 SetGameRule
    UPDATE_JIGSAW_BLOCK,         // 58 SetJigsawBlock
    UPDATE_STRUCTURE_BLOCK,      // 59 SetStructureBlock
    SET_TEST_BLOCK,              // 60 SetTestBlock
    UPDATE_SIGN,                 // 61 SignUpdate
    SPECTATE_ENTITY,             // 62 SpectateEntity
    ANIMATION,                   // 63 Swing
    SPECTATE,                    // 64 TeleportToEntity
    TEST_INSTANCE_BLOCK_ACTION,  // 65 TestInstanceBlockAction
    PLAYER_BLOCK_PLACEMENT,      // 66 UseItemOn
    USE_ITEM,                    // 67 UseItem
    CUSTOM_CLICK_ACTION,         // 68 CustomClickAction (common package)
}
