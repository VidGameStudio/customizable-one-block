package org.vidgamestudio.customizable_one_block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneratorBlockEntity extends BlockEntity {
    private String activeCategory = "minecraft";
    private final CompoundTag blocksBrokenMap = new CompoundTag();
    private final Random random = new Random();
    private int checkCooldown = 0;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Customizable_one_block.GENERATOR_BE.get(), pos, state);
    }

    public String getActiveCategory() { return activeCategory; }

    public void setActiveCategory(String category) {
        this.activeCategory = category;
        setChanged();
    }

    public int getBlocksBroken(String category) {
        return blocksBrokenMap.getInt(category);
    }


    public void incrementBlocksInternal() {
        int current = getBlocksBroken(activeCategory);
        int nextValue = current + 1;
        blocksBrokenMap.putInt(activeCategory, nextValue);
        setChanged();

        if (checkIfPhaseJustChanged(nextValue)) {
            notifyNearbyPlayers();
        }
    }


    private boolean checkIfPhaseJustChanged(int newScore) {
        ModConfig.Category configCat = ModConfig.INSTANCE.categories.get(activeCategory);
        if (configCat != null && configCat.phases != null) {
            for (ModConfig.Phase p : configCat.phases) {
                if (newScore == p.blocks_required && p.blocks_required > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void notifyNearbyPlayers() {
        if (level == null || level.isClientSide) return;

        AABB area = new AABB(this.worldPosition).inflate(50.0);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player p : players) {
            p.sendSystemMessage(Component.literal("§6[OneBlock] Блок перешел на новую стадию: §e" + activeCategory + "§6!"));

            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("ActiveCategory", activeCategory);
        tag.put("BlocksBrokenMap", blocksBrokenMap);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.activeCategory = tag.getString("ActiveCategory");
        if (tag.contains("BlocksBrokenMap")) {
            this.blocksBrokenMap.merge(tag.getCompound("BlocksBrokenMap"));
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.checkCooldown++;
        if (blockEntity.checkCooldown < 2) return;
        blockEntity.checkCooldown = 0;

        BlockPos brokenPos = pos.above();
        BlockState currentState = level.getBlockState(brokenPos);

        if (currentState.isAir() || currentState.is(Blocks.CAVE_AIR)) {
            blockEntity.generateNextBlock(level, brokenPos);
        }
    }

    public void generateNextBlock(Level level, BlockPos brokenPos) {
        if (level.getBlockEntity(brokenPos) instanceof ChestBlockEntity chest) { // Дроп сундука
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (!stack.isEmpty()) spawnItemStrictlyAbove(level, brokenPos, stack.copy());
            }
            chest.clearContent();
        }

        String categoryName = getActiveCategory();
        if (categoryName == null || !ModConfig.INSTANCE.categories.containsKey(categoryName)) {
            categoryName = "minecraft";
            setActiveCategory("minecraft");
        }
        int score = getBlocksBroken(categoryName);

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
