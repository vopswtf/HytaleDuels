package cc.vops.duels.arena;

import cc.vops.duels.duel.match.Match;
import cc.vops.duels.duel.match.MatchTeam;
import cc.vops.duels.util.PlayerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.BlockPlaceUtils;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.PrefabBuffer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.spawn.IndividualSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PrefabUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter @RequiredArgsConstructor
public class ArenaInstance {
    private final UUID instanceId;
    private final Match match;
    private final ArenaSchematic schematic;
    private final World world;

    public CompletableFuture<Void> placePrefab() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        final PrefabBuffer prefabBuffer = PrefabBufferUtil.loadBuffer(PrefabStore.get().getServerPrefabsPath().resolve(schematic.getPrefabAssetId()));

        assert world != null;

        world.execute(() -> {
            try {
                final Store<EntityStore> store = world.getEntityStore().getStore();
                final PrefabBuffer.PrefabBufferAccessor prefabBufferAccessor = prefabBuffer.newAccess();

                try {
                    PrefabUtil.paste(prefabBufferAccessor, world, ArenaSchematic.PASTE_POSITION, Rotation.None,true, new Random(), store);
                    future.complete(null);
                } finally {
                    prefabBufferAccessor.release();
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    public void initSpawnProvider() {
        ArenaSchematic.ArenaBlockSpawn teamOne = schematic.getTeamOneBlockSpawn();
        world.setBlock(teamOne.x, teamOne.y, teamOne.z, "Empty");

        ArenaSchematic.ArenaBlockSpawn teamTwo = schematic.getTeamTwoBlockSpawn();
        world.setBlock(teamTwo.x, teamTwo.y, teamTwo.z, "Empty");

        world.getWorldConfig().setSpawnProvider(new ISpawnProvider() {
            @Override
            public Transform getSpawnPoint(@Nonnull World world, @Nonnull UUID uuid) {
                if (match.getTeams().size() == 2) {
                    MatchTeam team = match.getTeam(uuid);
                    if (team != null) {
                        if (team.getTeamNumber() == 1) {
                            return schematic.getTeamOneSpawn().clone();
                        } else if (team.getTeamNumber() == 2) {
                            return schematic.getTeamTwoSpawn().clone();
                        }
                    }
                }

                return new Transform(0, 150, 0);
            }

            @Override
            public Transform[] getSpawnPoints() {
                return new Transform[] { schematic.getTeamOneSpawn().clone(), schematic.getTeamTwoSpawn().clone() };
            }

            @Override
            public boolean isWithinSpawnDistance(@Nonnull Vector3d vector3d, double v) {
                return false;
            }
        });
    }

    public void destroy() {
        Universe.get().removeWorld(world.getName());
    }


    public void teleport(PlayerRef playerRef) {
        Transform spawn = world.getWorldConfig()
                .getSpawnProvider()
                .getSpawnPoint(world, playerRef.getUuid())
                .clone();

        if (playerRef.getReference() != null && playerRef.isValid()) {
            PlayerUtil.teleport(playerRef.getReference(), world, spawn.getPosition(), spawn.getRotation());
        }
    }

}
