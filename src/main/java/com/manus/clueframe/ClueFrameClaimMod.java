package com.manus.clueframe;

import com.manus.clueframe.network.ClaimProgressPayload;
import com.manus.clueframe.network.HoldFramePayload;
import com.manus.clueframe.server.ClaimTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Common entrypoint for the Clue Frame Claim mod.
 */
public final class ClueFrameClaimMod implements ModInitializer {
    public static final String MOD_ID = "clue_frame_claim";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(HoldFramePayload.TYPE, HoldFramePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClaimProgressPayload.TYPE, ClaimProgressPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HoldFramePayload.TYPE, (payload, context) ->
                ClaimTracker.receiveHoldSignal(context.player(), payload.entityId()));
        ClaimTracker.register();
    }
}
