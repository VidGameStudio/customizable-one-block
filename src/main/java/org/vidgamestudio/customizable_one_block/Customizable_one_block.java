package org.vidgamestudio.customizable_one_block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Customizable_one_block.MODID)
public class Customizable_one_block {
    public static final String MODID = "customizable_one_block";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Block> GENERATOR_BLOCK = BLOCKS.register("generator_block",
            () -> new GeneratorBlock(BlockBehaviour.Properties.copy(Blocks.BARRIER).noOcclusion()));

    public static final RegistryObject<net.minecraft.world.item.Item> GENERATOR_ITEM = ITEMS.register("generator_block",
            () -> new net.minecraft.world.item.BlockItem(GENERATOR_BLOCK.get(), new net.minecraft.world.item.Item.Properties()));


    public static final RegistryObject<BlockEntityType<GeneratorBlockEntity>> GENERATOR_BE = BLOCK_ENTITIES.register("generator_be",
            () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, GENERATOR_BLOCK.get()).build(null));


    public static final RegistryObject<Item> MOD_UPGRADE = ITEMS.register("mod_upgrade",
            () -> new ModUpgradeItem(new Item.Properties().stacksTo(1)));


    public Customizable_one_block() {
        ModConfig.load();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(new BlockBreakHandler());

    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Customizable_one_block.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientEvents {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRegisterAdditionalModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
            event.register(new net.minecraft.client.resources.model.ModelResourceLocation(
                    new ResourceLocation(Customizable_one_block.MODID, "upgrade_card"), "inventory"));
        }
    }

}
