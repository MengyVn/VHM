package MengySmod.vhm.treadmill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import MengySmod.vhm.VhmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class TreadmillNetworkHelper {
    public static final int MAX_CASCADE = 32;

    private TreadmillNetworkHelper() {}

    public static List<TreadmillBlockEntity> collectCascade(Level level, BlockPos origin, Axis axis) {
        List<TreadmillBlockEntity> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        collectAlongAxis(level, origin, axis, Direction.AxisDirection.POSITIVE, visited, result);
        collectAlongAxis(level, origin, axis, Direction.AxisDirection.NEGATIVE, visited, result);
        return result;
    }

    private static void collectAlongAxis(
        Level level,
        BlockPos start,
        Axis axis,
        Direction.AxisDirection direction,
        Set<BlockPos> visited,
        List<TreadmillBlockEntity> result
    ) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        while (result.size() < MAX_CASCADE) {
            if (!visited.add(cursor.immutable())) {
                break;
            }
            if (!(level.getBlockEntity(cursor) instanceof TreadmillBlockEntity treadmill)) {
                break;
            }
            if (treadmill.getRotationAxis() != axis) {
                break;
            }
            result.add(treadmill);
            cursor.move(Direction.fromAxisAndDirection(axis, direction));
        }
    }

    public static int countCascade(Level level, BlockPos origin, Axis axis) {
        return collectCascade(level, origin, axis).size();
    }

    public static float networkMaxRpm(List<TreadmillBlockEntity> units) {
        float max = 0;
        for (TreadmillBlockEntity unit : units) {
            max = Math.max(max, Math.abs(unit.getLocalGeneratedSpeed()));
        }
        return max;
    }

    public static float networkTotalCapacity(List<TreadmillBlockEntity> units, float networkRpm) {
        if (networkRpm == 0) {
            return 0;
        }
        float total = 0;
        for (TreadmillBlockEntity unit : units) {
            if (unit.getLocalGeneratedSpeed() != 0) {
                total += unit.getLocalStressCapacity();
            }
        }
        return total;
    }

    public static float applyManualCap(List<TreadmillBlockEntity> units, float rawTotalCapacity) {
        float cap = Float.MAX_VALUE;
        for (TreadmillBlockEntity unit : units) {
            if (unit.isManualMode()) {
                cap = Math.min(cap, unit.getStressCap());
            }
        }
        return Math.min(rawTotalCapacity, cap);
    }

    public static boolean isTreadmillState(BlockState state) {
        return state.is(VhmBlocks.TREADMILL);
    }
}
