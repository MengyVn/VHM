package MengySmod.vhm;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class VhmEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Vhm.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> TREADMILL_BREAD_BOOST = EFFECTS.register(
        "treadmill_bread_boost",
        () -> new VhmTrackedEffect(MobEffectCategory.BENEFICIAL, 0xC89C5F)
    );

    public static final DeferredHolder<MobEffect, MobEffect> TREADMILL_DRINK_BOOST = EFFECTS.register(
        "treadmill_drink_boost",
        () -> new VhmTrackedEffect(MobEffectCategory.BENEFICIAL, 0x7CD4FF)
    );

    public static final DeferredHolder<MobEffect, MobEffect> TREADMILL_SNACK_BOOST = EFFECTS.register(
        "treadmill_snack_boost",
        () -> new VhmTrackedEffect(MobEffectCategory.BENEFICIAL, 0xFFB35C)
    );

    private VhmEffects() {}
}
