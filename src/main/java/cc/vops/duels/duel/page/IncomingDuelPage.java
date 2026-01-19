package cc.vops.duels.duel.page;

import cc.vops.duels.arena.page.ArenasPage;
import cc.vops.duels.duel.DuelInvite;
import cc.vops.duels.kit.Kit;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IncomingDuelPage extends InteractiveCustomUIPage<IncomingDuelPage.DuelPageData> {
    private final DuelInvite duelInvite;

    public IncomingDuelPage(
            @Nonnull PlayerRef playerRef,
            @Nonnull CustomPageLifetime lifetime,
            DuelInvite duelInvite
    ) {
        super(playerRef, lifetime, DuelPageData.CODEC);
        this.duelInvite = duelInvite;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("Pages/IncomingDuelPage.ui");

        commandBuilder.set("#ChallengerName.Text", duelInvite.getChallengerName());
        commandBuilder.set("#ChallengerExtra.Text", "has challenged you to a duel!");
        commandBuilder.set("#KitLabel.Text", duelInvite.getKit().getName());
        commandBuilder.set("#ArenaLabel.Text", duelInvite.getArenaSchematic().getName());

        ItemStack coverItem = duelInvite.getKit().getCoverItem();
        if (coverItem != null) {
            commandBuilder.set("#ItemIcon.ItemId", coverItem.getItemId());
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AcceptButton",
                EventData.of(DuelPageData.KEY_ACTION, DuelPageData.ACTION_ACCEPT)
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DeclineButton",
                EventData.of(DuelPageData.KEY_ACTION, DuelPageData.ACTION_DECLINE)
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull DuelPageData data
    ) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case DuelPageData.ACTION_ACCEPT -> {
                onAccept(ref, store);
            }
            case DuelPageData.ACTION_DECLINE -> {
                onDecline(ref, store);
            }
        }
    }

    protected void onAccept(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (this.duelInvite.isExpired()) {
            close();

            ref.getStore().getExternalData().getWorld().execute(() -> {
                Player playerComponent = store.getComponent(ref, Player.getComponentType());
                assert playerComponent != null;

                playerComponent.sendMessage(Message.raw("This duel invite has expired.").color(Color.RED));
            });
            return;
        }

        ref.getStore().getExternalData().getWorld().execute(duelInvite::accept);
        close();
    }

    protected void onDecline(Ref<EntityStore> ref, Store<EntityStore> store) {
        close();
    }

    private void onTimeout(Ref<EntityStore> ref, Store<EntityStore> store) {
        final Player playerComponent = store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        playerComponent.getPageManager().setPage(ref, store, Page.None);
    }

    public static class DuelPageData {
        public static final String KEY_ACTION = "Action";
        public static final String ACTION_ACCEPT = "accept";
        public static final String ACTION_DECLINE = "decline";

        public static final BuilderCodec<DuelPageData> CODEC =
                BuilderCodec.<DuelPageData>builder(DuelPageData.class, DuelPageData::new)
                        .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                                (d, v) -> d.action = v,
                                d -> d.action)
                        .build();

        private String action;
    }
}
