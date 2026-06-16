package MengySmod.vhm;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VhmItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Vhm.MODID);

    public static final DeferredItem<BlockItem> TREADMILL = ITEMS.registerSimpleBlockItem("treadmill", VhmBlocks.TREADMILL);
}
