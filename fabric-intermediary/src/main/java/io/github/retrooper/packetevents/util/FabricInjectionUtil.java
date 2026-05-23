package io.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.FakeChannelUtil;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.handler.PacketDecoder;
import io.github.retrooper.packetevents.handler.PacketEncoder;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class FabricInjectionUtil {
    private static final String VIA_DECODER_NAME = "via-decoder";
    private static final String VIA_ENCODER_NAME = "via-encoder";

    public static void injectAtPipelineBuilder(ChannelPipeline pipeline, NetworkSide flow) {
        PacketSide pipelineSide = switch (flow) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };

        FabricPacketEventsAPI fabricPacketEventsAPI = FabricPacketEventsAPI.getAPI(pipelineSide);
        fabricPacketEventsAPI.getLogManager().debug("Game connected!");

        Channel channel = pipeline.channel();
        User user = new User(channel, ConnectionState.HANDSHAKING,
            null, new UserProfile(null, null));

        fabricPacketEventsAPI.getProtocolManager().setUser(channel, user);

        UserConnectEvent connectEvent = new UserConnectEvent(user);
        fabricPacketEventsAPI.getEventManager().callEvent(connectEvent);
        if (connectEvent.isCancelled()) {
            channel.unsafe().closeForcibly();
            return;
        }

        String decoderName = channel.pipeline().names().contains("inbound_config") ? "inbound_config" : "decoder";
        channel.pipeline().addBefore(decoderName, PacketEvents.DECODER_NAME, new PacketDecoder(pipelineSide, user, false));
        String encoderName = channel.pipeline().names().contains("outbound_config") ? "outbound_config" : "encoder";
        channel.pipeline().addBefore(encoderName, PacketEvents.ENCODER_NAME, new PacketEncoder(pipelineSide, user, false));
        if (PacketEvents.getAPI().getSettings().isPreViaInjection() && ViaVersionUtil.isAvailable(user)) {
            channel.pipeline().addBefore(VIA_DECODER_NAME, "pre-" + PacketEvents.DECODER_NAME, new PacketDecoder(pipelineSide, user, true));
            channel.pipeline().addBefore(VIA_ENCODER_NAME, "pre-" + PacketEvents.ENCODER_NAME, new PacketEncoder(pipelineSide, user, true));
        }
        channel.closeFuture().addListener((ChannelFutureListener) future ->
            PacketEventsImplHelper.handleDisconnection(user.getChannel(), user.getUUID()));
    }

    // Shared method to remove handlers if they exist
    public static void removeIfExists(ChannelPipeline pipeline, String handlerName) {
        if (pipeline.get(handlerName) != null) {
            pipeline.remove(handlerName);
        }
    }

    /**
     * Reorders PacketEvents handlers to maintain correct relative positions after
     * compression handlers are added or ViaVersion triggers a reorder.
     * <p>
     * Required decoder order: decompress -> pre-pe-decoder -> via-decoder -> pe-decoder -> decoder
     * Required encoder order: compress -> pre-pe-encoder -> via-encoder -> pe-encoder -> encoder
     * <p>
     * Note: Other handlers may exist between these, but the relative ordering must be preserved.
     * This method is resilient to being called multiple times - it will only move handlers
     * if they are actually in the wrong position.
     */
    public static void reorderHandlers(ChannelPipeline pipeline, PacketSide side) {
        FabricPacketEventsAPI api = FabricPacketEventsAPI.getAPI(side);

        if (api.getSettings().isDebugEnabled()) {
            api.getLogManager().debug("Pipeline before reorder: " + pipeline.names());
        }

        String preDecoderName = "pre-" + PacketEvents.DECODER_NAME;
        String peDecoderName = PacketEvents.DECODER_NAME;
        String preEncoderName = "pre-" + PacketEvents.ENCODER_NAME;
        String peEncoderName = PacketEvents.ENCODER_NAME;

        // Reorder decoders
        reorderDecoderPipeline(pipeline, preDecoderName, peDecoderName);

        // Reorder encoders
        reorderEncoderPipeline(pipeline, preEncoderName, peEncoderName);

        if (api.getSettings().isDebugEnabled()) {
            api.getLogManager().debug("Pipeline after reorder: " + pipeline.names());
        }
    }


    /**
     * Reorders decoder handlers to ensure correct relative positioning.
     * Required order: decompress -> pre-pe-decoder -> via-decoder -> pe-decoder -> decoder
     */
    private static void reorderDecoderPipeline(ChannelPipeline pipeline,
                                               String preDecoderName, String peDecoderName) {
        List<String> names = pipeline.names();

        // Find anchor handlers
        String decompressName = names.contains("decompress") ? "decompress" : null;
        String vanillaDecoderName = names.contains("inbound_config") ? "inbound_config" : "decoder";
        boolean hasVia = names.contains(VIA_DECODER_NAME);

        // Check if Via is in a valid position (after decompress, if decompress exists)
        // If Via is before decompress, we can't properly position pre-pe-decoder relative to both
        // In that case, we prioritize decompress and wait for Via to fix itself on next reorder
        boolean viaInValidPosition = !hasVia || decompressName == null ||
                names.indexOf(VIA_DECODER_NAME) > names.indexOf(decompressName);

        // Get current handlers
        ChannelHandler preDecoder = pipeline.get(preDecoderName);
        ChannelHandler peDecoder = pipeline.get(peDecoderName);

        // Check if pe-decoder needs reordering
        // Must be: after decompress, after via-decoder, before vanilla decoder
        boolean peNeedsReorder = false;
        if (peDecoder != null) {
            int peIdx = names.indexOf(peDecoderName);

            // Must be after decompress
            if (decompressName != null && !isAfter(names, peDecoderName, decompressName)) {
                peNeedsReorder = true;
            }
            // Must be after via-decoder
            if (hasVia && !isAfter(names, peDecoderName, VIA_DECODER_NAME)) {
                peNeedsReorder = true;
            }
            // Must be before vanilla decoder
            if (!isBefore(names, peDecoderName, vanillaDecoderName)) {
                peNeedsReorder = true;
            }
        }

        // Check if pre-pe-decoder needs reordering
        // Must be: after decompress, before via-decoder (only if via is valid)
        boolean preNeedsReorder = false;
        if (preDecoder != null) {
            // Must be after decompress
            if (decompressName != null && !isAfter(names, preDecoderName, decompressName)) {
                preNeedsReorder = true;
            }
            // Must be before via-decoder (only enforce if Via is properly positioned)
            if (hasVia && viaInValidPosition && !isBefore(names, preDecoderName, VIA_DECODER_NAME)) {
                preNeedsReorder = true;
            }
        }

        if (!peNeedsReorder && !preNeedsReorder) {
            return; // Everything is correctly positioned, avoid unnecessary work
        }

        // Remove handlers that need reordering
        if (preDecoder != null) pipeline.remove(preDecoderName);
        if (peDecoder != null) pipeline.remove(peDecoderName);

        // Refresh names after removal
        names = pipeline.names();
        hasVia = names.contains(VIA_DECODER_NAME);
        decompressName = names.contains("decompress") ? "decompress" : null;
        vanillaDecoderName = names.contains("inbound_config") ? "inbound_config" : "decoder";

        // Recalculate via validity after removal
        viaInValidPosition = !hasVia || decompressName == null ||
                names.indexOf(VIA_DECODER_NAME) > names.indexOf(decompressName);

        // Add pe-decoder: should be after the LATEST of (decompress, via-decoder)
        // This ensures it's after both anchors
        if (peDecoder != null) {
            String addAfter = findLatestHandler(names, decompressName,
                    hasVia ? VIA_DECODER_NAME : null);

            if (addAfter != null) {
                pipeline.addAfter(addAfter, peDecoderName, peDecoder);
            } else {
                pipeline.addBefore(vanillaDecoderName, peDecoderName, peDecoder);
            }
        }

        // Refresh names after adding pe-decoder
        names = pipeline.names();

        // Add pre-pe-decoder: before via-decoder (if Via is valid) or after decompress
        if (preDecoder != null) {
            if (hasVia && viaInValidPosition && names.contains(VIA_DECODER_NAME)) {
                // Via is properly positioned after decompress, add before Via
                pipeline.addBefore(VIA_DECODER_NAME, preDecoderName, preDecoder);
            } else if (decompressName != null && names.contains(decompressName)) {
                // Via isn't present or is in wrong position, just add after decompress
                // Next reorder (when Via fixes itself) will properly position us
                pipeline.addAfter(decompressName, preDecoderName, preDecoder);
            }
            // If neither anchor exists, don't add pre-decoder (meaningless without via or decompress)
        }
    }

    /**
     * Reorders encoder handlers to ensure correct relative positioning.
     * Required order: compress -> pre-pe-encoder -> via-encoder -> pe-encoder -> encoder
     */
    private static void reorderEncoderPipeline(ChannelPipeline pipeline,
                                               String preEncoderName, String peEncoderName) {
        List<String> names = pipeline.names();

        // Find anchor handlers
        String compressName = names.contains("compress") ? "compress" : null;
        String vanillaEncoderName = names.contains("outbound_config") ? "outbound_config" : "encoder";
        boolean hasVia = names.contains(VIA_ENCODER_NAME);

        // Check if Via is in a valid position (after compress, if compress exists)
        boolean viaInValidPosition = !hasVia || compressName == null ||
                names.indexOf(VIA_ENCODER_NAME) > names.indexOf(compressName);

        // Get current handlers
        ChannelHandler preEncoder = pipeline.get(preEncoderName);
        ChannelHandler peEncoder = pipeline.get(peEncoderName);

        // Check if pe-encoder needs reordering
        // Must be: after compress, after via-encoder, before vanilla encoder
        boolean peNeedsReorder = false;
        if (peEncoder != null) {
            // Must be after compress
            if (compressName != null && !isAfter(names, peEncoderName, compressName)) {
                peNeedsReorder = true;
            }
            // Must be after via-encoder
            if (hasVia && !isAfter(names, peEncoderName, VIA_ENCODER_NAME)) {
                peNeedsReorder = true;
            }
            // Must be before vanilla encoder
            if (!isBefore(names, peEncoderName, vanillaEncoderName)) {
                peNeedsReorder = true;
            }
        }

        // Check if pre-pe-encoder needs reordering
        // Must be: after compress, before via-encoder (only if via is valid)
        boolean preNeedsReorder = false;
        if (preEncoder != null) {
            // Must be after compress
            if (compressName != null && !isAfter(names, preEncoderName, compressName)) {
                preNeedsReorder = true;
            }
            // Must be before via-encoder (only enforce if Via is properly positioned)
            if (hasVia && viaInValidPosition && !isBefore(names, preEncoderName, VIA_ENCODER_NAME)) {
                preNeedsReorder = true;
            }
        }

        if (!peNeedsReorder && !preNeedsReorder) {
            return; // Everything is correctly positioned
        }

        // Remove handlers that need reordering
        if (preEncoder != null) pipeline.remove(preEncoderName);
        if (peEncoder != null) pipeline.remove(peEncoderName);

        // Refresh names after removal
        names = pipeline.names();
        hasVia = names.contains(VIA_ENCODER_NAME);
        compressName = names.contains("compress") ? "compress" : null;
        vanillaEncoderName = names.contains("outbound_config") ? "outbound_config" : "encoder";

        // Recalculate via validity
        viaInValidPosition = !hasVia || compressName == null ||
                names.indexOf(VIA_ENCODER_NAME) > names.indexOf(compressName);

        // Add pe-encoder: should be after the LATEST of (compress, via-encoder)
        if (peEncoder != null) {
            String addAfter = findLatestHandler(names, compressName,
                    hasVia ? VIA_ENCODER_NAME : null);

            if (addAfter != null) {
                pipeline.addAfter(addAfter, peEncoderName, peEncoder);
            } else {
                pipeline.addBefore(vanillaEncoderName, peEncoderName, peEncoder);
            }
        }

        // Refresh names
        names = pipeline.names();

        // Add pre-pe-encoder: before via-encoder (if Via is valid) or after compress
        if (preEncoder != null) {
            if (hasVia && viaInValidPosition && names.contains(VIA_ENCODER_NAME)) {
                pipeline.addBefore(VIA_ENCODER_NAME, preEncoderName, preEncoder);
            } else if (compressName != null && names.contains(compressName)) {
                pipeline.addAfter(compressName, preEncoderName, preEncoder);
            }
        }
    }

    /**
     * Checks if handler is positioned AFTER target in the pipeline.
     * Returns true if target doesn't exist (constraint is trivially satisfied).
     */
    private static boolean isAfter(List<String> names, String handler, String target) {
        int handlerIdx = names.indexOf(handler);
        int targetIdx = names.indexOf(target);
        if (handlerIdx == -1 || targetIdx == -1) return true;
        return handlerIdx > targetIdx;
    }

    /**
     * Checks if handler is positioned BEFORE target in the pipeline.
     * Returns true if target doesn't exist (constraint is trivially satisfied).
     */
    private static boolean isBefore(List<String> names, String handler, String target) {
        int handlerIdx = names.indexOf(handler);
        int targetIdx = names.indexOf(target);
        if (handlerIdx == -1 || targetIdx == -1) return true;
        return handlerIdx < targetIdx;
    }

    /**
     * Finds the handler with the highest index (latest in pipeline) among the given handler names.
     * Returns null if none of the handlers exist in the pipeline.
     */
    private static String findLatestHandler(List<String> names, String... handlers) {
        String latest = null;
        int latestIdx = -1;

        for (String handler : handlers) {
            if (handler != null) {
                int idx = names.indexOf(handler);
                if (idx > latestIdx) {
                    latestIdx = idx;
                    latest = handler;
                }
            }
        }

        return latest;
    }

    public static void fireUserLoginEvent(ServerPlayerEntity player) {
        FabricPacketEventsAPI api = FabricPacketEventsAPI.getServerAPI();

        User user = api.getPlayerManager().getUser(player);
        if (user == null) {
            Object channelObj = api.getPlayerManager().getChannel(player);

            // Check if it's a fake connection
            if (!FakeChannelUtil.isFakeChannel(channelObj) &&
                    (!api.isTerminated() || api.getSettings().isKickIfTerminated())) {
                // Kick the player if they're not a fake player
                // player.connection.disconnect(Component.literal("PacketEvents 2.0 failed to inject"));
                FabricPacketEventsAPI.getServerAPI().getPlayerManager().disconnectPlayer(player, "PacketEvents failed to inject into a channel.");
            }
            return;
        }

        api.getEventManager().callEvent(new UserLoginEvent(user, player));
    }
}
