package cc.vops.duels.duel.listener;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.duel.match.Match;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class DuelDeath extends DeathSystems.OnDeathSystem {

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Match match = HytaleDuels.getInstance().getDuelManager().getMatchByWorld(ref.getStore().getExternalData().getWorld());
        final Player player = store.getComponent(ref, Player.getComponentType());
        if (match != null && player != null) {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                UUID uuid = playerRef.getUuid();
                if (match.getTeam(uuid) != null) {
                    deathComponent.setShowDeathMenu(false);
                    deathComponent.setItemsLossMode(DeathConfig.ItemsLossMode.ALL);
                    deathComponent.setItemsDurabilityLossPercentage(0);
                    deathComponent.setItemsLostOnDeath(List.of());

                    match.markDead(playerRef);
                }
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
