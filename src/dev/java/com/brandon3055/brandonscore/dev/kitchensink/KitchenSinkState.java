package com.brandon3055.brandonscore.dev.kitchensink;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Server-side state of the dev kitchen-sink menu (one per player, kept for the lifetime of the server so the 3x3
 * inventory survives closing / reopening the menu). The client side uses a throw-away instance as the slot mirror.
 * <p>
 * Dev-only code of the BrandonsCore 1.19.2 port (WP-10c, src/dev): wired into the run configs only, never shipped.
 */
public class KitchenSinkState {
    /** 3x3 inventory (Forge {@link ItemStackHandler}, shown through mgui {@code ModularSlot}s). */
    public final ItemStackHandler items = new ItemStackHandler(9);
    /** Energy shown by the {@code GuiEnergyBar}; changed by the "+10k RF" button (client to server packet). */
    public int energy = 25_000;
    /** Last JEI ghost-dropped stack (client to server packet). */
    public ItemStack ghost = ItemStack.EMPTY;
    /** Last text submitted from the text field (client to server packet, echoed back server to client). */
    public String lastText = "";
}
