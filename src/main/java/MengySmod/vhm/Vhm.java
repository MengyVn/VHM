package MengySmod.vhm;

import com.simibubi.create.api.stress.BlockStressValues;
import com.mojang.logging.LogUtils;
import MengySmod.vhm.event.TreadmillEvents;
import MengySmod.vhm.network.VhmNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Vhm.MODID)
public class Vhm {
    public static final String MODID = "vhm";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TREADMILL_TAB = CREATIVE_MODE_TABS.register("treadmill_tab", () -> CreativeModeTab.builder()
        .title(Component.literal("机械动力:你跑不过我你信吗"))
        .icon(() -> VhmItems.TREADMILL.get().getDefaultInstance())
        .displayItems((params, output) -> output.accept(VhmItems.TREADMILL.get()))
        .build());

    public Vhm(IEventBus modEventBus) {
        VhmBlocks.BLOCKS.register(modEventBus);
        VhmItems.ITEMS.register(modEventBus);
        VhmBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(VhmNetwork::registerPayloads);

        NeoForge.EVENT_BUS.register(TreadmillEvents.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.CAPACITIES.register(
                VhmBlocks.TREADMILL.get(),
                () -> (double) MengySmod.vhm.treadmill.TreadmillBlockEntity.BASE_CAPACITY
            );
            BlockStressValues.RPM.register(
                VhmBlocks.TREADMILL.get(),
                new BlockStressValues.GeneratedRpm((int) MengySmod.vhm.treadmill.TreadmillBlockEntity.BASE_RPM, true)
            );
        });
        LOGGER.info("VHM treadmill mod initialized");
    }
}
