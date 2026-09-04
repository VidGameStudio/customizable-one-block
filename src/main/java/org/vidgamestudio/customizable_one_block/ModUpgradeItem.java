package org.vidgamestudio.customizable_one_block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class ModUpgradeItem extends Item {
    public ModUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("TargetCategory")) {
            return Component.translatable("item.customizable_one_block.mod_upgrade.titled", tag.getString("TargetCategory"));
        }
        return Component.translatable("item.customizable_one_block.mod_upgrade.empty");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        var clickedPos = context.getClickedPos();
        var generatorPos = clickedPos.below();

        if (level.getBlockState(generatorPos).is(Customizable_one_block.GENERATOR_BLOCK.get())) {
            ItemStack stack = context.getItemInHand();
            CompoundTag tag = stack.getTag();

            if (tag != null && tag.contains("TargetCategory")) {
                String targetCategory = tag.getString("TargetCategory");

                if (ModConfig.INSTANCE.categories.containsKey(targetCategory)) {
                    if (level.getBlockEntity(generatorPos) instanceof GeneratorBlockEntity generatorBE) {
                        generatorBE.setActiveCategory(targetCategory);

                        if (context.getPlayer() != null) {
                            int broken = generatorBE.getBlocksBroken(targetCategory);
                            context.getPlayer().sendSystemMessage(Component.literal("Генератор переключен на: " + targetCategory + " (Сломано блоков: " + broken + ")"));
                        }
                        return InteractionResult.CONSUME;
                    }
                }
            }
        } else {
            if (level.getBlockEntity(generatorPos) instanceof GeneratorBlockEntity generatorBE) {
                String targetCategory = generatorBE.getActiveCategory();
                generatorBE.setActiveCategory(targetCategory);

                if (context.getPlayer() != null) {
                    int broken = generatorBE.getBlocksBroken(targetCategory);
                    context.getPlayer().sendSystemMessage(Component.literal("Генератор сейчас на: " + targetCategory + " (Сломано блоков: " + broken + ")"));
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }
}