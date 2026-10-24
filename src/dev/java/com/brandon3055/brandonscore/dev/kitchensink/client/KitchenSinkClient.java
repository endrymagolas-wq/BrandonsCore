package com.brandon3055.brandonscore.dev.kitchensink.client;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.dev.kitchensink.BCDevKitchenSink;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client half of the dev kitchen-sink: binds the menu type to {@link KitchenSinkGui.Screen}.
 * Physical client only ({@code value = Dist.CLIENT}), so the dedicated server never loads any client class of the
 * kitchen-sink. Dev-only code of the BrandonsCore 1.19.2 port (WP-10c, src/dev): never shipped.
 */
@Mod.EventBusSubscriber(modid = BrandonsCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KitchenSinkClient {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(BCDevKitchenSink.MENU_TYPE, KitchenSinkGui.Screen::new));
    }
}
