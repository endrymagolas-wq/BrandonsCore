package codechicken.lib.gui.modular.sprite;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 1.19.2 backport notes (BrandonsCore 1.19.2 port, WP-10):
 * 1.19.2 has no SpriteLoader / atlas json "sprite sources", so the 1.20 {@code atlases/<atlasInfoLocation>.json}
 * (a single "directory" source with source = atlasInfoLocation and prefix = atlasInfoLocation + "/", which is what
 * CCL, BrandonsCore and Draconic Evolution all use) is reproduced in code: every {@code textures/<atlasInfoLocation>/**.png}
 * of this mod's namespace becomes the sprite {@code modid:<atlasInfoLocation>/<path without .png>}, stitched with
 * the 1.19.2 {@link TextureAtlas#prepareToStitch} / {@link TextureAtlas#reload} (as 1.19.2 TextureAtlasHolder does).
 * The json file itself is not read. The 1.19.2 TextureAtlas constructor queries GL, so the atlas is created lazily
 * on the render thread.
 * <p>
 * Created by brandon3055 on 20/08/2023
 */
public class ModAtlasHolder implements PreparableReloadListener, AutoCloseable {

    private TextureAtlas textureAtlas;
    private final ResourceLocation atlasLocation;
    private final ResourceLocation atlasInfoLocation;
    private final String modid;

    /**
     * Defines a mod texture atlas.
     * Must be registered as a resource reload listener via RegisterClientReloadListenersEvent
     * This is all that is needed to create a custom texture atlas.
     *
     * @param modid             The mod id of the mod registering this atlas.
     * @param atlasLocation     The texture atlas location. e.g. "textures/atlas/gui.png" (Will have the modid: prefix added automatically)
     * @param atlasInfoLocation The path to the atlas json file relative to modid:atlases/
     *                          e.g. "gui" will point to modid:atlases/gui.json
     */
    public ModAtlasHolder(String modid, String atlasLocation, String atlasInfoLocation) {
        this.atlasInfoLocation = new ResourceLocation(modid, atlasInfoLocation);
        this.atlasLocation = new ResourceLocation(modid, atlasLocation);
        this.modid = modid;
        // TODO remove in 1.20.4
        // 1.19.2: create (and register) the atlas right away only when it is safe to touch GL, otherwise on first use.
        if (RenderSystem.isOnRenderThreadOrInit() && Minecraft.getInstance() != null && Minecraft.getInstance().getTextureManager() != null) {
            atlas();
        }
    }

    public void init() {
        Minecraft.getInstance().getTextureManager().register(atlas().location(), atlas());
    }

    /**
     * 1.19.2: lazily creates the texture atlas (the 1.19.2 TextureAtlas constructor queries the max texture size from GL)
     * and registers it with the texture manager as soon as one exists.
     */
    private TextureAtlas atlas() {
        if (this.textureAtlas == null) {
            this.textureAtlas = new TextureAtlas(this.atlasLocation);
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            if (textureManager != null) {
                textureManager.register(this.textureAtlas.location(), this.textureAtlas);
            }
        }
        return this.textureAtlas;
    }

    public ResourceLocation atlasLocation() {
        return atlasLocation;
    }

    public TextureAtlasSprite getSprite(ResourceLocation resourceLocation) {
        return atlas().getSprite(resourceLocation);
    }

    @Override
    public final @NotNull CompletableFuture<Void> reload(PreparationBarrier prepBarrier, ResourceManager resourceManager, ProfilerFiller profiler, ProfilerFiller profiler2, Executor executor, Executor executor2) {
        Objects.requireNonNull(prepBarrier);
        TextureAtlas atlas = atlas(); //The reload instance calls this on the main (render) thread.
        ResourceManager modResources = new ModResourceManager(resourceManager, modid);
        return CompletableFuture.supplyAsync(() -> this.prepare(atlas, modResources, profiler), executor)
                .thenCompose(prepBarrier::wait)
                .thenAcceptAsync((preparations) -> this.apply(atlas, preparations, profiler2), executor2);
    }

    /**
     * 1.19.2 equivalent of the 1.20 "directory" sprite source with source = atlasInfoLocation and prefix = atlasInfoLocation + "/":
     * {@code modid:textures/<source>/a/b.png} becomes the sprite {@code modid:<source>/a/b}.
     * (The 1.19.2 atlas loads {@code textures/<sprite>.png}, which is exactly the file that was found.)
     */
    private Stream<ResourceLocation> listSprites(ResourceManager modResources) {
        String source = "textures/" + atlasInfoLocation.getPath();
        return modResources.listResources(source, e -> e.getPath().endsWith(".png")).keySet().stream()
                .map(e -> new ResourceLocation(e.getNamespace(), e.getPath().substring("textures/".length(), e.getPath().length() - ".png".length())))
                .sorted();
    }

    private TextureAtlas.Preparations prepare(TextureAtlas atlas, ResourceManager modResources, ProfilerFiller profilerFiller) {
        profilerFiller.startTick();
        profilerFiller.push("stitching");
        TextureAtlas.Preparations preparations = atlas.prepareToStitch(modResources, listSprites(modResources), profilerFiller, 0);
        profilerFiller.pop();
        profilerFiller.endTick();
        return preparations;
    }

    private void apply(TextureAtlas atlas, TextureAtlas.Preparations preparations, ProfilerFiller profilerFiller) {
        profilerFiller.startTick();
        profilerFiller.push("upload");
        atlas.reload(preparations);
        profilerFiller.pop();
        profilerFiller.endTick();
    }

    @Override
    public void close() {
        if (this.textureAtlas != null) {
            this.textureAtlas.clearTextureData();
        }
    }

    public static class ModResourceManager implements ResourceManager {

        private final ResourceManager wrapped;
        private final String modid;

        public ModResourceManager(ResourceManager wrapped, String modid) {
            this.wrapped = wrapped;
            this.modid = modid;
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(String pPath, Predicate<ResourceLocation> pFilter) {
            return wrapped.listResources(pPath, pFilter.and(e -> e.getNamespace().equals(modid)));
        }

        //@formatter:off
        @Override public Set<String> getNamespaces() { return wrapped.getNamespaces(); }
        @Override public List<Resource> getResourceStack(ResourceLocation pLocation) { return wrapped.getResourceStack(pLocation); }
        @Override public Map<ResourceLocation, List<Resource>> listResourceStacks(String pPath, Predicate<ResourceLocation> pFilter) { return wrapped.listResourceStacks(pPath, pFilter); }
        @Override public Stream<PackResources> listPacks() { return wrapped.listPacks(); }
        @Override public Optional<Resource> getResource(ResourceLocation pLocation) { return wrapped.getResource(pLocation); }
        //@formatter:on
    }
}
