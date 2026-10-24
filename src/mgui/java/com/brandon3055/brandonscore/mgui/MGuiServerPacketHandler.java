package com.brandon3055.brandonscore.mgui;

import codechicken.lib.inventory.container.modular.ModularGuiContainerMenu;
import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import static com.brandon3055.brandonscore.mgui.MGuiBootstrap.S_GUI_SYNC;

/**
 * Server side handler of the {@code brandonscore:mgui} channel.
 * Port of CCL 1.20 {@code codechicken.lib.internal.network.ServerPacketHandler} (its only branch is GUI_SYNC).
 * <p>
 * Part of the LGPL-2.1 mgui tree (see src/mgui/LICENSE-CodeChickenLib.txt). Written for the BrandonsCore 1.19.2 port (WP-10b).
 */
public class MGuiServerPacketHandler implements ICustomPacketHandler.IServerPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, ServerPlayer sender, ServerGamePacketListenerImpl handler) {
        switch (packet.getType()) {
            case S_GUI_SYNC -> ModularGuiContainerMenu.handlePacketFromClient(sender, packet);
        }
    }
}
