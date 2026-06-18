package MengySmod.vhm;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import MengySmod.vhm.item.CleansingBrushItem;
import MengySmod.vhm.item.SpriteSipItem;
import MengySmod.vhm.item.ChocoLizItem;

public class VhmItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Vhm.MODID);

    public static final DeferredItem<BlockItem> TREADMILL = ITEMS.registerSimpleBlockItem("treadmill", VhmBlocks.TREADMILL);
    // 占位资源名已先接好，贴图和模型后续可按这些 id 直接补。
    public static final DeferredItem<Item> SPRITE_SIP = ITEMS.register("sprite_sip", SpriteSipItem::new);
    public static final DeferredItem<Item> CHOCO_LIZ = ITEMS.register("choco_liz", ChocoLizItem::new);
    public static final DeferredItem<Item> CLEANSING_BRUSH = ITEMS.register("cleansing_brush", CleansingBrushItem::new);
}
