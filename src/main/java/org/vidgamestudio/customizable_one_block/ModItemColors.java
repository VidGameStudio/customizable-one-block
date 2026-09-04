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
        // Регистрируем обработчик цвета для нашего предмета улучшения
        event.register((stack, tintIndex) -> {
            // tintIndex — это номер слоя из JSON модели.
            // layer0 (основа) имеет индекс 0, layer1 (оверлей) имеет индекс 1.
            // Мы красим ТОЛЬКО слой 1 (оверлей)! Слой 0 оставляем нетронутым (-1).
            if (tintIndex == 1) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("TargetCategory")) {
                    String category = tag.getString("TargetCategory");
                    ModConfig.Category configCat = ModConfig.INSTANCE.categories.get(category);

                    if (configCat != null && configCat.color != null) {
                        try {
                            // Декодируем HEX строку типа "#7248B6" или "0x7248B6" в RGB число
                            String hexColor = configCat.color.replace("#", "");
                            return Integer.parseInt(hexColor, 16);
                        } catch (Exception e) {
                            // Если админ написал в конфиге кривой HEX, красим в белый (дефолт)
                            return 0xFFFFFF;
                        }
                    }
                }
                // Если у карточки нет тега мода, красим маску в дефолтный белый
                return 0xFFFFFF;
            }

            // Возвращаем -1, чтобы не красить основу карточки (layer0)
            return -1;
        }, Customizable_one_block.MOD_UPGRADE.get());
    }
}
