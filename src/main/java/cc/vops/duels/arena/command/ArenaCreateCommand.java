package cc.vops.duels.arena.command;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.ArenaSchematic;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.PrefabListAsset;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.prefab.PrefabLoadException;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.PrefabBuffer;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;

import javax.annotation.Nonnull;
import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ArenaCreateCommand extends AbstractAsyncCommand {
    @Nonnull
    private final RequiredArg<String> arenaName;
    @Nonnull
    private final RequiredArg<String> prefabId;
    @Nonnull
    private final OptionalArg<Boolean> forced;

    public ArenaCreateCommand() {
        super("create", "Create an ArenaSchematic.");
        this.requirePermission("duels.command.arena.create");
        this.arenaName = this.withRequiredArg("arenaName", "Arena Display Name", ArgTypes.STRING);
        this.prefabId = this.withRequiredArg("prefabId", "Server Prefab ID", ArgTypes.STRING);
        this.forced = this.withOptionalArg("forced", "Whether to force this creation", ArgTypes.BOOLEAN);
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        final String name = this.arenaName.get(context);
        String prefabAssetId = this.prefabId.get(context);
        if (name.isEmpty() || prefabAssetId.isEmpty()) {
            context.sendMessage(Message.raw("Arena name and prefab ID cannot be empty.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        if (!prefabAssetId.endsWith(".prefab.json")) {
            prefabAssetId += ".prefab.json";
        }

        Path path = PrefabStore.get().getServerPrefabsPath().resolve(prefabAssetId);
        if (!path.toFile().exists()) {
            context.sendMessage(Message.raw("Prefab with ID '" + prefabAssetId + "' does not exist.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        ArenaSchematic arenaSchematic = new ArenaSchematic(name, prefabAssetId);

        if (!forced.provided(context) || !this.forced.get(context)) {
            ArenaSchematic existingName = HytaleDuels.getInstance().getArenaManager().getSchematicByName(name);
            if (existingName != null) {
                context.sendMessage(Message.raw("An arena with the name '" + name + "' already exists. Use the 'forced' option to override.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            ArenaSchematic existingPrefab = HytaleDuels.getInstance().getArenaManager().getSchematics()
                    .values().stream()
                    .filter(other -> other.getPrefabAssetId().equals(arenaSchematic.getPrefabAssetId()))
                    .findFirst()
                    .orElse(null);

            if (existingPrefab != null) {
                context.sendMessage(Message.raw("An arena with the prefab ID '" + prefabAssetId + "' already exists. Use the 'forced' option to override.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
        }

        return arenaSchematic.scanLocations().whenComplete((_, throwable) -> {
            if (throwable != null) {
                context.sendMessage(Message.raw("Failed to scan arena locations: " + throwable.getMessage()).color(Color.RED));
                return;
            }

            HytaleDuels.getInstance().getArenaManager().registerSchematic(arenaSchematic);
            HytaleDuels.getInstance().getArenaManager().save();

            context.sendMessage(Message.raw("Arena '" + name + "' created successfully with prefab ID '" + arenaSchematic.getPrefabAssetId() + "'.").color(Color.GREEN));
        });
    }
}
