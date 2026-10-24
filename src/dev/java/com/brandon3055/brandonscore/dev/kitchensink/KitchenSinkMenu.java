package com.brandon3055.brandonscore.dev.kitchensink;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.gui.modular.lib.container.DataSync;
import codechicken.lib.gui.modular.lib.container.SlotGroup;
import codechicken.lib.inventory.container.data.FluidData;
import codechicken.lib.inventory.container.data.IntData;
import codechicken.lib.inventory.container.modular.ModularGuiContainerMenu;
import codechicken.lib.inventory.container.modular.ModularSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Kitchen-sink container for the mgui backport (PORTING_PLAN §5 WP-10c): exercises every container-side part of
 * CodeChickenLib's Modular GUI that DE relies on, on a dedicated server as well as on the integrated one.
 * <ul>
 *     <li>player inventory (main + hot bar) and a 3x3 Forge {@code ItemStackHandler} on {@link ModularSlot}s in {@link SlotGroup}s;</li>
 *     <li>quick-move (shift click) both ways: player zone 0 to machine zone 1 and back;</li>
 *     <li>{@link DataSync} of an {@code int} (game time) and of a {@link FluidStack} (cycling water), plus the energy
 *     value, all over the {@code brandonscore:mgui} channel (packet id 255);</li>
 *     <li>custom client to server packets ({@link #PKT_TEXT}, {@link #PKT_ADD_ENERGY}, {@link #PKT_GHOST}) and a
 *     server to client packet ({@link #PKT_ECHO}).</li>
 * </ul>
 * Dev-only code of the BrandonsCore 1.19.2 port (WP-10c, src/dev): wired into the run configs only, never shipped.
 */
public class KitchenSinkMenu extends ModularGuiContainerMenu {
    private static final Logger LOGGER = LogManager.getLogger("BCDev");

    public static final long ENERGY_CAPACITY = 100_000L;
    public static final int TANK_CAPACITY = 16_000;

    /** client to server: text field submitted (String) */
    public static final int PKT_TEXT = 1;
    /** client to server: "+10k RF" button */
    public static final int PKT_ADD_ENERGY = 2;
    /** client to server: JEI ghost ingredient dropped (ItemStack) */
    public static final int PKT_GHOST = 3;
    /** server to client: echo of the submitted text (String) */
    public static final int PKT_ECHO = 1;

    public final SlotGroup main = createSlotGroup(0, 1);
    public final SlotGroup hotBar = createSlotGroup(0, 1);
    public final SlotGroup machine = createSlotGroup(1, 0);

    public final DataSync<Integer> ticks;
    public final DataSync<Integer> energy;
    public final DataSync<FluidStack> fluid;

    public final KitchenSinkState state;

    /** Client only: last echo received from the server. */
    public String lastEcho = "";
    /** Client only: last ghost-dropped stack (shown in the drop target). */
    public ItemStack ghostFilter = ItemStack.EMPTY;

    /** Client side constructor ({@code IForgeMenuType} factory). */
    public KitchenSinkMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, new KitchenSinkState());
    }

    public KitchenSinkMenu(int containerId, Inventory inventory, KitchenSinkState state) {
        super(BCDevKitchenSink.MENU_TYPE, containerId, inventory);
        this.state = state;
        main.addPlayerMain(inventory);
        hotBar.addPlayerBar(inventory);
        machine.addSlots(9, 0, index -> new ModularSlot(state.items, index));

        Player player = inventory.player;
        ticks = new DataSync<>(this, new IntData(), () -> (int) (player.level.getGameTime() % 1_000_000L));
        energy = new DataSync<>(this, new IntData(), () -> state.energy);
        fluid = new DataSync<>(this, new FluidData(), () -> new FluidStack(Fluids.WATER, (int) ((player.level.getGameTime() * 50L) % (TANK_CAPACITY + 1))));
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void handlePacketFromClient(Player player, int packetId, MCDataInput packet) {
        switch (packetId) {
            case PKT_TEXT -> {
                String text = packet.readString();
                state.lastText = text;
                LOGGER.info("[bcdev] kitchen-sink text field from {}: '{}'", player.getGameProfile().getName(), text);
                sendPacketToClient(PKT_ECHO, buf -> buf.writeString("server echo: " + text));
            }
            case PKT_ADD_ENERGY -> {
                state.energy = state.energy + 10_000 > ENERGY_CAPACITY ? 0 : state.energy + 10_000;
                LOGGER.info("[bcdev] kitchen-sink energy of {} -> {}", player.getGameProfile().getName(), state.energy);
            }
            case PKT_GHOST -> {
                state.ghost = packet.readItemStack();
                LOGGER.info("[bcdev] kitchen-sink JEI ghost drop from {}: {}", player.getGameProfile().getName(), state.ghost);
            }
            default -> LOGGER.warn("[bcdev] kitchen-sink: unknown client packet id {}", packetId);
        }
    }

    @Override
    public void handlePacketFromServer(Player player, int packetId, MCDataInput packet) {
        super.handlePacketFromServer(player, packetId, packet); //DataSync (id 255)
        if (packetId == PKT_ECHO) {
            lastEcho = packet.readString();
        }
    }
}
