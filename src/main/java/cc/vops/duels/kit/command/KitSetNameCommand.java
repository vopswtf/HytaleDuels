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
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class KitSetNameCommand extends AbstractPlayerCommand {
    @Nonnull
    private final RequiredArg<String> kitId;
    @Nonnull
    private final RequiredArg<String> displayName;

    public KitSetNameCommand() {
        super("setname", "Set the display name of a kit.");
        this.requirePermission("duels.command.kit.setinventory");
        this.kitId = this.withRequiredArg("kitId", "Kit ID", ArgTypes.STRING);
        this.displayName = this.withRequiredArg("displayName", "New Display Name", ArgTypes.STRING);
    }

    @Nonnull
    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store, @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        String id = this.kitId.get(context);
        if (id.isEmpty()) {
            context.sendMessage(Message.raw("Kit ID cannot be empty.").color(Color.RED));
            return;
        }

        String newName = this.displayName.get(context);
        if (newName.isEmpty()) {
            context.sendMessage(Message.raw("Display name cannot be empty.").color(Color.RED));
            return;
        }

        if (newName.startsWith("\"") && newName.endsWith("\"") && newName.length() >= 2) {
            newName = newName.substring(1, newName.length() - 1);
        }

        Kit kit = HytaleDuels.getInstance().getKitManager().getKitById(id);
        if (kit == null) {
            context.sendMessage(Message.raw("No kit with that ID exists.").color(Color.RED));
            return;
        }

        kit.setName(newName);
        context.sendMessage(Message.raw("Kit display name set to " + newName + ".").color(Color.GREEN));
    }
}
