package org.vidgamestudio.customizable_one_block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockBreakHandler {
    private final Random random = new Random();

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos brokenPos = event.getPos();
        BlockPos generatorPos = brokenPos.below();

        if (level.getBlockState(generatorPos).is(Customizable_one_block.GENERATOR_BLOCK.get())) {
            if (level.getBlockEntity(generatorPos) instanceof GeneratorBlockEntity generatorBE) {
                event.setCanceled(true);

                BlockState currentState = level.getBlockState(brokenPos);
                net.minecraft.world.entity.player.Player player = event.getPlayer();
                ItemStack mainHandItem = player.getMainHandItem();

                boolean isFake = player instanceof FakePlayer;

                if (!isFake && !player.isCreative() && !mainHandItem.isEmpty() && mainHandItem.isDamageableItem()) {
                    mainHandItem.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND));
                }

                if (level.getBlockEntity(brokenPos) instanceof ChestBlockEntity chest) { // Дроп сундука
                    for (int i = 0; i < chest.getContainerSize(); i++) {
                        ItemStack stack = chest.getItem(i);
                        if (!stack.isEmpty()) spawnItemStrictlyAbove(level, brokenPos, stack.copy());
                    }
                    chest.clearContent();
                }

                if (isFake || !player.isCreative()) { // зачарования
                    boolean canHarvest = !currentState.requiresCorrectToolForDrops() || mainHandItem.isCorrectToolForDrops(currentState);

                    if (canHarvest) {
                        var lootParams = new net.minecraft.world.level.storage.loot.LootParams.Builder((ServerLevel) level)
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(brokenPos))
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, mainHandItem)
                                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, player);

                        for (ItemStack drop : currentState.getDrops(lootParams)) {
                            int fortuneLvl = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, mainHandItem);
                            if (fortuneLvl > 0 && currentState.requiresCorrectToolForDrops() && !EnchantmentHelper.hasSilkTouch(mainHandItem)) {
                                int multiplier = random.nextInt(fortuneLvl + 1) + 1;
                                drop.setCount(drop.getCount() * multiplier);
                            }
                            spawnItemStrictlyAbove(level, brokenPos, drop);
                        }
                    }
                }

                if (!isFake && player instanceof ServerPlayer serverPlayer) { // Таб
                    generatorBE.incrementBlocks(serverPlayer);
                } else {
                    generatorBE.incrementBlocksInternal();
                }

                String categoryName = generatorBE.getActiveCategory();
                if (categoryName == null || !ModConfig.INSTANCE.categories.containsKey(categoryName)) {
                    categoryName = "minecraft";
                    generatorBE.setActiveCategory("minecraft");
                }
                int score = generatorBE.getBlocksBroken(categoryName);

                ModConfig.Category configCat = ModConfig.INSTANCE.categories.get(categoryName); // Выбор фазы
                ModConfig.Phase activePhase = null;
                if (configCat != null && configCat.phases != null) {
                    for (ModConfig.Phase p : configCat.phases) {
                        if (score >= p.blocks_required) {
                            if (activePhase == null || p.blocks_required > activePhase.blocks_required) {
                                activePhase = p;
                            }
                        }
                    }
                }

                if (activePhase == null || activePhase.blocks == null || activePhase.blocks.isEmpty()) {
                    level.setBlock(brokenPos, Blocks.STONE.defaultBlockState(), 3);
                    return;
                }

                String selectedBlockId = getRandomWeightedItem(activePhase.blocks); // Систама весов
                Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(selectedBlockId));
                BlockState nextState = (block != null && block != Blocks.AIR) ? block.defaultBlockState() : Blocks.STONE.defaultBlockState();

                level.setBlock(brokenPos, nextState, 3);

                if (activePhase.monsters != null && !activePhase.monsters.isEmpty() && random.nextInt(100) < 8) { // Спавн мобов
                    String selectedMobId = getRandomWeightedItem(activePhase.monsters);
                    var entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(selectedMobId));
                    if (entityType != null) {
                        net.minecraft.world.entity.Entity entity = entityType.create(level);
                        if (entity != null) {
                            entity.moveTo(brokenPos.getX() + 0.5, brokenPos.getY() + 1.2, brokenPos.getZ() + 0.5, random.nextFloat() * 360F, 0.0F);
                            level.addFreshEntity(entity);
                        }
                    }
                }

                if (nextState.is(Blocks.CHEST)) { // Напонение сундука
                    if (level.getBlockEntity(brokenPos) instanceof ChestBlockEntity chestEntity) {
                        if (activePhase.loot_chest != null) {
                            for (String lootEntry : activePhase.loot_chest) {
                                try {
                                    int lastColon = lootEntry.lastIndexOf(':');
                                    if (lastColon == -1) continue;
                                    String leftPart = lootEntry.substring(0, lastColon);
                                    int chance = Integer.parseInt(lootEntry.substring(lastColon + 1));

                                    if (random.nextInt(100) < chance) {
                                        String[] itemData = leftPart.split(";");
                                        if (itemData.length != 3) continue;
                                        int min = Integer.parseInt(itemData[0]);
                                        String itemId = itemData[1];
                                        int max = Integer.parseInt(itemData[2]);

                                        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
                                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                            int finalCount = min + random.nextInt((max - min) + 1);
                                            chestEntity.setItem(random.nextInt(chestEntity.getContainerSize()), new ItemStack(item, finalCount));
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("[Customizable One Block] Ошибка лута: " + lootEntry);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void spawnItemStrictlyAbove(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(0, 0.05, 0);
        level.addFreshEntity(entity);
    }

    private String getRandomWeightedItem(List<String> entries) {
        int totalWeight = 0;
        List<String> items = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (String entry : entries) {
            try {
                int lastColon = entry.lastIndexOf(':');
                if (lastColon == -1) continue;
                String blockId = entry.substring(0, lastColon);
                int w = Integer.parseInt(entry.substring(lastColon + 1));
                if (w <= 0) continue;
                totalWeight += w;
                items.add(blockId);
                weights.add(totalWeight);
            } catch (Exception e) {
                System.err.println("[Customizable One Block] Ошибка веса: " + entry);
            }
        }
        if (totalWeight <= 0) return "minecraft:stone";
        int r = random.nextInt(totalWeight);
        for (int i = 0; i < items.size(); i++) {
            if (r < weights.get(i)) return items.get(i);
        }
        return "minecraft:stone";
    }
}
