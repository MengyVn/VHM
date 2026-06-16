package MengySmod.vhm.client;

import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.minecraft.resources.ResourceLocation;

public class VhmSpriteShifts {
    private static final ResourceLocation BELT = ResourceLocation.fromNamespaceAndPath("create", "block/belt");
    private static final ResourceLocation BELT_SCROLL = ResourceLocation.fromNamespaceAndPath("create", "block/belt_scroll");

    public static final SpriteShiftEntry TREADMILL_BELT = SpriteShifter.get(BELT, BELT_SCROLL);

    private VhmSpriteShifts() {}
}
