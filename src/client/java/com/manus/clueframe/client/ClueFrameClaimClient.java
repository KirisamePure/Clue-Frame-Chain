package com.manus.clueframe.client;

import com.manus.clueframe.network.ClaimProgressPayload;
import com.manus.clueframe.network.HoldFramePayload;
import com.manus.clueframe.server.ClaimTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Client-side input detection and HUD rendering for clue frame claims.
 */
public final class ClueFrameClaimClient implements ClientModInitializer {
    private static final int REQUIRED_TICKS = ClaimTracker.REQUIRED_TICKS;
    private static int activeFrameId = -1;
    private static int heldTicks;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClaimProgressPayload.TYPE, (payload, context) ->
                context.client().execute(() -> applyServerProgress(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(ClueFrameClaimClient::onClientTick);
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderProgressBar(graphics));
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null || !client.options.keyUse.isDown()) {
            reset();
            return;
        }

        ItemFrame frame = targetedFrame(client);
        if (frame == null || frame.getItem().isEmpty()) {
            reset();
            return;
        }

        int frameId = frame.getId();
        if (activeFrameId != -1 && activeFrameId != frameId) {
            reset();
        }
        activeFrameId = frameId;
        ClientPlayNetworking.send(new HoldFramePayload(frameId));
    }

    private static ItemFrame targetedFrame(Minecraft client) {
        if (!(client.hitResult instanceof EntityHitResult entityHit)) {
            return null;
        }
        Entity target = entityHit.getEntity();
        return target instanceof ItemFrame frame ? frame : null;
    }

    private static void applyServerProgress(ClaimProgressPayload payload) {
        if (!payload.active()) {
            if (payload.entityId() == activeFrameId) {
                reset();
            }
            return;
        }
        activeFrameId = payload.entityId();
        heldTicks = Math.min(payload.heldTicks(), REQUIRED_TICKS);
    }

    private static void renderProgressBar(GuiGraphics graphics) {
        if (activeFrameId == -1 || heldTicks <= 0) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int width = 92;
        int height = 10;
        int x = client.getWindow().getGuiScaledWidth() / 2 - width / 2;
        int y = client.getWindow().getGuiScaledHeight() / 2 + 18;
        int fillWidth = Math.round(width * (heldTicks / (float) REQUIRED_TICKS));

        // 外部阴影与金色边框。
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xA0000000);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFFE1B94A);

        // 深色背景与青绿色进度填充。
        graphics.fill(x, y, x + width, y + height, 0xFF18232A);
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, 0xFF36D2AF);
        }

    }

    private static void reset() {
        activeFrameId = -1;
        heldTicks = 0;
    }
}
