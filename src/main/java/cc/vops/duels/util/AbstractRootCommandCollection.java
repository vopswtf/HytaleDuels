package cc.vops.duels.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractRootCommandCollection extends AbstractAsyncCommand {
    @Nonnull
    private static final Message MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD = Message.translation("server.commands.errors.playerNotInWorld");
    @Nonnull
    private static final Message MESSAGE_COMMANDS_ERRORS_PLAYER_OR_ARG = Message.translation("server.commands.errors.playerOrArg").param("option", "player");


    public AbstractRootCommandCollection(@Nonnull String name, @Nonnull String description) {
        super(name, description);
    }

    @Nonnull
    public Message getFullUsage(@Nonnull CommandSender sender) {
        return super.getUsageString(sender);
    }

    @Nonnull
    protected final CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        final Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null) {
            context.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_OR_ARG);
            return CompletableFuture.completedFuture(null);
        }

        if (!ref.isValid()) {
            context.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
            return CompletableFuture.completedFuture(null);
        }

        final Store<EntityStore> store = ref.getStore();
        final World world = store.getExternalData().getWorld();
        return this.runAsync(context, () -> {
            final PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRefComponent == null) {
                context.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
            }
            else {
                this.executeRoot(context, store, ref, playerRefComponent, world);
            }
        }, world);
    }

    protected abstract void executeRoot(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    );

    @Nonnull
    public Message getUsageString(@Nonnull CommandSender sender) {
        return this.getUsageShort(sender, false);
    }
}