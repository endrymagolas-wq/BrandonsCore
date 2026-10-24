package com.brandon3055.brandonscore.mgui;

import codechicken.lib.inventory.container.modular.ModularGuiContainerMenu;
import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import static com.brandon3055.brandonscore.mgui.MGuiBootstrap.C_GUI_SYNC;

/**
 * Client side handler of the {@code brandonscore:mgui} channel.
 * Port of the GUI_SYNC branch of CCL 1.20 {@code codechicken.lib.internal.network.ClientPacketHandler}.
 * Only ever instantiated on the physical client (see {@link MGuiBootstrap#initCommon()}).
 * <p>
 * Part of the LGPL-2.1 mgui tree (see src/mgui/LICENSE-CodeChickenLib.txt). Written for the BrandonsCore 1.19.2 port (WP-10b).
 */
public class MGuiClientPacketHandler implements ICustomPacketHandler.IClientPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, ClientPacketListener handler) {
        switch (packet.getType()) {
            case C_GUI_SYNC -> ModularGuiContainerMenu.handlePacketFromServer(mc.player, packet);
        }
    }
}
