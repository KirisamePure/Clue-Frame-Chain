package com.manus.clueframe.server;

import com.manus.clueframe.network.ClaimProgressPayload;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative claim state for tagged item frames.
 */
public final class ClaimTracker {
    public static final String CLUE_FRAME_TAG = "clue_frame";
    public static final int REQUIRED_TICKS = 60;
    private static final double MAX_CLAIM_DISTANCE = 5.0D;
    private static final int GRACE_TICKS = 10;
    private static final Map<UUID, HoldState> HOLDING_PLAYERS = new HashMap<>();

    private ClaimTracker() {
    }

    public static void register() {
        // A tagged frame cannot be dismantled or emptied through the normal click actions.
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                isTaggedFrame(entity) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                isTaggedFrame(entity) ? InteractionResult.SUCCESS : InteractionResult.PASS);

        ServerTickEvents.END_SERVER_TICK.register(ClaimTracker::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                HOLDING_PLAYERS.remove(handler.player.getUUID()));
    }

    /**
     * Records a single client tick of holding the use key. The server validates every signal.
     */
    public static void receiveHoldSignal(ServerPlayer player, int entityId) {
        ItemFrame frame = findEligibleFrame(player, entityId);
        if (frame == null) {
            clear(player);
            // Always reset the target supplied by the client as well. This prevents a
            // stale HUD bar when the player moves from a tagged frame to an ordinary one.
            sendProgress(player, entityId, 0, false);
            return;
        }

        HoldState state = HOLDING_PLAYERS.get(player.getUUID());
        if (state == null || state.entityId != entityId) {
            state = new HoldState(entityId);
            HOLDING_PLAYERS.put(player.getUUID(), state);
            sendProgress(player, entityId, 0, true);
        }
        state.receivedSignalThisTick = true;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, HoldState>> iterator = HOLDING_PLAYERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HoldState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            HoldState state = entry.getValue();

            if (player == null || !state.receivedSignalThisTick) {
                if (player != null) {
                    sendProgress(player, state.entityId, 0, false);
                }
                iterator.remove();
                continue;
            }

            if (!state.receivedSignalThisTick) {
                state.missedSignalTicks++;
                if (state.missedSignalTicks > GRACE_TICKS) {
                    // 只有连续丢失超过 5 tick 信号才真正重置
                    sendProgress(player, state.entityId, 0, false);
                    iterator.remove();
                    continue;
                }
            } else {
                state.missedSignalTicks = 0; // 收到信号则重置丢失计数
            }

            state.receivedSignalThisTick = false;

            ItemFrame frame = findEligibleFrame(player, state.entityId);
            if (frame == null) {
                sendProgress(player, state.entityId, 0, false);
                iterator.remove();
                continue;
            }

            state.heldTicks++;
            sendProgress(player, state.entityId, state.heldTicks, true);

            if (state.heldTicks >= REQUIRED_TICKS) {
                ItemStack reward = frame.getItem().copy();
                frame.setItem(ItemStack.EMPTY, true);
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
                frame.discard();
                sendProgress(player, state.entityId, REQUIRED_TICKS, false);
                iterator.remove();
            }
        }
    }

    private static ItemFrame findEligibleFrame(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof ItemFrame frame)
                || !frame.getTags().contains(CLUE_FRAME_TAG)
                || frame.getItem().isEmpty()
                || !frame.closerThan(player, MAX_CLAIM_DISTANCE)) {
            return null;
        }
        return frame;
    }

    private static boolean isTaggedFrame(Entity entity) {
        return entity instanceof ItemFrame frame && frame.getTags().contains(CLUE_FRAME_TAG);
    }

    private static void clear(ServerPlayer player) {
        HoldState previous = HOLDING_PLAYERS.remove(player.getUUID());
        if (previous != null) {
            sendProgress(player, previous.entityId, 0, false);
        }
    }

    private static void sendProgress(ServerPlayer player, int entityId, int heldTicks, boolean active) {
        ServerPlayNetworking.send(player, new ClaimProgressPayload(entityId, heldTicks, active));
    }

    private static final class HoldState {
        private final int entityId;
        private int heldTicks;
        private int missedSignalTicks;
        private boolean receivedSignalThisTick;

        private HoldState(int entityId) {
            this.entityId = entityId;
        }
    }
}
