package com.manus.clueframe.client;

import com.manus.clueframe.ClueFrameClaimMod;
import com.manus.clueframe.network.ClaimProgressPayload;
import com.manus.clueframe.network.HoldFramePayload;
import com.manus.clueframe.server.ClaimTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Client-side input detection and HUD rendering for clue frame claims.
 */
public final class ClueFrameClaimClient implements ClientModInitializer {
    private static final int REQUIRED_TICKS = ClaimTracker.REQUIRED_TICKS;
    private static final int MAGNIFIER_TEXTURE_SIZE = 32;
    private static final int MAGNIFIER_DRAW_SIZE = 32;
    private static final int MAGNIFIER_FRAME_TICKS = 5;

    private static final ResourceLocation MAGNIFIER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ClueFrameClaimMod.MOD_ID,
                    "textures/gui/search_magnifier.png");
    private static final int BAR_WIDTH = 92;
    private static final int BAR_HEIGHT = 10;
    private static final ResourceLocation PROGRESS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ClueFrameClaimMod.MOD_ID,
                    "textures/gui/clue_progress.png");
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
        int x = client.getWindow().getGuiScaledWidth() / 2 - BAR_WIDTH / 2;
        int y = client.getWindow().getGuiScaledHeight() / 2 + 18;

        float progress = Math.clamp(heldTicks / (float) REQUIRED_TICKS, 0.0F, 1.0F);
        int fillWidth = Math.round(BAR_WIDTH * progress);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                PROGRESS_TEXTURE,
                x, y,
                0, 0,
                BAR_WIDTH, BAR_HEIGHT,
                BAR_WIDTH, BAR_HEIGHT * 2
        );

        if (fillWidth > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    PROGRESS_TEXTURE,
                    x, y,
                    0, BAR_HEIGHT,
                    fillWidth, BAR_HEIGHT,
                    BAR_WIDTH, BAR_HEIGHT * 2
            );
        }
        int iconX = x - MAGNIFIER_DRAW_SIZE - 6;
        int iconY = y + (BAR_HEIGHT - MAGNIFIER_DRAW_SIZE) / 2;
        renderSearchingMagnifier(graphics, iconX, iconY);

    }

    private static void renderSearchingMagnifier(GuiGraphics graphics, int x, int y) {
        int phase = Math.floorMod(heldTicks / MAGNIFIER_FRAME_TICKS, 4);

        int offsetX = switch (phase) {
            case 0 -> -1;
            case 1, 2 -> 1;
            default -> 0;
        };

        int offsetY = switch (phase) {
            case 0, 1 -> -1;
            case 2 -> 1;
            default -> 0;
        };

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                MAGNIFIER_TEXTURE,
                x + offsetX, y + offsetY,
                0, 0,
                MAGNIFIER_DRAW_SIZE, MAGNIFIER_DRAW_SIZE,
                MAGNIFIER_TEXTURE_SIZE, MAGNIFIER_TEXTURE_SIZE
        );
    }

    private static void reset() {
        activeFrameId = -1;
        heldTicks = 0;
    }
}
