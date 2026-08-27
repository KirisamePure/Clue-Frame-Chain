package com.manus.clueframe.network;

import com.manus.clueframe.ClueFrameClaimMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-authoritative progress update for the local claim HUD.
 */
public record ClaimProgressPayload(int entityId, int heldTicks, boolean active) implements CustomPacketPayload {
    public static final Type<ClaimProgressPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ClueFrameClaimMod.MOD_ID, "claim_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimProgressPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ClaimProgressPayload::entityId,
            ByteBufCodecs.INT,
            ClaimProgressPayload::heldTicks,
            ByteBufCodecs.BOOL,
            ClaimProgressPayload::active,
            ClaimProgressPayload::new);

    @Override
    public Type<ClaimProgressPayload> type() {
        return TYPE;
    }
}
