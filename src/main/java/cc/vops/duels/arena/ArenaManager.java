package cc.vops.duels.arena;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.command.ArenaCommands;
import cc.vops.duels.arena.page.ArenasPage;
import cc.vops.duels.arena.page.ArenasPageSupplier;
import cc.vops.duels.duel.match.Match;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class ArenaManager {
    private final HashMap<UUID, ArenaSchematic> schematics = new HashMap<>();
    private final HashMap<String, ArenaSchematic> byName = new HashMap<>();

    private final HashMap<UUID, ArenaInstance> activeArenas = new HashMap<>();

    public ArenaManager() {
        OpenCustomUIInteraction.registerCustomPageSupplier(HytaleDuels.getInstance(), ArenasPage.class, "Arenas", new ArenasPageSupplier());

        final CommandRegistry commandRegistry = HytaleDuels.getInstance().getCommandRegistry();
        commandRegistry.registerCommand(new ArenaCommands());

        Path arenaConfig = HytaleDuels.getInstance().getDataDirectory().resolve("arenas.json");
        if (arenaConfig.toFile().exists()) {
            try {
                load(arenaConfig.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void load(File file) throws IOException {
        String contents = Files.readString(file.toPath());
        for (JsonElement schematic : HytaleDuels.GSON.fromJson(contents, JsonArray.class)) {
            ArenaSchematic arenaSchematic = ArenaSchematic.deserialize(schematic.getAsJsonObject());
            registerSchematic(arenaSchematic);
        }
    }

    public void save() {
        Path arenaConfig = HytaleDuels.getInstance().getDataDirectory().resolve("arenas.json");
        JsonArray array = new JsonArray();
        for (ArenaSchematic schematic : schematics.values()) {
            array.add(schematic.serialize());
        }
        try {
            Files.writeString(arenaConfig, HytaleDuels.GSON.toJson(array));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerSchematic(ArenaSchematic schematic) {
        schematics.put(schematic.getId(), schematic);
        byName.put(schematic.getName().toLowerCase(), schematic);
    }

    public ArenaSchematic getSchematicByName(String name) {
        return byName.get(name.toLowerCase());
    }

    public CompletableFuture<ArenaInstance> createInstance(ArenaSchematic schematic, Match match) {
        CompletableFuture<ArenaInstance> future = new CompletableFuture<>();

        UUID instanceId = UUID.randomUUID();

        Universe.get().makeWorld(
                "arena-" + instanceId,
                HytaleDuels.getInstance().getDataDirectory().resolve("arenas").resolve(instanceId.toString()),
                schematic.createWorldConfig()
        ).thenAccept(world -> {
            ArenaInstance arenaInstance = new ArenaInstance(instanceId, match, schematic, world);
            arenaInstance.placePrefab().thenRun(() -> {
                arenaInstance.initSpawnProvider();
                activeArenas.put(instanceId, arenaInstance);
                future.complete(arenaInstance);
            }).exceptionally(t -> {
                future.completeExceptionally(t);
                return null;
            });
        }).exceptionally(t -> {
            future.completeExceptionally(t);
            return null;
        }).exceptionally(t -> {
            future.completeExceptionally(t);
            return null;
        });

        return future;
    }

    public ArenaSchematic getRandomArena() {
        if (schematics.isEmpty()) return null;
        return schematics.values().stream().skip((int) (schematics.size() * Math.random())).findFirst().orElse(null);
    }
}
