package cc.vops.duels.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class PlayerUtil {
    public static void teleport(Ref<EntityStore> ref, World world, double x, double y, double z) {
        teleport(ref, world, new Vector3d(x, y, z), new Vector3f(0, 0, 0));
    }

    public static void teleport(Ref<EntityStore> ref, World world, @Nonnull Vector3d position, @Nonnull Vector3f rotation) {
        final Store<EntityStore> store = ref.getStore();

        store.getExternalData().getWorld().execute(() -> {
            final Player playerComponent = store.getComponent(ref, Player.getComponentType());
            assert playerComponent != null;


            final Teleport teleport = new Teleport(world, position, rotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        });
    }

    public static PlayerRef getPlayer(Predicate<PlayerRef> predicate, World world) {
        return world.getPlayerRefs().stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    public static void playSound(PlayerRef ref, String soundId, float volumeModifier, float pitchModifier) {
        int index = SoundEvent.getAssetMap().getIndex(soundId);
        if (index == -1) return; // dont even know if this is real lol
        if (ref.getReference() == null || !ref.isValid()) return;
        ref.getReference().getStore().getExternalData().getWorld().execute(() -> {
            SoundUtil.playSoundEvent2dToPlayer(ref, index, SoundCategory.UI, volumeModifier, pitchModifier);
        });
    }

    public static void playSound(PlayerRef ref, String soundId) {
        playSound(ref, soundId, 1.0f, 1.0f);
    }
}
