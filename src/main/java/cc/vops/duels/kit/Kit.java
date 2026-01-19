package cc.vops.duels.kit;

import cc.vops.duels.HytaleDuels;
import com.google.gson.JsonObject;
import com.hypixel.hytale.codec.EmptyExtraInfo;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;
import org.bson.BsonDocument;

import java.util.UUID;

@Getter
public class Kit {
    private final String id;
    @Setter private String name;
    private Inventory inventory;

    public Kit(String id, String name) {
        this.id = id.toUpperCase();
        this.name = name;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = deepClone(inventory, null);
        this.coverItem = null;
    }

    public void applyTo(Player player) {
        if (player == null || player.getReference() == null) return;
        Store<EntityStore> store = player.getReference().getStore();
        Player.setGameMode(player.getReference(), GameMode.Adventure, store);

        EntityStatMap entityStatMapComponent = store.getComponent(player.getReference(), EntityStatMap.getComponentType());
        if (entityStatMapComponent != null) {
            EntityStatValue healthStatValue = entityStatMapComponent.get(DefaultEntityStatTypes.getHealth());
            if (healthStatValue != null) {
                entityStatMapComponent.setStatValue(DefaultEntityStatTypes.getHealth(), healthStatValue.getMax());
            }
        }

        if (inventory != null) {
            player.setInventory(deepClone(inventory, player));
            player.sendInventory();
        }
    }

    private ItemStack coverItem = null;
    public ItemStack getCoverItem() {
        if (coverItem != null) return coverItem;
        if (inventory != null) {
            for (int i = 0; i < inventory.getHotbar().getCapacity(); i++) {
                ItemStack itemStack = inventory.getHotbar().getItemStack((short) i);
                if (itemStack != null && !itemStack.isEmpty()) {
                    return coverItem = itemStack;
                }
            }
        }
        return null;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);

        if (inventory != null) {
            try {
                BsonDocument bson = Inventory.CODEC.encode(inventory, new ExtraInfo());
                JsonObject inventoryJson = HytaleDuels.GSON.fromJson(bson.toJson(), JsonObject.class);
                json.add("inventory", inventoryJson);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        return json;
    }

    public static Kit fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        Kit kit = new Kit(id, name);

        if (json.has("inventory")) {
            try {
                JsonObject inventoryJson = json.getAsJsonObject("inventory");
                BsonDocument bson = BsonDocument.parse(inventoryJson.toString());
                Inventory inventory = Inventory.CODEC.decode(bson, new ExtraInfo());
                kit.setInventory(inventory);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        return kit;
    }

    private static Inventory deepClone(Inventory original, LivingEntity entity) {
        try {
            BsonDocument bson = Inventory.CODEC.encode(original, EmptyExtraInfo.EMPTY);
            Inventory clone = Inventory.CODEC.decode(bson, EmptyExtraInfo.EMPTY);
            assert clone != null;
            clone.setEntity(entity);
            return clone;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
