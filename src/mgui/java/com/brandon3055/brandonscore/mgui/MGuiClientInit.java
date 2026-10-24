package com.brandon3055.brandonscore.mgui;

import codechicken.lib.gui.modular.lib.CursorHelper;
import codechicken.lib.gui.modular.sprite.CCGuiTextures;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Client only part of {@link MGuiBootstrap} (kept in its own class so that common code never loads client classes).
 * Port of the mgui part of CCL 1.20 {@code codechicken.lib.internal.ClientInit} ({@code onResourceReload}).
 * <p>
 * Part of the LGPL-2.1 mgui tree (see src/mgui/LICENSE-CodeChickenLib.txt). Written for the BrandonsCore 1.19.2 port (WP-10b).
 */
final class MGuiClientInit {

    private MGuiClientInit() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(MGuiClientInit::onResourceReload);
    }

    private static void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(CCGuiTextures.getAtlasHolder());
        event.registerReloadListener((ResourceManagerReloadListener) e -> CursorHelper.onResourceReload());
    }
}
