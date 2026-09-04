package org.vidgamestudio.customizable_one_block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GeneratorBlock extends BaseEntityBlock {

    private static final VoxelShape TALL_COLLISION_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);

    protected GeneratorBlock(Properties properties) {
        super(properties);
    }

    // 1. Изменяем физическую коллизию (по чему ходят)
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TALL_COLLISION_SHAPE;
    }

    // 2. Оставляем хитбокс выбора (черная рамка при наведении) стандартным (1 блок)
    // Это нужно, чтобы игрок мог целиться и бить по верхнему блоку (руде/дереву) отдельно!
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block(); // Обычный куб 1х1х1
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            BlockPos targetPos = pos.above();
            if (level.isEmptyBlock(targetPos)) {
                level.setBlockAndUpdate(targetPos, net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState());
            }
        }
    }
}