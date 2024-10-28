package io.github.retrooper.packetevents.injector.handlers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.injector.connection.ServerConnectionInitializer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/*
 * Why this class exists? This class is instantiated only if we are using PacketEvents before ViaVersion
 * This class works only in newer paper versions where the COMPRESSION_ENABLED_EVENT exists
 *
 * Since PacketEventsDecoder is BEFORE ViaDecoder, its userEventTriggered method is called before ViaVersion's
 * leading Via to relocate AFTER our relocation
 *
 * We don't want this, in order to put our handlers in the right position our relocation must happen after ViaVersion
 * So this handler is put after via-decoder in order to relocate our handlers right after ViaVersion did
 * And then it's removed from the pipeline
 *
 * @author ytnoos
 */
public class PacketEventsHandlerMonitor extends ChannelInboundHandlerAdapter {

    private final User user;

    public PacketEventsHandlerMonitor(User user) {
        this.user = user;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object event) throws Exception {
        if (PacketEventsEncoder.COMPRESSION_ENABLED_EVENT != null && event == PacketEventsEncoder.COMPRESSION_ENABLED_EVENT) {
            ServerConnectionInitializer.relocateHandlers(ctx.channel(), (PacketEventsDecoder) ctx.pipeline().get(PacketEvents.DECODER_NAME), user);
            ctx.pipeline().remove(this);
        }

        super.userEventTriggered(ctx, event);
    }
}
