package cc.vops.duels.arena;

import cc.vops.duels.HytaleDuels;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.PrefabListAsset;
import com.hypixel.hytale.server.core.prefab.PrefabRotation;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferCall;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.PrefabBuffer;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.provider.EmptyChunkStorageProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider;
import com.hypixel.hytale.server.core.util.BsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

@Getter @Setter
public class ArenaSchematic {
    public static final String SPAWN_BLOCK = "Furniture_Construction_Sign";
    public static final Vector3i PASTE_POSITION = new Vector3i(0, 100, 0);

    private final UUID id; // so nothing clashes if renamed
    private String name;
    private String prefabAssetId;
    private Transform teamOneSpawn;
    private Transform teamTwoSpawn;

    public ArenaSchematic(String name, String prefabAssetId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.prefabAssetId = prefabAssetId;
    }

    public ArenaSchematic(UUID id, String name, String prefabAssetId) {
        this.id = id;
        this.name = name;
        this.prefabAssetId = prefabAssetId;
    }

    /**
     * in case of format updates check here lol
     * @see com.hypixel.hytale.server.core.prefab.selection.buffer.BsonPrefabBufferDeserializer
     */
    public CompletableFuture<Void> scanLocations() {
        return CompletableFuture.runAsync(() -> {
            Path path = PrefabStore.get().getServerPrefabsPath().resolve(prefabAssetId);
            if (!path.toFile().exists()) {
                System.err.println("Prefab with ID '" + prefabAssetId + "' does not exist. Failed to scan arena locations.");
                return;
            }

            try {
                String contents = Files.readString(path);
                JsonObject prefabJson = HytaleDuels.GSON.fromJson(contents, JsonObject.class);

                Vector3i anchor = new Vector3i();
                anchor.x = prefabJson.get("anchorX").getAsInt();
                anchor.y = prefabJson.get("anchorY").getAsInt();
                anchor.z = prefabJson.get("anchorZ").getAsInt();

                JsonArray blocksValue = prefabJson.getAsJsonArray("blocks");
                if (blocksValue != null) {
                    for (JsonElement blockValue : blocksValue) {
                        JsonObject block = blockValue.getAsJsonObject();
                        if (!block.get("name").getAsString().equals(SPAWN_BLOCK)) continue;
                        if (block.get("filler") != null) continue; // skip fillers since its the 2nd block (sign is 2 blocks tall)

                        int realX = block.get("x").getAsInt();
                        int realY = block.get("y").getAsInt();
                        int realZ = block.get("z").getAsInt();
                        double x = realX - anchor.x + PASTE_POSITION.getX() + 0.5;
                        double y = realY - anchor.y + PASTE_POSITION.getY();
                        double z = realZ - anchor.z + PASTE_POSITION.getZ() + 0.5;

                        Transform spawnTransform = new Transform(x, y, z);
                        if (teamOneSpawn == null) {
                            teamOneSpawn = spawnTransform;
                            System.out.println("Found Team One spawn at " + spawnTransform);
                        } else if (teamTwoSpawn == null) {
                            teamTwoSpawn = spawnTransform;
                            System.out.println("Found Team Two spawn at " + spawnTransform);
                        }
                    }
                }

                if (teamOneSpawn == null) throw new CompletionException(new Exception("Missing Team One spawn point."));
                if (teamTwoSpawn == null) throw new CompletionException(new Exception("Missing Team Two spawn point."));

                makeFacing(teamOneSpawn, teamTwoSpawn);
                makeFacing(teamTwoSpawn, teamOneSpawn);
            } catch (Exception e) {
                throw new CompletionException(new Exception("Failed to scan arena locations: " + e.getMessage(), e));
            }
        });
    }

    public ArenaBlockSpawn getTeamOneBlockSpawn() {
        ArenaBlockSpawn spawn = new ArenaBlockSpawn();
        spawn.x = (int) Math.floor(teamOneSpawn.getPosition().x);
        spawn.y = (int) Math.floor(teamOneSpawn.getPosition().y);
        spawn.z = (int) Math.floor(teamOneSpawn.getPosition().z);
        return spawn;
    }

    public ArenaBlockSpawn getTeamTwoBlockSpawn() {
        ArenaBlockSpawn spawn = new ArenaBlockSpawn();
        spawn.x = (int) Math.floor(teamTwoSpawn.getPosition().x);
        spawn.y = (int) Math.floor(teamTwoSpawn.getPosition().y);
        spawn.z = (int) Math.floor(teamTwoSpawn.getPosition().z);
        return spawn;
    }

    private void makeFacing(Transform from, Transform to) {
        double dx = to.getPosition().x - from.getPosition().x;
        double dz = to.getPosition().z - from.getPosition().z;

        float angle = (float) Math.toDegrees(Math.atan2(-dx, dz)) + 180f;
        angle = ((angle + 180f) % 360f + 360f) % 360f - 180f;
        from.setRotation(new Vector3f(0f, angle, 0f));
    }

    public WorldConfig createWorldConfig() {
        WorldConfig config = new WorldConfig();
        config.setCanSaveChunks(false);
        config.setGameTimePaused(true);
        config.setSavingConfig(false);
        config.setBlockTicking(false);
        config.setChunkStorageProvider(new EmptyChunkStorageProvider());
        config.setWorldGenProvider(new VoidWorldGenProvider());
        config.setDeleteOnRemove(true);
        config.setGameMode(GameMode.Adventure);
        config.setPvpEnabled(true);
        config.setSavingPlayers(false);
        config.setIsSpawnMarkersEnabled(false);
        return config;
    }


    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id.toString());
        obj.addProperty("name", name);
        obj.addProperty("prefabAssetId", prefabAssetId);
        if (teamOneSpawn != null) {
            obj.add("teamOneSpawn", HytaleDuels.GSON.toJsonTree(teamOneSpawn));
        }
        if (teamTwoSpawn != null) {
            obj.add("teamTwoSpawn", HytaleDuels.GSON.toJsonTree(teamTwoSpawn));
        }
        return obj;
    }

    public static ArenaSchematic deserialize(JsonObject obj) {
        UUID id = UUID.fromString(obj.get("id").getAsString());
        String name = obj.get("name").getAsString();
        String prefabAssetId = obj.get("prefabAssetId").getAsString();

        ArenaSchematic schematic = new ArenaSchematic(id, name, prefabAssetId);
        if (obj.has("teamOneSpawn")) {
            schematic.setTeamOneSpawn(HytaleDuels.GSON.fromJson(obj.get("teamOneSpawn"), Transform.class));
        }

        if (obj.has("teamTwoSpawn")) {
            schematic.setTeamTwoSpawn(HytaleDuels.GSON.fromJson(obj.get("teamTwoSpawn"), Transform.class));
        }

        return schematic;
    }

    public class ArenaBlockSpawn {
        public int x;
        public int y;
        public int z;
    }
}
