package com.brandon3055.brandonscore.dev.kitchensink;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.mgui.MGuiBootstrap;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entry point of the dev kitchen-sink (PORTING_PLAN §5 WP-10c). The dev source set is part of the brandonscore mod in
 * the run configs ({@code mods { brandonscore { source main; source dev } }}), so this class is found by Forge's
 * {@code @Mod.EventBusSubscriber} scan; nothing in src/main references it.
 * <ul>
 *     <li>registers the menu type {@code brandonscore:dev_kitchensink} (mod bus, {@link RegisterEvent});</li>
 *     <li>registers {@code /bcdev kitchensink} (opens the menu via {@link NetworkHooks#openScreen}) and
 *     {@code /bcdev info} (usable from the server console; reports the menu type and the mgui channel).</li>
 * </ul>
 * The client screen is registered by {@code client.KitchenSinkClient} (Dist.CLIENT only).
 * Dev-only code of the BrandonsCore 1.19.2 port (WP-10c, src/dev): wired into the run configs only, never shipped.
 */
public class BCDevKitchenSink {
    private static final Logger LOGGER = LogManager.getLogger("BCDev");
    public static final ResourceLocation MENU_ID = new ResourceLocation(BrandonsCore.MODID, "dev_kitchensink");
    public static final MenuType<KitchenSinkMenu> MENU_TYPE = IForgeMenuType.create(KitchenSinkMenu::new);
    private static final Map<UUID, KitchenSinkState> STATES = new HashMap<>();

    public static KitchenSinkState stateFor(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new KitchenSinkState());
    }

    @Mod.EventBusSubscriber(modid = BrandonsCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> {
                helper.register(MENU_ID, MENU_TYPE);
                LOGGER.info("[bcdev] registered menu type {}", MENU_ID);
            });
        }
    }

    @Mod.EventBusSubscriber(modid = BrandonsCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            register(event.getDispatcher());
            LOGGER.info("[bcdev] registered command /bcdev (kitchensink, info)");
        }
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bcdev")
                .then(Commands.literal("kitchensink")
                        .executes(ctx -> openKitchenSink(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource())))
        );
    }

    private static int openKitchenSink(ServerPlayer player) {
        KitchenSinkState state = stateFor(player);
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new KitchenSinkMenu(id, inventory, state),
                Component.literal("BrandonsCore mgui kitchen sink")));
        return 1;
    }

    private static int info(CommandSourceStack source) {
        ResourceLocation key = ForgeRegistries.MENU_TYPES.getKey(MENU_TYPE);
        source.sendSuccess(Component.literal("[bcdev] kitchen-sink menu type: " + key
                + "; mgui channel " + MGuiBootstrap.NET_CHANNEL + " registered: " + (MGuiBootstrap.getNetChannel() != null)), false);
        return 1;
    }
}
