package MengySmod.vhm.client;

import MengySmod.vhm.Vhm;
import MengySmod.vhm.VhmBlockEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = Vhm.MODID, value = Dist.CLIENT)
public class VhmClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(VhmBlockEntities.TREADMILL.get(), TreadmillRenderer::new);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerPartialModels(ModelEvent.RegisterAdditional event) {
        VhmPartialModels.init();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(VhmBlockEntities.TREADMILL.get())
            .factory(TreadmillVisual.factory())
            .apply());
    }
}
