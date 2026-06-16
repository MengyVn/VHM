package MengySmod.vhm;

import MengySmod.vhm.treadmill.TreadmillBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VhmBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Vhm.MODID);

    public static final DeferredBlock<TreadmillBlock> TREADMILL = BLOCKS.register(
        "treadmill",
        () -> new TreadmillBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion())
    );
}
