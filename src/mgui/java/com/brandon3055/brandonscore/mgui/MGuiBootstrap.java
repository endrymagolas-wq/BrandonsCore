package com.brandon3055.brandonscore.mgui;

import codechicken.lib.packet.PacketCustomChannelBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.event.EventNetworkChannel;

/**
 * Glue between BrandonsCore and the embedded 1.19.2 backport of CodeChickenLib's Modular GUI ("mgui", src/mgui).
 * <p>
 * In CCL 1.20 these hooks live in CCL itself (CCLNetwork GUI_SYNC packets, internal ClientInit reload listeners).
 * BrandonsCore 1.19.2 runs on the stock CCL 4.3.2, so it must call:
 * <ul>
 *     <li>{@link #initCommon()} once on both sides during mod construction (registers the {@code brandonscore:mgui}
 *     channel used by {@link codechicken.lib.inventory.container.modular.ModularGuiContainerMenu} for GUI sync);</li>
 *     <li>{@link #initClient(IEventBus)} once on the client during mod construction, with the mod event bus
 *     (registers the mgui texture atlas and cursor reload listeners).</li>
 * </ul>
 * Part of the LGPL-2.1 mgui tree (see src/mgui/LICENSE-CodeChickenLib.txt). Written for the BrandonsCore 1.19.2 port (WP-10b).
 */
public final class MGuiBootstrap {

    /** Replaces CCL 1.20's {@code CCLNetwork.NET_CHANNEL} ("ccl:internal") for mgui GUI sync. */
    public static final ResourceLocation NET_CHANNEL = new ResourceLocation("brandonscore", "mgui");

    /** Client handled: container GUI sync (CCL 1.20 {@code CCLNetwork.C_GUI_SYNC}, same id). */
    public static final int C_GUI_SYNC = 20;

    /** Server handled: container GUI sync (CCL 1.20 {@code CCLNetwork.S_GUI_SYNC}, same id). */
    public static final int S_GUI_SYNC = 20;

    private static EventNetworkChannel netChannel;
    private static boolean clientInitDone = false;

    private MGuiBootstrap() {
    }

    /**
     * Registers the {@code brandonscore:mgui} network channel (client and server handlers). Call once during mod
     * construction on both sides. Subsequent calls are ignored.
     */
    public static synchronized void initCommon() {
        if (netChannel != null) return;
        netChannel = PacketCustomChannelBuilder.named(NET_CHANNEL)
                .assignClientHandler(() -> MGuiClientPacketHandler::new)
                .assignServerHandler(() -> MGuiServerPacketHandler::new)
                .build();
    }

    /**
     * Client only. Registers the reload listeners that CCL 1.20 registers in its internal ClientInit:
     * the mgui GUI texture atlas ({@link codechicken.lib.gui.modular.sprite.CCGuiTextures#getAtlasHolder()}) and
     * {@link codechicken.lib.gui.modular.lib.CursorHelper#onResourceReload()} (recreates the GLFW cursors).
     *
     * @param modBus the mod event bus (RegisterClientReloadListenersEvent is a mod bus event).
     */
    public static synchronized void initClient(IEventBus modBus) {
        if (clientInitDone) return;
        clientInitDone = true;
        MGuiClientInit.init(modBus);
    }

    /**
     * @return the mgui network channel, or null if {@link #initCommon()} has not been called yet.
     */
    public static EventNetworkChannel getNetChannel() {
        return netChannel;
    }
}
