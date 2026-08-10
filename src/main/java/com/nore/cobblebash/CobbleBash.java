package com.nore.cobblebash;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.block.TrainingSimulatorBlock;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.event.GymEventHandler;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.item.TrainingDiskItem;
import com.nore.cobblebash.stats.CobbleBashStats;
import com.nore.cobblebash.util.DelayedTaskScheduler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Point d'entree Fabric.
 *
 * <p>NeoForge differe l'enregistrement via {@code DeferredRegister} parce que
 * ses registres ne sont ouverts que pendant un evenement precis. Fabric appelle
 * {@link #onInitialize()} pendant que les registres sont encore modifiables :
 * on enregistre donc directement, dans l'ordre ou les objets dependent les uns
 * des autres (bloc, puis son item, puis l'onglet qui les affiche).
 */
public class CobbleBash implements ModInitializer {
    public static final String MODID = "cobblebash";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static TrainingSimulatorBlock TRAINING_SIMULATOR;
    public static BlockItem TRAINING_SIMULATOR_ITEM;
    public static Map<GymType, TrainingDiskItem> TRAINING_DISKS = Collections.emptyMap();
    public static CreativeModeTab COBBLEBASH_TAB;

    @Override
    public void onInitialize() {
        Config.load();

        registerBlocksAndItems();
        registerCreativeTab();
        CobbleBashCriteriaTriggers.bootstrap();
        CobbleBashStats.bootstrap();

        DelayedTaskScheduler.register();
        GymEventHandler.register();

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> GymCommand.register(dispatcher));

        // L'original branche RCT depuis FMLCommonSetupEvent. Sur Fabric, attendre
        // le demarrage du serveur est plus sur : RCT a forcement fini de
        // s'initialiser, et l'appel est idempotent.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            RctApiProbe.logLoaded();
            GymEventHandler.registerRctListeners();
        });

        LOGGER.info("CobbleBash (Fabric) initialise.");
    }

    private static void registerBlocksAndItems() {
        TRAINING_SIMULATOR = Registry.register(
                BuiltInRegistries.BLOCK,
                id("training_simulator"),
                new TrainingSimulatorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.5F))
        );
        TRAINING_SIMULATOR_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                id("training_simulator"),
                new BlockItem(TRAINING_SIMULATOR, new Item.Properties())
        );

        Map<GymType, TrainingDiskItem> disks = new EnumMap<>(GymType.class);
        for (GymType type : GymType.values()) {
            disks.put(type, Registry.register(
                    BuiltInRegistries.ITEM,
                    id(type.getId() + "_training_disk"),
                    new TrainingDiskItem(type, new Item.Properties().stacksTo(16))
            ));
        }
        TRAINING_DISKS = Collections.unmodifiableMap(disks);
    }

    private static void registerCreativeTab() {
        COBBLEBASH_TAB = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                id("cobblebash"),
                // `withTabsBefore` est une extension NeoForge du constructeur
                // d'onglet : en vanilla, l'ordre suit celui de l'enregistrement.
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.cobblebash"))
                        .icon(() -> TRAINING_SIMULATOR_ITEM.getDefaultInstance())
                        .displayItems((parameters, output) -> {
                            output.accept(TRAINING_SIMULATOR_ITEM);
                            TRAINING_DISKS.values().forEach(output::accept);
                        })
                        .build()
        );

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(entries -> entries.accept(TRAINING_SIMULATOR_ITEM));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
