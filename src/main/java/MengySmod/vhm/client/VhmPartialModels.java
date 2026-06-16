package MengySmod.vhm.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class VhmPartialModels {
    public static final PartialModel TREADMILL_BELT =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath("vhm", "block/treadmill_belt"));
    public static final PartialModel TREADMILL_SHAFT_END =
        PartialModel.of(ResourceLocation.fromNamespaceAndPath("vhm", "block/treadmill_shaft_end"));

    private VhmPartialModels() {}

    /** Must run during {@link net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional}. */
    public static void init() {
        // Force static field init so PartialModel.of entries exist before Flywheel bakes models.
        if (TREADMILL_BELT == null || TREADMILL_SHAFT_END == null) {
            throw new IllegalStateException("VHM partial models failed to initialize");
        }
    }
}
