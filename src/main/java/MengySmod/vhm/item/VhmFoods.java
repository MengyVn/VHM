package MengySmod.vhm.item;

import net.minecraft.world.food.FoodProperties;

public final class VhmFoods {
    private VhmFoods() {}

    public static FoodProperties spriteSip() {
        return new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.4f)
            .alwaysEdible()
            .build();
    }

    public static FoodProperties chocoLiz() {
        return new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.8f)
            .alwaysEdible()
            .build();
    }
}
