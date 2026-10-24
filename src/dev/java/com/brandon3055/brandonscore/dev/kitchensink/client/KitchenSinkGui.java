package com.brandon3055.brandonscore.dev.kitchensink.client;

import codechicken.lib.gui.modular.ModularGui;
import codechicken.lib.gui.modular.ModularGuiContainer;
import codechicken.lib.gui.modular.elements.*;
import codechicken.lib.gui.modular.lib.Constraints;
import codechicken.lib.gui.modular.lib.container.ContainerGuiProvider;
import codechicken.lib.gui.modular.lib.container.ContainerScreenAccess;
import codechicken.lib.gui.modular.lib.geometry.Align;
import codechicken.lib.gui.modular.lib.geometry.Axis;
import codechicken.lib.gui.modular.sprite.CCGuiTextures;
import com.brandon3055.brandonscore.dev.kitchensink.KitchenSinkMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

import static codechicken.lib.gui.modular.lib.Constraints.LayoutPos.*;
import static codechicken.lib.gui.modular.lib.geometry.Constraint.*;
import static codechicken.lib.gui.modular.lib.geometry.GeoParam.*;

/**
 * Client screen of the dev kitchen-sink (PORTING_PLAN §5 WP-10c). One mgui container screen with:
 * draggable root ({@link GuiManipulable}, cursors) on the mgui "gui_vanilla" dynamic texture; player inventory +
 * 3x3 machine {@link GuiSlots} (slot item tooltips, quick-move); {@link GuiEnergyBar} and {@link GuiFluidTank} with the
 * mgui lang keys ({@code ccl.energy_bar.*}, {@code ccl.fluid_tank.*}); a DataSync int label; a vanilla button that
 * sends a packet; a {@link GuiTextField} whose text is echoed back by the server; an enchanted {@link GuiItemStack}
 * with its item tooltip; a scrolling {@link GuiList} with a {@link GuiSlider} scroll bar; a {@link GuiEntityRenderer}
 * of the local player; a JEI exclusion zone (side panel, {@code jeiExclude()}) and a JEI ghost-drop target
 * ({@code setJeiDropTarget}).
 * Dev-only code of the BrandonsCore 1.19.2 port (WP-10c, src/dev): never shipped.
 */
public class KitchenSinkGui extends ContainerGuiProvider<KitchenSinkMenu> {
    public static final int GUI_WIDTH = 280;
    public static final int GUI_HEIGHT = 210;

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(4).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), CCGuiTextures.get("dynamic/gui_vanilla")).dynamicTexture();
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<KitchenSinkMenu> screenAccess) {
        gui.initStandardGui(GUI_WIDTH, GUI_HEIGHT);
        KitchenSinkMenu menu = screenAccess.getMenu();
        GuiElement<?> root = gui.getRoot();

        GuiText title = new GuiText(root, gui::getGuiTitle)
                .setAlignment(Align.LEFT)
                .setShadow(false)
                .setTextColour(0x404040)
                .constrain(TOP, relative(root.get(TOP), 6))
                .constrain(LEFT, relative(root.get(LEFT), 8))
                .constrain(RIGHT, relative(root.get(RIGHT), -8))
                .constrain(HEIGHT, literal(9));

        //=== Slots: 3x3 machine inventory (zone 1) + player inventory (zone 0) ===//
        GuiSlots machine = new GuiSlots(root, screenAccess, menu.machine, 3);
        Constraints.placeInside(machine, root, TOP_LEFT, 8, 18);

        var playerInv = GuiSlots.player(root, screenAccess, menu.main, menu.hotBar);
        Constraints.placeInside(playerInv.container(), root, BOTTOM_LEFT, 8, -7);
        new GuiText(root, Component.translatable("container.inventory"))
                .setAlignment(Align.LEFT)
                .setShadow(false)
                .setTextColour(0x404040)
                .constrain(BOTTOM, relative(playerInv.container().get(TOP), -2))
                .constrain(LEFT, match(playerInv.container().get(LEFT)))
                .constrain(WIDTH, literal(100))
                .constrain(HEIGHT, literal(9));

        //=== Energy bar + fluid tank (DataSync int / FluidStack, mgui lang keys in the tooltips) ===//
        var energyBar = GuiEnergyBar.simpleBar(root);
        energyBar.bar()
                .setCapacity(KitchenSinkMenu.ENERGY_CAPACITY)
                .setEnergy(() -> (long) menu.energy.get());
        Constraints.size(energyBar.container(), 14, 54);
        Constraints.placeOutside(energyBar.container(), machine, MIDDLE_RIGHT, 6, 0);

        var tank = GuiFluidTank.simpleTank(root);
        tank.tank()
                .setCapacity((long) KitchenSinkMenu.TANK_CAPACITY)
                .setFluidStack(() -> menu.fluid.get());
        Constraints.size(tank.container(), 18, 54);
        Constraints.placeOutside(tank.container(), energyBar.container(), MIDDLE_RIGHT, 4, 0);

        //=== Middle column: button, DataSync label, text field + echo ===//
        GuiButton addEnergy = GuiButton.vanillaAnimated(root, Component.literal("+10k RF"),
                        () -> menu.sendPacketToServer(KitchenSinkMenu.PKT_ADD_ENERGY, buf -> {}))
                .setTooltip(Component.literal("Client -> server packet; the energy bar updates through DataSync"));
        addEnergy.constrain(TOP, match(tank.container().get(TOP)))
                .constrain(LEFT, relative(tank.container().get(RIGHT), 6))
                .constrain(WIDTH, literal(60))
                .constrain(HEIGHT, literal(14));

        GuiText ticks = new GuiText(root, () -> Component.literal("DataSync int: " + menu.ticks.get()))
                .setAlignment(Align.LEFT)
                .setShadow(false)
                .setTextColour(0x404040)
                .constrain(TOP, relative(addEnergy.get(BOTTOM), 4))
                .constrain(LEFT, match(addEnergy.get(LEFT)))
                .constrain(WIDTH, literal(100))
                .constrain(HEIGHT, literal(9));

        var textField = GuiTextField.create(root, 0xFF000000, 0xFFA0A0A0, 0xE0E0E0);
        textField.field()
                .setSuggestion(Component.literal("type, then Enter"))
                .setMaxLength(64)
                .setEnterPressed(() -> {
                    String value = textField.field().getValue();
                    menu.sendPacketToServer(KitchenSinkMenu.PKT_TEXT, buf -> buf.writeString(value));
                });
        textField.container()
                .constrain(TOP, relative(ticks.get(BOTTOM), 3))
                .constrain(LEFT, match(addEnergy.get(LEFT)))
                .constrain(WIDTH, literal(100))
                .constrain(HEIGHT, literal(14));

        new GuiText(root, () -> Component.literal(menu.lastEcho))
                .setAlignment(Align.LEFT)
                .setShadow(false)
                .setTextColour(0x204080)
                .constrain(TOP, relative(textField.container().get(BOTTOM), 3))
                .constrain(LEFT, match(addEnergy.get(LEFT)))
                .constrain(WIDTH, literal(100))
                .constrain(HEIGHT, literal(9));

        //=== Item stack with tooltip + JEI ghost drop target (under the machine slots) ===//
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 5);
        sword.setHoverName(Component.literal("Kitchen-sink sword"));
        GuiRectangle swordBg = GuiRectangle.vanillaSlot(root);
        Constraints.size(swordBg, 18, 18);
        Constraints.placeOutside(swordBg, machine, BOTTOM_LEFT, 0, 6);
        GuiItemStack swordStack = new GuiItemStack(swordBg, sword).enableStackToolTip(true);
        Constraints.bind(swordStack, swordBg, 1);

        GuiRectangle ghostBg = GuiRectangle.vanillaSlot(root);
        Constraints.size(ghostBg, 18, 18);
        Constraints.placeOutside(ghostBg, swordBg, MIDDLE_RIGHT, 4, 0);
        GuiItemStack ghost = new GuiItemStack(ghostBg, () -> menu.ghostFilter);
        Constraints.bind(ghost, ghostBg, 1);
        ghostBg.setJeiDropTarget(stack -> {
            menu.ghostFilter = stack.copy();
            menu.sendPacketToServer(KitchenSinkMenu.PKT_GHOST, buf -> buf.writeItemStack(stack));
        }, true);
        ghostBg.setTooltip(Component.literal("JEI ghost-drop target: drag an item from the JEI list here"));

        //=== Scrolling list with a scroll bar (top right) ===//
        GuiRectangle listBg = GuiRectangle.vanillaSlot(root)
                .constrain(TOP, relative(root.get(TOP), 18))
                .constrain(RIGHT, relative(root.get(RIGHT), -8))
                .constrain(WIDTH, literal(78))
                .constrain(HEIGHT, literal(80));
        GuiList<String> list = new GuiList<String>(listBg)
                .setItemSpacing(1)
                .constrain(TOP, relative(listBg.get(TOP), 2))
                .constrain(LEFT, relative(listBg.get(LEFT), 2))
                .constrain(BOTTOM, relative(listBg.get(BOTTOM), -2))
                .constrain(RIGHT, relative(listBg.get(RIGHT), -7));
        for (int i = 1; i <= 40; i++) list.add("List entry " + i);
        list.setDisplayBuilder((parent, entry) -> {
            GuiText text = new GuiText(parent, Component.literal(entry))
                    .setAlignment(Align.LEFT)
                    .setShadow(false)
                    .setTextColour(0xFFFFFF)
                    .constrain(HEIGHT, literal(9));
            text.setTooltip(Component.literal("Tooltip of " + entry));
            return text;
        });
        GuiSlider scrollBar = new GuiSlider(listBg, Axis.Y)
                .setSliderState(list.scrollState())
                .setScrollableElement(list)
                .constrain(TOP, match(list.get(TOP)))
                .constrain(LEFT, relative(list.get(RIGHT), 1))
                .constrain(BOTTOM, match(list.get(BOTTOM)))
                .constrain(WIDTH, literal(4));
        Constraints.bind(new GuiRectangle(scrollBar).fill(0x40FFFFFF), scrollBar);
        scrollBar.installSlider(new GuiRectangle(scrollBar).fill(0xA0FFFFFF))
                .bindSliderLength()
                .bindSliderWidth();

        //=== Entity renderer (local player, follows the mouse) ===//
        GuiRectangle entityBg = GuiRectangle.invertedSlot(root)
                .constrain(TOP, relative(listBg.get(BOTTOM), 6))
                .constrain(RIGHT, relative(root.get(RIGHT), -8))
                .constrain(WIDTH, literal(78))
                .constrain(BOTTOM, relative(root.get(BOTTOM), -7));
        GuiEntityRenderer entity = new GuiEntityRenderer(entityBg)
                .setEntity(Minecraft.getInstance().player)
                .setTrackMouse(true);
        Constraints.bind(entity, entityBg, 4);

        //=== JEI exclusion zone: a panel outside the gui on the right ===//
        GuiRectangle side = GuiRectangle.toolTipBackground(root)
                .jeiExclude()
                .constrain(TOP, match(root.get(TOP)))
                .constrain(LEFT, relative(root.get(RIGHT), 2))
                .constrain(WIDTH, literal(72))
                .constrain(HEIGHT, literal(60));
        new GuiText(side, Component.literal("JEI exclusion zone: JEI must not draw here"))
                .setWrap(true)
                .setTextColour(0xFFFFFF)
                .constrain(TOP, relative(side.get(TOP), 4))
                .constrain(LEFT, relative(side.get(LEFT), 4))
                .constrain(RIGHT, relative(side.get(RIGHT), -4))
                .constrain(BOTTOM, relative(side.get(BOTTOM), -4));
        side.setTooltip(List.of(Component.literal("jeiExclude()"), Component.literal("ModularGui#getJeiExclusions")));
    }

    public static class Screen extends ModularGuiContainer<KitchenSinkMenu> {
        public Screen(KitchenSinkMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, new KitchenSinkGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
