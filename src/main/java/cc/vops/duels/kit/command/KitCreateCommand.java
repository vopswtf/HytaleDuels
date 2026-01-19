package cc.vops.duels.kit.command;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.ArenaSchematic;
import cc.vops.duels.kit.Kit;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class KitCreateCommand extends AbstractPlayerCommand {
    @Nonnull
    private final RequiredArg<String> kitId;
    @Nonnull
    private final RequiredArg<String> kitName;

    public KitCreateCommand() {
        super("create", "Create a kit.");
        this.requirePermission("duels.command.kit.create");
        this.kitId = this.withRequiredArg("kitId", "Kit ID", ArgTypes.STRING);
        this.kitName = this.withRequiredArg("kitName", "Kit Name", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store, @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        String id = this.kitId.get(context);
        String name = this.kitName.get(context);
        if (id.isEmpty() || name.isEmpty()) {
            context.sendMessage(Message.raw("Kit ID cannot be empty.").color(Color.RED));
            return;
        }

        if (HytaleDuels.getInstance().getKitManager().getKitById(name) != null) {
            context.sendMessage(Message.raw("A kit with that ID already exists.").color(Color.RED));
            return;
        }

        Kit kit = new Kit(id, name);

        if (context.isPlayer()) {
            final Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            assert player != null;

            kit.setInventory(player.getInventory());
        }

        HytaleDuels.getInstance().getKitManager().registerKit(kit);
        context.sendMessage(Message.raw("Kit '" + kit.getName() + "' has been created.").color(Color.GREEN));
    }
}
