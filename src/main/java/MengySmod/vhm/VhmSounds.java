package MengySmod.vhm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class VhmSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Vhm.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TREADMILL_VILLAGER_MOUNT = register("treadmill_villager_mount");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREADMILL_VILLAGER_FEED = register("treadmill_villager_feed");

    private VhmSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Vhm.MODID, name)));
    }
}
