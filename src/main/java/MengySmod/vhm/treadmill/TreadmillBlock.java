package MengySmod.vhm.treadmill;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import MengySmod.vhm.VhmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TreadmillBlock extends HorizontalKineticBlock implements IBE<TreadmillBlockEntity> {
    // 新模型的碰撞箱：底座2格高，侧板5格高（从y=2到y=7），前控制台6格高
    private static final VoxelShape SHAPE = Shapes.or(
        // 底座框架 (0-2, 0-16, 0-16)
        Shapes.box(0, 0, 0, 1, 2 / 16.0, 1),
        // 左侧板 (0-2, 2-7, 1-15)
        Shapes.box(0, 2 / 16.0, 1 / 16.0, 2 / 16.0, 7 / 16.0, 15 / 16.0),
        // 右侧板 (14-16, 2-7, 1-15)
        Shapes.box(14 / 16.0, 2 / 16.0, 1 / 16.0, 1, 7 / 16.0, 15 / 16.0),
        // 前控制台 (1-15, 2-8, 0-2)
        Shapes.box(1 / 16.0, 2 / 16.0, 0, 15 / 16.0, 8 / 16.0, 2 / 16.0),
        // 后支撑 (1-15, 2-6, 14-16)
        Shapes.box(1 / 16.0, 2 / 16.0, 14 / 16.0, 15 / 16.0, 6 / 16.0, 1),
        // 传送带表面可站立区域 (2-14, 4-5, 2-14) - 让玩家能站在上面
        Shapes.box(2 / 16.0, 4 / 16.0, 2 / 16.0, 14 / 16.0, 5 / 16.0, 14 / 16.0)
    );

    public TreadmillBlock(Properties properties) {
        super(properties);
    }

    // 重写getStateForPlacement方法，确保放置时面向玩家
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TreadmillBlockEntity treadmill) {
            AABB releaseBox = treadmill.beltBounds().inflate(1.25, 1.0, 1.25);
            level.getEntitiesOfClass(Player.class, releaseBox, TreadmillMount::isMounted).forEach(TreadmillMount::release);
            level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, releaseBox, TreadmillMount::isMounted)
                .forEach(TreadmillMount::release);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.NONE;
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        return onBlockEntityUse(level, pos, be -> {
            if (be.isMountedHere(player)) {
                if (player.isShiftKeyDown()) {
                    if (!level.isClientSide) {
                        be.dismount(player);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (player.isShiftKeyDown()) {
                if (!level.isClientSide && be.hasBoundVillager()) {
                    be.ejectBoundVillager();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide && be.tryMount(player)) {
                player.displayClientMessage(Component.translatable("message.vhm.treadmill.run"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public Class<TreadmillBlockEntity> getBlockEntityClass() {
        return TreadmillBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TreadmillBlockEntity> getBlockEntityType() {
        return VhmBlockEntities.TREADMILL.get();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> ((TreadmillBlockEntity) be).tick();
    }
}
