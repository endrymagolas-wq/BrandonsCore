package com.brandon3055.brandonscore.mixin;

import com.brandon3055.brandonscore.client.render.TransparentPassTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PORT(1.19.2) WP-13 R1: reports every block entity the dispatcher actually renders to {@link TransparentPassTracker},
 * so the BrandonsCore transparent BER pass also works when Rubidium replaces vanilla chunk rendering
 * (spec phase5-ic2-compat §2.9 option B). Observe-only: nothing is cancelled or changed.
 * <p>
 * {@code setupAndRender} is reached from {@code render} only after the renderer, level, block-state and
 * {@code shouldRender} checks, and after HEAD cancels of other mods (EntityCulling).
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(
            method = "setupAndRender(Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD")
    )
    private static void brandonscore$captureTransparent(BlockEntityRenderer<?> renderer, BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        TransparentPassTracker.onSetupAndRender(renderer, blockEntity);
    }
}
