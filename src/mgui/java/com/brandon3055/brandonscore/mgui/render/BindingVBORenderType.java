/*
 * Modified copy of CodeChickenLib 4.3.2 codechicken.lib.render.buffer.VBORenderType (LGPL-2.1), for the unofficial
 * BrandonsCore 1.19.2 port: binds the VertexBuffer around upload/draw like CodeChickenLib 1.20.1 does. The stock 4.3.2
 * class draws without binding, which corrupts vanilla vertex array state on 1.19.2.
 */
package com.brandon3055.brandonscore.mgui.render;

import codechicken.lib.render.buffer.DelegateRenderType;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import static net.covers1624.quack.util.SneakyUtils.none;

/**
 * A RenderType that is backed by a VertexBufferObject used for Instanced rendering.
 * <p>
 * Created by covers1624 on 25/5/20.
 */
public class BindingVBORenderType extends DelegateRenderType implements AutoCloseable {

    private final BiConsumer<VertexFormat, BufferBuilder> factory;
    private final VertexBuffer vertexBuffer = new VertexBuffer();
    private final BufferBuilder builder;
    private boolean dirty = true;

    /**
     * Create a new VBORenderType, delegates render state setup
     * to the provided parent, also uses the parents VertexFormat.
     *
     * @param parent  The parent, for state setup and buffer VertexFormat.
     * @param factory The Factory used to fill the BufferBuilder with data.
     */
    public BindingVBORenderType(RenderType parent, BiConsumer<VertexFormat, BufferBuilder> factory) {
        super(parent, parent.format());
        this.factory = factory;
        builder = new BufferBuilder(bufferSize());
    }

    /**
     * Marks this {@link VBORenderType} as needing to be re-built.
     */
    public void setDirty() {
        dirty = true;
    }

    /**
     * An extra {@link Runnable} to be applied before draw calls.
     *
     * @param action The action.
     * @return The same {@link WithCallbacks}.
     */
    public WithCallbacks withCallback(Runnable action) {
        return new WithCallbacks().withAction(action);
    }

    /**
     * An extra {@link RenderStateShard} to be applied.
     *
     * @param shard The {@link RenderStateShard}.
     * @return The same {@link WithCallbacks}.
     */
    public WithCallbacks withState(RenderStateShard shard) {
        return new WithCallbacks().withState(shard);
    }

    private void render() {
        rebuild();
        // 1.19.2 port fix: a VertexBuffer must be bound (its VAO) before drawing, and unbound afterwards, otherwise the
        // draw uses / corrupts whatever VAO is bound (vanilla's immediate-mode buffers): invisible GUI items, garbage geometry.
        this.vertexBuffer.bind();
        this.vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    private void rebuild() {
        if (!dirty) return;
        builder.begin(mode(), format());
        factory.accept(format(), builder);
        vertexBuffer.bind(); // 1.19.2 port fix: upload into this buffer's own VAO
        vertexBuffer.upload(builder.end());
        VertexBuffer.unbind();
        builder.clear();
        dirty = false;
    }

    @Override
    public void end(BufferBuilder buffer, int cameraX, int cameraY, int cameraZ) {
        // End buffer and discard state, we don't operate like this.
        buffer.endOrDiscardIfEmpty();

        setupRenderState();
        render();
        clearRenderState();
    }

    @Override
    public void close() throws Exception {
        vertexBuffer.close();
    }

    public class WithCallbacks extends DelegateRenderType {

        private final List<RenderStateShard> shards = new LinkedList<>();

        public WithCallbacks() {
            super(BindingVBORenderType.this);
        }

        /**
         * An extra {@link Runnable} to be applied before draw calls.
         *
         * @param action The action.
         * @return The same {@link WithCallbacks}.
         */
        public WithCallbacks withAction(Runnable action) {
            return withState(new RenderStateShard("none", none(), none()) {
                @Override
                public void setupRenderState() {
                    action.run();
                }
            });
        }

        /**
         * An extra {@link RenderStateShard} to be applied.
         *
         * @param shard The {@link RenderStateShard}.
         * @return The same {@link WithCallbacks}.
         */
        public WithCallbacks withState(RenderStateShard shard) {
            shards.add(shard);
            return this;
        }

        @Override
        public void setupRenderState() {
            super.setupRenderState();
            for (RenderStateShard state : shards) {
                state.setupRenderState();
            }
        }

        @Override
        public void end(BufferBuilder buffer, int cameraX, int cameraY, int cameraZ) {
            // End buffer and discard state, we don't operate like this.
            buffer.endOrDiscardIfEmpty();

            setupRenderState();
            render();
            clearRenderState();
        }

        @Override
        public void clearRenderState() {
            for (RenderStateShard state : shards) {
                state.clearRenderState();
            }
            super.clearRenderState();
        }
    }
}
