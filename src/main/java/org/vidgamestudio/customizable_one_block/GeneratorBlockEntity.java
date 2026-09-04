package org.vidgamestudio.customizable_one_block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class GeneratorBlockEntity extends BlockEntity {
    private String activeCategory = "minecraft";
    private final CompoundTag blocksBrokenMap = new CompoundTag();

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

    public void incrementBlocks(ServerPlayer player) {
        int current = getBlocksBroken(activeCategory);
        int nextValue = current + 1;
        blocksBrokenMap.putInt(activeCategory, nextValue);
        setChanged();

        ModScoreboard.updatePlayerScore(player, nextValue);

        if (checkIfPhaseJustChanged(nextValue)) {
            notifyNearbyPlayers();
        }
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
}
