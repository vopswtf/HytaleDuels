package cc.vops.duels.kit.page;

import cc.vops.duels.kit.Kit;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class SelectKitPage extends InteractiveCustomUIPage<SelectKitPage.SelectKitData> {

    private static final int KITS_PER_ROW = 5;

    private final List<Kit> kits;
    private final Consumer<Kit> onSelected;

    public SelectKitPage(
            @Nonnull PlayerRef playerRef,
            @Nonnull CustomPageLifetime lifetime,
            @Nonnull List<Kit> kits,
            @Nonnull Consumer<Kit> onSelected
    ) {
        super(playerRef, lifetime, SelectKitData.CODEC);
        this.kits = kits;
        this.onSelected = onSelected;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("Pages/Kit/SelectKitPage.ui");
        buildKitGrid(commandBuilder, eventBuilder);
    }

    private void buildKitGrid(
            UICommandBuilder commandBuilder,
            UIEventBuilder eventBuilder
    ) {
        commandBuilder.clear("#KitGrid");

        int row = 0;
        int col = 0;

        for (Kit kit : kits) {
            if (col == 0) {
                commandBuilder.appendInline(
                        "#KitGrid",
                        "Group { LayoutMode: Left; Anchor: (Bottom: 10); }"
                );
            }

            String path = "#KitGrid[" + row + "][" + col + "]";
            commandBuilder.append(
                    "#KitGrid[" + row + "]",
                    "Pages/Kit/KitCard.ui"
            );

            commandBuilder.set(path + " #Name.Text", kit.getName());
            if (kit.getCoverItem() != null) {
                commandBuilder.set(
                        path + " #Icon.ItemId",
                        kit.getCoverItem().getItemId()
                );
            }

            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    path,
                    EventData.of(SelectKitData.KEY_KIT_ID, kit.getId())
            );

            col++;
            if (col >= KITS_PER_ROW) {
                col = 0;
                row++;
            }
        }
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull SelectKitData data
    ) {
        super.handleDataEvent(ref, store, data);

        if (data.kitId == null) return;

        Kit selected = kits.stream()
                .filter(k -> k.getId().equals(data.kitId))
                .findFirst()
                .orElse(null);

        if (selected == null) return;

        close();
        onSelected.accept(selected);
    }

    public static class SelectKitData {
        public static final String KEY_KIT_ID = "KitId";

        public static final BuilderCodec<SelectKitData> CODEC =
                BuilderCodec.builder(SelectKitData.class, SelectKitData::new)
                        .addField(new KeyedCodec<>(KEY_KIT_ID, Codec.STRING),
                                (d, v) -> d.kitId = v,
                                d -> d.kitId)
                        .build();

        private String kitId;
    }
}
