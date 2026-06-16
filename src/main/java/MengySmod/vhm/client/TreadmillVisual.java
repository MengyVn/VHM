package MengySmod.vhm.client;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import org.slf4j.Logger;

import MengySmod.vhm.treadmill.TreadmillBlock;
import MengySmod.vhm.treadmill.TreadmillBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class TreadmillVisual extends KineticBlockEntityVisual<TreadmillBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float MAGIC_SCROLL_MULTIPLIER = 1f / (31.5f * 16f);

    private final ArrayList<RotatingInstance> shafts = new ArrayList<>(2);
    private ScrollInstance belt;

    public TreadmillVisual(VisualizationContext context, TreadmillBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        try {
            Direction facing = blockState.getValue(TreadmillBlock.HORIZONTAL_FACING);
            float yRot = TreadmillBlockEntity.modelYRotation(facing);
            Direction.Axis axis = blockEntity.getRotationAxis();

            for (Direction dir : Iterate.directionsInAxis(axis)) {
                RotatingInstance shaft = instancerProvider()
                    .instancer(AllInstanceTypes.ROTATING, Models.partial(VhmPartialModels.TREADMILL_SHAFT_END))
                    .createInstance();
                shaft.setup(blockEntity)
                    .setPosition(getVisualPosition())
                    .rotateToFace(Direction.SOUTH, dir)
                    .setChanged();
                shafts.add(shaft);
            }

            belt = instancerProvider()
                .instancer(AllInstanceTypes.SCROLLING, Models.partial(VhmPartialModels.TREADMILL_BELT))
                .createInstance();
            setupBelt(belt, facing, yRot);
            
            LOGGER.debug("[TreadmillVisual] Initialized at {}", getVisualPosition());
        } catch (Exception e) {
            LOGGER.error("[TreadmillVisual] Error in constructor", e);
        }
    }

    public static SimpleBlockEntityVisualizer.Factory<TreadmillBlockEntity> factory() {
        return TreadmillVisual::new;
    }

    @Override
    public void update(float partialTick) {
        try {
            for (RotatingInstance shaft : shafts) {
                shaft.setup(blockEntity).setChanged();
            }

            Direction facing = blockState.getValue(TreadmillBlock.HORIZONTAL_FACING);
            float yRot = TreadmillBlockEntity.modelYRotation(facing);
            setupBelt(belt, facing, yRot);
        } catch (Exception e) {
            LOGGER.error("[TreadmillVisual] Error in update", e);
        }
    }

    private void setupBelt(ScrollInstance beltInstance, Direction facing, float yRot) {
        try {
            float beltMultiplier = blockEntity.getBeltSpeedMultiplier();
            float baseRpm = TreadmillBlockEntity.BASE_RPM;
            float effectiveSpeed = baseRpm * beltMultiplier;

            Quaternionf rotation = new Quaternionf().rotationY(yRot * Mth.DEG_TO_RAD);

            beltInstance.setSpriteShift(VhmSpriteShifts.TREADMILL_BELT, 1f, 0.5f)
                .position(getVisualPosition())
                .rotation(rotation)
                .speed(0, effectiveSpeed * MAGIC_SCROLL_MULTIPLIER)
                .offset(0, 0f)
                .colorRgb(RotatingInstance.colorFromBE(blockEntity))
                .setChanged();
        } catch (Exception e) {
            LOGGER.error("[TreadmillVisual] Error in setupBelt", e);
        }
    }

    @Override
    public void updateLight(float partialTick) {
        try {
            relight(belt);
            shafts.forEach(shaft -> relight(shaft));
        } catch (Exception e) {
            LOGGER.error("[TreadmillVisual] Error in updateLight", e);
        }
    }

    @Override
    protected void _delete() {
        try {
            shafts.forEach(AbstractInstance::delete);
            shafts.clear();
            belt.delete();
        } catch (Exception e) {
            LOGGER.error("[TreadmillVisual] Error in _delete", e);
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        try {
            shafts.forEach(consumer);
            consumer.accept(belt);
        } catch (Exception e) {
            LOGGER.error("[TreadmillVisual] Error in collectCrumblingInstances", e);
        }
    }
}
