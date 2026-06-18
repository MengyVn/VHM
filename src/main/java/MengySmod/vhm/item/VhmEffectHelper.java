package MengySmod.vhm.item;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class VhmEffectHelper {
    private VhmEffectHelper() {}

    public static boolean chance(RandomSource random, float probability) {
        return random.nextFloat() < probability;
    }

    public static void clearHarmfulEffects(LivingEntity entity) {
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                entity.removeEffect(effect.getEffect());
            }
        }
    }
}
