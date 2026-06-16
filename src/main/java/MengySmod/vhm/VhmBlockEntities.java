package MengySmod.vhm;

import MengySmod.vhm.treadmill.TreadmillBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VhmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Vhm.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TreadmillBlockEntity>> TREADMILL =
        BLOCK_ENTITY_TYPES.register("treadmill", () -> BlockEntityType.Builder
            .of(TreadmillBlockEntity::new, VhmBlocks.TREADMILL.get())
            .build(null));
}
