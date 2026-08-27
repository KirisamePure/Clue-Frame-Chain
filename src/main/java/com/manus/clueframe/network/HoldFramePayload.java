package com.manus.clueframe.network;

import com.manus.clueframe.ClueFrameClaimMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent once per client tick while the use key is held over an item frame.
 */
public record HoldFramePayload(int entityId) implements CustomPacketPayload {
    public static final Type<HoldFramePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ClueFrameClaimMod.MOD_ID, "hold_frame"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HoldFramePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            HoldFramePayload::entityId,
            HoldFramePayload::new);

    @Override
    public Type<HoldFramePayload> type() {
        return TYPE;
    }
}
