/*
 * FIXED VERSION: Restored buffer management logic
 */

package io.github.retrooper.packetevents.handler;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.exception.CancelPacketException;
import com.github.retrooper.packetevents.exception.InvalidDisconnectPacketSend;
import com.github.retrooper.packetevents.exception.PacketProcessException;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.ExceptionUtil;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal @ChannelHandler.Sharable
public class PacketEncoder extends ChannelOutboundHandlerAdapter {

    private static final boolean NETTY_4_1_0;

    static {
        boolean netty410 = false;
        try {
            ChannelPromise.class.getDeclaredMethod("unvoid");
            netty410 = true;
        } catch (NoSuchMethodException ignored) {
        }
        NETTY_4_1_0 = netty410;
    }

    private final PacketSide side;
    public User user;
    public PlayerEntity player;
    private ChannelPromise promise;
    private final boolean preViaVersion;

    public PacketEncoder(PacketSide side, User user, boolean preViaVersion) {
        this.side = side;
        this.user = user;
        this.preViaVersion = preViaVersion;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof ByteBuf in)) {
            ctx.write(msg, promise);
            return;
        }

        // Handle promise management
        ChannelPromise oldPromise = this.promise != null && !this.promise.isSuccess() ? this.promise : null;
        if (NETTY_4_1_0) {
            promise = promise.unvoid();
        }
        promise.addListener(p -> this.promise = oldPromise);
        this.promise = promise;

        handlePacket(ctx, in, promise);

        if (!ByteBufHelper.isReadable(in)) {
            throw CancelPacketException.INSTANCE;
        } else {
            ctx.write(in, promise);
        }
    }

    private @Nullable ProtocolPacketEvent handlePacket(ChannelHandlerContext ctx, ByteBuf buffer, ChannelPromise promise) throws Exception {
        if (!preViaVersion && PacketEvents.getAPI().getSettings().isPreViaInjection() && !ViaVersionUtil.isAvailable(user))
            PacketEventsImplHelper.handlePacket(ctx.channel(), user, player, buffer, preViaVersion, this.side);

        ProtocolPacketEvent protocolPacketEvent = PacketEventsImplHelper.handlePacket(
                ctx.channel(), this.user, this.player, buffer, !preViaVersion, this.side
        );

        if (protocolPacketEvent instanceof PacketSendEvent packetSendEvent && packetSendEvent.hasTasksAfterSend()) {
            promise.addListener((p) -> {
                for (Runnable task : packetSendEvent.getTasksAfterSend()) {
                    task.run();
                }
            });
        }
        return protocolPacketEvent;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ExceptionUtil.isException(cause, CancelPacketException.class)) return;
        if (ExceptionUtil.isException(cause, InvalidDisconnectPacketSend.class)) return;

        boolean didWeCauseThis = ExceptionUtil.isException(cause, PacketProcessException.class);
        if (didWeCauseThis && (user == null || user.getEncoderState() != ConnectionState.HANDSHAKING)) {
            if (PacketEvents.getAPI().getSettings().isKickOnPacketExceptionEnabled()) {
                try {
                    if (user != null && player instanceof ServerPlayerEntity) {
                        WrapperPlayServerDisconnect disconnectPacket = new WrapperPlayServerDisconnect(
                                net.kyori.adventure.text.Component.text("Invalid packet")
                        );
                        user.sendPacket(disconnectPacket);
                    }
                } catch (Exception ignored) {}
                ctx.channel().close();
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.getServer().execute(() -> {
                        FabricPacketEventsAPI.getServerAPI().getPlayerManager().disconnectPlayer(serverPlayer, "Invalid packet");
                    });
                }
            }
        }
        super.exceptionCaught(ctx, cause);
    }
}