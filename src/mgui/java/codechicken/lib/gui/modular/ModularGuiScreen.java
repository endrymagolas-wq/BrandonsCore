// Modified for Minecraft 1.19.2 / Forge 43 by the BrandonsCore 1.19.2 port (2026-10-24, see git history); based on CodeChickenLib 1.20.1 @3b43c7f, LGPL-2.1.
package codechicken.lib.gui.modular;

import codechicken.lib.gui.modular.lib.GuiProvider;
import codechicken.lib.gui.modular.lib.GuiRender;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * A simple ModularGui screen implementation.
 * This is simply a wrapper for a {@link ModularGui} that takes a {@link GuiProvider}
 * This should be suitable for most basic gui screens.
 * <p>
 * Created by brandon3055 on 19/08/2023
 */
public class ModularGuiScreen extends Screen {

    protected final ModularGui modularGui;

    public ModularGuiScreen(GuiProvider provider) {
        super(Component.empty());
        this.modularGui = new ModularGui(provider);
        this.modularGui.setScreen(this);
    }

    public ModularGuiScreen(GuiProvider builder, Screen parentScreen) {
        super(Component.empty());
        this.modularGui = new ModularGui(builder, parentScreen);
        this.modularGui.setScreen(this);
    }

    public ModularGui getModularGui() {
        return modularGui;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return modularGui.getGuiTitle();
    }

    @Override
    public boolean isPauseScreen() {
        return modularGui.isPauseScreen();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return modularGui.closeOnEscape();
    }

    @Override
    protected void init() {
        modularGui.onScreenInit(minecraft, font, width, height);
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        modularGui.onScreenInit(minecraft, font, width, height);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (modularGui.renderBackground()) {
            renderBackground(poseStack);
        }
        GuiRender render = GuiRender.convert(poseStack);
        modularGui.render(render, partialTicks);
        modularGui.renderOverlay(render, partialTicks);
        render.flush(); //1.19.2: vanilla does not flush the shared buffer source after the screen renders (1.20 does).
    }

    @Override
    public void tick() {
        modularGui.tick();
    }

    @Override
    public void removed() {
        modularGui.onGuiClose();
    }

    //=== Input Pass-though ===//

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        modularGui.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return modularGui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return modularGui.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return modularGui.mouseScrolled(mouseX, mouseY, scroll) || super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return modularGui.keyPressed(key, scancode, modifiers) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        return modularGui.keyReleased(key, scancode, modifiers) || super.keyReleased(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return modularGui.charTyped(character, modifiers) || super.charTyped(character, modifiers);
    }
}
