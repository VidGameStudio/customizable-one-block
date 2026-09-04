package org.vidgamestudio.customizable_one_block;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Customizable_one_block.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModItemColors {

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 1) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("TargetCategory")) {
                    String category = tag.getString("TargetCategory");
                    ModConfig.Category configCat = ModConfig.INSTANCE.categories.get(category);

                    if (configCat != null && configCat.color != null) {
                        try {
                            String hexColor = configCat.color.replace("#", "");
                            return Integer.parseInt(hexColor, 16);
                        } catch (Exception e) {
                            return 0xFFFFFF;
                        }
                    }
                }
                return 0xFFFFFF;
            }
            return -1;
        }, Customizable_one_block.MOD_UPGRADE.get());
    }
}
