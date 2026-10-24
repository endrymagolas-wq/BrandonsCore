package codechicken.lib.gui.modular.elements;

import codechicken.lib.colour.Colour;
import codechicken.lib.gui.modular.lib.*;
import codechicken.lib.gui.modular.lib.geometry.Axis;
import codechicken.lib.gui.modular.lib.geometry.GuiParent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static codechicken.lib.gui.modular.lib.geometry.Constraint.*;
import static codechicken.lib.gui.modular.lib.geometry.GeoParam.*;

/**
 * Created by brandon3055 on 17/11/2023
 */
public class GuiColourPicker extends GuiManipulable {

    private ColourState colourState = ColourState.create();
    private final Colour initialColour;
    private boolean blockOutsideClicks = false;
    private boolean closeOnOutsideClick = false;
    private GuiButton okButton;
    private GuiButton cancelButton;

    public GuiColourPicker(@NotNull GuiParent<?> parent, Colour initialColour) {
        super(parent);
        this.initialColour = initialColour;
    }

    public static GuiColourPicker create(GuiParent<?> guiParent, ColourState colourState) {
        return create(guiParent, colourState, true);
    }

    public static GuiColourPicker create(GuiParent<?> guiParent, ColourState colourState, boolean hasAlpha) {
        GuiColourPicker picker = new GuiColourPicker(guiParent.getModularGui().getRoot(), colourState.getColour());
        picker.setOpaque(true);
        picker.setColourState(colourState);
        Constraints.size(picker, 80, hasAlpha ? 80 : 70);

        GuiRectangle background = GuiRectangle.toolTipBackground(picker.getContentElement());
        Constraints.bind(background, picker.getContentElement());

        var hexField = GuiTextField.create(background, 0xFF000000, 0xFF505050, 0xe0e0e0);
        hexField.field()
                .setTextState(picker.getTextState())
                .setMaxLength(hasAlpha ? 8 : 6)
                .setFilter(s -> s.isEmpty() || validHex(s));
        hexField.container()
                .setOpaque(true)
                .constrain(HEIGHT, literal(12))
                .constrain(TOP, relative(background.get(TOP), 4))
                .constrain(LEFT, relative(background.get(LEFT), 4))
                .constrain(RIGHT, relative(background.get(RIGHT), -4));

        SliderBG slider = makeSlider(background, 0xFFFF0000, picker.sliderStateRed())
                .constrain(TOP, relative(hexField.container().get(BOTTOM), 2));

        slider = makeSlider(background, 0xFF00FF00, picker.sliderStateGreen())
                .constrain(TOP, relative(slider.get(BOTTOM), 1));

        slider = makeSlider(background, 0xFF0000FF, picker.sliderStateBlue())
                .constrain(TOP, relative(slider.get(BOTTOM), 1));

        if (hasAlpha) {
            slider = makeSlider(background, 0xFFFFFFFF, picker.sliderStateAlpha())
                    .constrain(TOP, relative(slider.get(BOTTOM), 1));
        } else {
            colourState.set(aF(colourState.getColour(), 0));
        }

        ColourPreview preview = new ColourPreview(background, () -> hasAlpha ? colourState.get() : (colourState.get() | 0xFF000000))
                .setOpaque(true)
                .constrain(HEIGHT, literal(6))
                .constrain(TOP, relative(slider.get(BOTTOM), 2))
                .constrain(LEFT, relative(background.get(LEFT), 4))
                .constrain(RIGHT, relative(background.get(RIGHT), -4));

        picker.cancelButton = GuiButton.flatColourButton(background, () -> Component.translatable("gui.cancel"), e -> 0xFF000000, e -> e ? 0xFF777777 : 0xFF555555)
                .setOpaque(true)
                .onPress(picker::cancel)
                .constrain(HEIGHT, literal(10))
                .constrain(TOP, relative(preview.get(BOTTOM), 2))
                .constrain(LEFT, midPoint(background.get(LEFT), background.get(RIGHT), -4))
                .constrain(RIGHT, relative(background.get(RIGHT), -4));

        picker.okButton = GuiButton.flatColourButton(background, () -> Component.translatable("gui.ok"), e -> 0xFF000000, e -> e ? 0xFF777777 : 0xFF555555)
                .setOpaque(true)
                .onPress(() -> picker.getParent().removeChild(picker))
                .constrain(HEIGHT, literal(10))
                .constrain(TOP, relative(preview.get(BOTTOM), 2))
                .constrain(LEFT, relative(background.get(LEFT), 4))
                .constrain(RIGHT, dynamic(() -> picker.cancelButton.isEnabled() ? picker.cancelButton.xMin() - 2 : background.xMax() - 4));

        return picker;
    }

    public static SliderBG makeSlider(GuiElement<?> background, int colour, SliderState state) {
        SliderBG slideBG = new SliderBG(background, 0xFF505050, 0x30FFFFFF)
                .setOpaque(true)
                .constrain(HEIGHT, literal(9))
                .constrain(LEFT, relative(background.get(LEFT), 4))
                .constrain(RIGHT, relative(background.get(RIGHT), -4));

        GuiSlider slider = new GuiSlider(slideBG, Axis.X)
                .setSliderState(state);
        Constraints.bind(slider, slideBG, 0, 1, 0, 1);
        slideBG.slider = slider;

        GuiRectangle handle = new GuiRectangle(slider)
                .fill(colour)
                .border(0xFF000000)
                .borderWidth(0.5)
                .constrain(WIDTH, literal(4));

        slider.installSlider(handle)
                .bindSliderWidth();

        return slideBG;
    }

    public GuiColourPicker setBlockOutsideClicks(boolean blockOutsideClicks) {
        this.blockOutsideClicks = blockOutsideClicks;
        return this;
    }

    public GuiColourPicker setCancelOnOutsideClick(boolean closeOnOutsideClick) {
        this.closeOnOutsideClick = closeOnOutsideClick;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean consumed) {
        if (closeOnOutsideClick && !getContentElement().getRectangle().contains(mouseX, mouseY)) {
            cancel();
        }
        return super.mouseClicked(mouseX, mouseY, button, consumed) || blockOutsideClicks;
    }

    public void cancel() {
        if (cancelButton != null && cancelButton.isEnabled()){
            colourState.set(initialColour);
        }
        close();
    }

    public void close() {
        getParent().removeChild(this);
    }

    private static boolean validHex(String value) {
        try {
            Integer.parseUnsignedInt(value, 16);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static class SliderBG extends GuiElement<SliderBG> implements BackgroundRender {
        public int colour;
        public int highlight;
        public GuiSlider slider;
        public boolean pressed = false;

        public SliderBG(@NotNull GuiParent<?> parent, int colour, int highlight) {
            super(parent);
            this.colour = colour;
            this.highlight = highlight;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button, boolean consumed) {
            pressed = button == 0;
            return super.mouseClicked(mouseX, mouseY, button, consumed);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button, boolean consumed) {
            if (button == 0) pressed = false;
            return super.mouseReleased(mouseX, mouseY, button, consumed);
        }

        @Override
        public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            render.fill(xMin(), yMin(), xMin() + 1, yMax(), colour);
            render.fill(xMax() - 1, yMin(), xMax(), yMax(), colour);
            render.fill(xMin() + 1, yCenter() - 0.5, xMax() - 1, yCenter() + 0.5, colour);

            if ((isMouseOver() && !pressed) || slider.isDragging()) {
                render.rect(getRectangle(), highlight);
            }
        }
    }

    public static class ColourPreview extends GuiElement<ColourPreview> implements BackgroundRender {
        private final Supplier<Integer> colour;
        public int colourA = 0xFF999999;
        public int colourB = 0xFF666666;

        public ColourPreview(@NotNull GuiParent<?> parent, Supplier<Integer> colour) {
            super(parent);
            this.colour = colour;
        }

        @Override
        public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            render.pushScissorRect(xMin(), yMin(), xSize(), ySize());
            for (int x = 0; xMin() + (x * 2) < xMax(); x++) {
                for (int y = 0; yMin() + (y * 2) < yMax(); y++) {
                    int col = (y & 1) == 0 ? ((x & 1) == 0 ? colourA : colourB) : ((x & 1) == 0 ? colourB : colourA);
                    render.rect(xMin() + (x * 2), yMin() + (y * 2), 2, 2, col);
                }
            }
            render.popScissor();
            render.rect(getRectangle(), colour.get());
        }
    }

    public GuiColourPicker setColourState(ColourState colourState) {
        this.colourState = colourState;
        return this;
    }

    public ColourState getState() {
        return colourState;
    }

    public SliderState sliderStateAlpha() {
        return SliderState.forSlider(() -> (double) aF(colourState.getColour()), e -> colourState.set(aF(colourState.getColour(), e.floatValue())), () -> -1D / (Screen.hasShiftDown() ? 16 : 64));
    }

    public SliderState sliderStateRed() {
        return SliderState.forSlider(() -> (double) rF(colourState.getColour()), e -> colourState.set(rF(colourState.getColour(), e.floatValue())), () -> -1D / (Screen.hasShiftDown() ? 16 : 64));
    }

    public SliderState sliderStateGreen() {
        return SliderState.forSlider(() -> (double) gF(colourState.getColour()), e -> colourState.set(gF(colourState.getColour(), e.floatValue())), () -> -1D / (Screen.hasShiftDown() ? 16 : 64));
    }

    public SliderState sliderStateBlue() {
        return SliderState.forSlider(() -> (double) bF(colourState.getColour()), e -> colourState.set(bF(colourState.getColour(), e.floatValue())), () -> -1D / (Screen.hasShiftDown() ? 16 : 64));
    }

    public TextState getTextState() {
        return TextState.create(colourState::getHexColour, colourState::setHexColour);
    }

    public GuiButton getOkButton() {
        return okButton;
    }

    /**
     * If cancel button is disabled, ok button will automatically resize.
     */
    public GuiButton getCancelButton() {
        return cancelButton;
    }

    //=== 1.19.2 (WP-10) ===//
    // CCL 4.3.2's Colour has no float accessors (rF()/gF()/bF()/aF() and their setters were added in CCL 1.20) and
    // codechicken.lib.colour is a CCL package, so these local helpers reproduce the CCL 1.20 implementations exactly.

    private static float rF(Colour c) { return (c.r & 0xFF) / 255F; }

    private static float gF(Colour c) { return (c.g & 0xFF) / 255F; }

    private static float bF(Colour c) { return (c.b & 0xFF) / 255F; }

    private static float aF(Colour c) { return (c.a & 0xFF) / 255F; }

    private static Colour rF(Colour c, float r) {
        c.r = (byte) (255F * r);
        return c;
    }

    private static Colour gF(Colour c, float g) {
        c.g = (byte) (255F * g);
        return c;
    }

    private static Colour bF(Colour c, float b) {
        c.b = (byte) (255F * b);
        return c;
    }

    private static Colour aF(Colour c, float a) {
        c.a = (byte) (255F * a);
        return c;
    }

    private static Colour aF(Colour c, int a) {
        c.a = (byte) a;
        return c;
    }
}
