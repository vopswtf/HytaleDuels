package cc.vops.duels.arena.command;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.ArenaSchematic;
import cc.vops.duels.duel.match.Match;
import cc.vops.duels.util.PlayerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ArenaTestInstanceCommand extends AbstractAsyncPlayerCommand {
    @Nonnull
    private final RequiredArg<String> arenaName;

    public ArenaTestInstanceCommand() {
        super("testinstance", "Generate a test arena instance.");
        this.requirePermission("duels.command.arena.test");
        this.arenaName = this.withRequiredArg("arenaName", "Arena Name", ArgTypes.STRING);
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store, @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final String name = this.arenaName.get(context);
        if (name.isEmpty()) {
            context.sendMessage(Message.raw("Arena name cannot be empty.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        ArenaSchematic schematic = HytaleDuels.getInstance().getArenaManager().getSchematicByName(name);
        if (schematic == null) {
            context.sendMessage(Message.raw("No arena schematic found with name '" + name + "'.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

//        HytaleDuels.getInstance().getArenaManager().createInstance(schematic, new Match(playerRef.getUuid(), UUID.randomUUID())).thenAccept(instance -> {
//            context.sendMessage(Message.raw("Test arena instance created successfully from schematic '" + name + "'.").color(Color.GREEN));
//            instance.teleport(playerRef);
//        }).exceptionally(ex -> {
//            context.sendMessage(Message.raw("Failed to create test arena instance: " + ex.getMessage()).color(Color.RED));
//            ex.printStackTrace();
//            return null;
//        });

        return CompletableFuture.completedFuture(null);
    }
}
