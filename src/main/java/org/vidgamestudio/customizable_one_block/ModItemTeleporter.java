package org.vidgamestudio.customizable_one_block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Customizable_one_block.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModItemTeleporter {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Проверяем, что спавнится именно ПРЕДМЕТ (дроп), и это происходит на сервере
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            Level level = event.getLevel();
            if (level.isClientSide) return;

            // Получаем округленные координаты, где этот предмет пытается появиться
            BlockPos itemPos = itemEntity.blockPosition();
            BlockPos generatorPos = itemPos.below(); // Проверяем блок снизу

            // Проверяем, стоит ли под этим предметом наш генератор
            if (level.getBlockState(generatorPos).is(Customizable_one_block.GENERATOR_BLOCK.get())) {

                // МГНОВЕННАЯ ТЕЛЕПОРТАЦИЯ: двигаем предмет строго в центр над блоком
                itemEntity.moveTo(
                        itemPos.getX() + 0.5,
                        itemPos.getY() + 1.1,
                        itemPos.getZ() + 0.5,
                        0.0F, 0.0F
                );

                // Обнуляем горизонтальную скорость разброса ваниллы, даем легкий импульс вверх
                itemEntity.setDeltaMovement(0, 0.05, 0);
            }
        }
    }
}
