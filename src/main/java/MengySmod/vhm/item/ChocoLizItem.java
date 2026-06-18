package MengySmod.vhm.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ChocoLizItem extends Item {
    public ChocoLizItem() {
        super(new Properties().stacksTo(16).food(VhmFoods.chocoLiz()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 10, 0));
            entity.heal(entity.getMaxHealth() * 0.3f);
            if (VhmEffectHelper.chance(level.random, 0.3f)) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 10, 1));
            }
        }
        return result;
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.EAT;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_EAT;
    }
}
