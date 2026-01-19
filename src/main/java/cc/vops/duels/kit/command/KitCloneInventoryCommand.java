package cc.vops.duels.kit.command;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.kit.Kit;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class KitCloneInventoryCommand extends AbstractAsyncPlayerCommand {
    @Nonnull
    private final RequiredArg<String> kitId;

    public KitCloneInventoryCommand() {
        super("cloneinventory", "Clone the inventory of a kit.");
        this.requirePermission("duels.command.kit.cloneinventory");
        this.kitId = this.withRequiredArg("kitId", "Kit ID", ArgTypes.STRING);
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store, @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        String id = this.kitId.get(context);
        if (id.isEmpty()) {
            context.sendMessage(Message.raw("Kit ID cannot be empty.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Kit kit = HytaleDuels.getInstance().getKitManager().getKitById(id);
        if (kit == null) {
            context.sendMessage(Message.raw("No kit with that ID exists.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        final Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        assert player != null;

        kit.applyTo(player);

        context.sendMessage(Message.raw("Kit for '" + kit.getName() + "' inventory has been cloned to your inventory.").color(Color.GREEN));
        return CompletableFuture.completedFuture(null);
    }
}
