package com.brandon3055.brandonscore.client.render;

import com.brandon3055.brandonscore.BrandonsCore;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

/**
 * PORT(1.19.2) WP-13 R1: renderer-agnostic source of block entities for the BrandonsCore transparent BER pass
 * ({@link BlockEntityRendererTransparent#renderTransparent}, drawn by {@code BCClientEventHandler.renderLevelStage}
 * at {@code AFTER_PARTICLES}).
 * <p>
 * Upstream (1.20.1) finds those block entities only by walking the vanilla {@code LevelRenderer.renderChunksInFrustum}
 * and {@code globalBlockEntities} lists. Rubidium (Sodium) replaces vanilla chunk rendering and never fills either list,
 * so the pass never runs and e.g. the Draconic Evolution reactor core is invisible. (Embeddium patches BC for this on
 * 1.20.1; Rubidium 0.6.2c has no such patch.)
 * <p>
 * Vanilla, Rubidium and Oculus all draw block entities through {@code BlockEntityRenderDispatcher.render}, which calls
 * {@code setupAndRender} only after its own checks (renderer present, level set, valid block state, {@code shouldRender},
 * and after mods that cancel at its HEAD, e.g. EntityCulling). {@code mixin.BlockEntityRenderDispatcherMixin} reports
 * every such call here. Block entities of the client level whose renderer is a {@link BlockEntityRendererTransparent}
 * are remembered (identity, render order) until the transparent pass drains them. The set is also reset at the start
 * of every render frame and on client level unload, so captures made outside a level render (GUI, item renderers)
 * never leak into the next frame, and no block entity is held longer than one frame.
 * <p>
 * The transparent pass keeps the upstream vanilla-list walk and draws every block entity at most once, so with vanilla
 * chunk rendering it behaves exactly like upstream; captured block entities are only drawn when that walk did not reach
 * them (Rubidium). Nothing here references Rubidium, so it works the same when Rubidium is absent.
 * <p>
 * Render thread only (as the whole block entity pass).
 */
public final class TransparentPassTracker {

    private static final ReferenceLinkedOpenHashSet<BlockEntity> CAPTURED = new ReferenceLinkedOpenHashSet<>();
    private static final BlockEntity[] EMPTY = new BlockEntity[0];
    private static boolean hookSeen = false;

    private TransparentPassTracker() {
    }

    /**
     * Called by {@code BlockEntityRenderDispatcherMixin} at HEAD of {@code BlockEntityRenderDispatcher.setupAndRender}.
     */
    public static void onSetupAndRender(BlockEntityRenderer<?> renderer, BlockEntity blockEntity) {
        if (!hookSeen) {
            hookSeen = true;
            BrandonsCore.LOGGER.info("[1.19.2 port] Transparent BER pass: BlockEntityRenderDispatcher hook active (WP-13 R1).");
        }
        if (!(renderer instanceof BlockEntityRendererTransparent)) return;
        if (!RenderSystem.isOnRenderThread()) return;
        if (blockEntity.getLevel() != Minecraft.getInstance().level) return;
        //Oculus shadow pass: not a view of the camera; the transparent pass is a camera-view pass only.
        if (ShaderModCompat.isRenderingShadowPass()) return;
        CAPTURED.add(blockEntity);
    }

    /**
     * Passes the block entities captured since the last drain / reset to the consumer, in render order, and forgets them.
     * Safe if the consumer causes new captures (they are kept for the next drain).
     */
    public static void drain(Consumer<BlockEntity> consumer) {
        if (CAPTURED.isEmpty()) return;
        BlockEntity[] tiles = CAPTURED.toArray(EMPTY);
        CAPTURED.clear();
        for (BlockEntity tile : tiles) {
            consumer.accept(tile);
        }
    }

    /**
     * Forgets all captured block entities (start of a render frame, client level unload, logout).
     */
    public static void reset() {
        CAPTURED.clear();
    }

    /**
     * @return true once the dispatcher hook has been called at least once (diagnostics).
     */
    public static boolean isHookActive() {
        return hookSeen;
    }
}
