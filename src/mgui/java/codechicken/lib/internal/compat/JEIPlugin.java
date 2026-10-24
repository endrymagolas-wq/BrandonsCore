// Modified for Minecraft 1.19.2 / Forge 43 by the BrandonsCore 1.19.2 port (2026-10-24, see git history); based on CodeChickenLib 1.20.1 @3b43c7f, LGPL-2.1.
package codechicken.lib.internal.compat;

import codechicken.lib.gui.modular.ModularGui;
import codechicken.lib.gui.modular.ModularGuiContainer;
import codechicken.lib.gui.modular.elements.GuiElement;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.covers1624.quack.collection.FastStream;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.19.2 backport notes (BrandonsCore 1.19.2 port, WP-10b): ships inside BrandonsCore (stock CCL 4.3.2 has no
 * modular GUI and no such plugin), so the plugin UID is {@code brandonscore:mgui_jei}; built against the JEI 11.6
 * (1.19.2) API, whose {@link IGhostIngredientHandler} still has the abstract untyped {@code getTargets}.
 * <p>
 * Created by brandon3055 on 31/12/2023
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation("brandonscore", "mgui_jei");

    public JEIPlugin() {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(ModularGuiContainer.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(ModularGuiContainer screen) {
                return FastStream.of(screen.getModularGui().getJeiExclusions()).map(e -> e.getRectangle().toRect2i()).toList();
            }
        });
        registration.addGhostIngredientHandler(ModularGuiContainer.class, new IngredientDropHandler());
    }

    private static class IngredientDropHandler implements IGhostIngredientHandler<ModularGuiContainer> {
        private ModularGui gui;
        private boolean highlight = true;

        @Override
        public <I> List<Target<I>> getTargetsTyped(ModularGuiContainer screen, ITypedIngredient<I> ingredient, boolean doStart) {
            gui = screen.getModularGui();
            gui.setJeiHighlightTime(doStart ? 60 * 20 : 3 * 20);
            highlight = !doStart;
            if (!doStart) return Collections.emptyList();
            List<Target<I>> targets = new ArrayList<>();
            ingredient.getIngredient(VanillaTypes.ITEM_STACK).ifPresent(stack -> gui.getJeiDropTargets().forEach(e -> targets.add(new DropTarget<>(e))));
            return targets;
        }

        /**
         * JEI 11.x (1.19.2): abstract untyped variant (the typed default above is what JEI 11 calls; this one is
         * required by the interface). Same behaviour, the ingredient must be an ItemStack.
         */
        @Override
        public <I> List<Target<I>> getTargets(ModularGuiContainer screen, I ingredient, boolean doStart) {
            gui = screen.getModularGui();
            gui.setJeiHighlightTime(doStart ? 60 * 20 : 3 * 20);
            highlight = !doStart;
            if (!doStart) return Collections.emptyList();
            List<Target<I>> targets = new ArrayList<>();
            if (ingredient instanceof ItemStack) {
                gui.getJeiDropTargets().forEach(e -> targets.add(new DropTarget<>(e)));
            }
            return targets;
        }

        @Override
        public void onComplete() {
            highlight = true;
            gui.setJeiHighlightTime(0);
        }

        @Override
        public boolean shouldHighlightTargets() {
            return highlight;
        }
    }

    private record DropTarget<I>(GuiElement<?> element) implements IGhostIngredientHandler.Target<I> {
        @Override
        public Rect2i getArea() {
            return element.getRectangle().toRect2i();
        }

        @Override
        public void accept(I ingredient) {
            element.getJeiDropConsumer().accept((ItemStack) ingredient);
        }
    }
}
