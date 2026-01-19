package cc.vops.duels.duel.match;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.ArenaInstance;
import cc.vops.duels.kit.Kit;
import cc.vops.duels.util.PlayerUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class Match {
    private final String id;

    @Setter
    private Kit kit;
    @Setter
    private ArenaInstance arenaInstance;

    @Setter private MatchTeam winner;
    private final List<MatchTeam> teams;
    private EndReason endReason;
    private State state;
    private Date startedAt;
    private Date endedAt;
    private final boolean ranked; // TODO: ranked?

    // this references every player that has ever been in the match
    // make sure to only use this for non-alive player lookups
    private final List<UUID> everyPlayer = new ArrayList<>();

    private ScheduledFuture<?> countdownTask;

    public Match(Kit kit, List<MatchTeam> teams, boolean ranked) {
        this.id = UUID.randomUUID().toString();
        this.kit = kit;
        this.teams = List.copyOf(teams);
        this.ranked = ranked;

        for (int i = 0; i < teams.size(); i++) {
            MatchTeam team = teams.get(i);
            team.setTeamNumber(i + 1);
        }

        for (MatchTeam team : teams) {
            everyPlayer.addAll(team.getAliveMembers());
        }
    }

    public enum State {
        COUNTDOWN,
        IN_PROGRESS,
        ENDING,
        TERMINATED
    }

    public enum EndReason {
        ENEMIES_ELIMINATED,
        DURATION_LIMIT_EXCEEDED
    }

    public void onLoad(ArenaInstance arenaInstance) {
        this.arenaInstance = arenaInstance;

        HytaleDuels.getInstance().getDuelManager().registerMatch(this);
        startCountdown();
    }

    private void startCountdown() {
        this.state = State.COUNTDOWN;

        List<PlayerRef> allPlayers = getAllPlayers();
        for (PlayerRef playerRef : allPlayers) {
            if (playerRef.getReference() == null) continue;
            playerRef.getReference().getStore().getExternalData().getWorld().execute(() -> {
                final Player playerComponent = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
                kit.applyTo(playerComponent);
                arenaInstance.teleport(playerRef);
            });
        }

        AtomicInteger countdown = new AtomicInteger(4);
        countdownTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            int timeLeft = countdown.getAndDecrement();
            if (timeLeft <= 0 && state == State.COUNTDOWN) {
                onStart();
                return;
            }

            broadcastSound("SFX_Daggers_T1_Slash_Impact");
            broadcast(Message.raw("Starting in " + timeLeft + "...").color(Color.YELLOW));
        }, 0L, 1L, TimeUnit.SECONDS);
    }

    public MatchTeam getTeam(UUID playerUuid) {
        for (MatchTeam team : teams) {
            if (team.isAlive(playerUuid)) {
                return team;
            }
        }

        return null;
    }

    public void markDead(PlayerRef player) {
        MatchTeam team = getTeam(player.getUuid());
        if (team == null) return;

        team.markDead(player.getUuid());
        checkEnded();
    }

    public void checkEnded() {
        if (state == State.ENDING || state == State.TERMINATED) {
            return;
        }

        List<MatchTeam> teamsAlive = new ArrayList<>();

        for (MatchTeam team : teams) {
            if (!team.getAliveMembers().isEmpty()) {
                teamsAlive.add(team);
            }
        }

        if (teamsAlive.size() == 1) {
            this.winner = teamsAlive.get(0);
            endMatch(EndReason.ENEMIES_ELIMINATED);
        }

        if (teamsAlive.isEmpty()) {
            endMatch(EndReason.DURATION_LIMIT_EXCEEDED);
        }
    }

    public void endMatch(EndReason reason) {
        if (state == State.ENDING || state == State.TERMINATED) {
            return;
        }

        state = State.ENDING;
        endedAt = new Date();
        endReason = reason;

        broadcast(Message.raw("Match ended.").color(Color.RED));
        HytaleServer.SCHEDULED_EXECUTOR.schedule(this::terminateMatch, 5L, TimeUnit.SECONDS);
    }

    private void terminateMatch() {
        this.state = State.TERMINATED;

        World defaultWorld = Universe.get().getDefaultWorld();
        assert defaultWorld != null;

        for (PlayerRef player : this.arenaInstance.getWorld().getPlayerRefs()) {
            if (player.getReference() == null) continue;
            Transform loc = defaultWorld.getWorldConfig().getSpawnProvider().getSpawnPoint(defaultWorld, player.getUuid());
            PlayerUtil.teleport(player.getReference(), defaultWorld, loc.getPosition(), loc.getRotation());
        }

        HytaleDuels.getInstance().getDuelManager().unregisterMatch(this);
        this.arenaInstance.destroy();
    }

    public void onStart() {
        if (countdownTask != null) countdownTask.cancel(true);

        this.state = State.IN_PROGRESS;
        startedAt = new Date();

        broadcastSound("SFX_Glass_Break");
        broadcast(Message.raw("The match has started!").color(Color.GREEN));

        for (PlayerRef allPlayer : getAllPlayers()) {
            if (teams.size() == 2) {
                MatchTeam teamOne = teams.get(0);
                MatchTeam teamTwo = teams.get(1);

                if (teamOne.contains(allPlayer.getUuid())) {
                    EventTitleUtil.showEventTitleToPlayer(allPlayer, Message.raw(teamTwo.getName()), Message.raw(kit.getName()), true);
                } else if (teamTwo.contains(allPlayer.getUuid())) {
                    EventTitleUtil.showEventTitleToPlayer(allPlayer, Message.raw(teamOne.getName()), Message.raw(kit.getName()), true);
                }
            } else {
                EventTitleUtil.showEventTitleToPlayer(allPlayer, Message.raw("Free For All"), Message.raw(kit.getName()), true);
            }
        }
    }

    public void broadcast(Message message) {
        for (PlayerRef playerRef : arenaInstance.getWorld().getPlayerRefs()) {
            playerRef.sendMessage(message);
        }
    }

    public void broadcastSound(String sound) {
        for (PlayerRef playerRef : arenaInstance.getWorld().getPlayerRefs()) {
            PlayerUtil.playSound(playerRef, sound);
        }
    }

    // Getters
    public List<PlayerRef> getAllPlayers() {
        List<PlayerRef> refs = new ArrayList<>();

        for (MatchTeam team : teams) {
            for (UUID memberUuid : team.getAliveMembers()) {
                PlayerRef ref = Universe.get().getPlayer(memberUuid);
                if (ref != null) {
                    refs.add(ref);
                }
            }
        }

        return refs;
    }
}
