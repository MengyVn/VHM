package MengySmod.vhm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import org.slf4j.Logger;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import MengySmod.vhm.treadmill.TreadmillBlock;
import MengySmod.vhm.treadmill.TreadmillBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class TreadmillRenderer extends KineticBlockEntityRenderer<TreadmillBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TreadmillRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
        TreadmillBlockEntity be,
        float partialTicks,
        PoseStack ms,
        MultiBufferSource buffer,
        int light,
        int overlay
    ) {
        try {
            if (VisualizationManager.supportsVisualization(be.getLevel())) {
                return;
            }
            renderBelt(be, ms, buffer, light);
            renderShafts(be, ms, buffer, light);
        } catch (Exception e) {
            LOGGER.error("[TreadmillRenderer] Error in renderSafe at {}", be.getBlockPos(), e);
        }
    }

    private void renderShafts(
        TreadmillBlockEntity be,
        PoseStack ms,
        MultiBufferSource buffer,
        int light
    ) {
        try {
            Axis axis = be.getRotationAxis();
            BlockPos pos = be.getBlockPos();
            float time = AnimationTickHolder.getRenderTime(be.getLevel());
            float offset = getRotationOffsetForPosition(be, pos, axis);
            float fixedRpm = TreadmillBlockEntity.BASE_RPM;
            float angle = ((time * fixedRpm * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;

            // Use shaft's own BlockState instead of treadmill's BlockState
            BlockState shaftState = shaft(axis);

            for (Direction direction : Iterate.directionsInAxis(axis)) {
                SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, shaftState, direction);
                kineticRotationTransform(shaft, be, axis, angle, light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
            }
        } catch (Exception e) {
            LOGGER.error("[TreadmillRenderer] Error in renderShafts at {}", be.getBlockPos(), e);
        }
    }

    private void renderBelt(TreadmillBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        try {
            BlockState state = be.getBlockState();
            Direction facing = state.getValue(TreadmillBlock.HORIZONTAL_FACING);
            SuperByteBuffer beltBuffer = CachedBuffers.partialFacing(VhmPartialModels.TREADMILL_BELT, state, facing);
            float beltMultiplier = be.getBeltSpeedMultiplier();
            if (beltMultiplier > 0) {
                float time = AnimationTickHolder.getRenderTime(be.getLevel());
                float spriteSize = VhmSpriteShifts.TREADMILL_BELT.getTarget().getV1()
                    - VhmSpriteShifts.TREADMILL_BELT.getTarget().getV0();
                float effectiveSpeed = TreadmillBlockEntity.BASE_RPM * beltMultiplier;
                double scroll = effectiveSpeed * time / (31.5 * 16);
                scroll = scroll - Math.floor(scroll);
                beltBuffer.shiftUVScrolling(VhmSpriteShifts.TREADMILL_BELT, (float) scroll * spriteSize);
            }
            beltBuffer.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        } catch (Exception e) {
            LOGGER.error("[TreadmillRenderer] Error in renderBelt at {}", be.getBlockPos(), e);
        }
    }
}
