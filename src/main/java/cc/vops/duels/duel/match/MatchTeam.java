package cc.vops.duels.duel.match;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class MatchTeam {
    private final Set<UUID> allMembers;
    private final Set<UUID> aliveMembers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @Setter
    private int teamNumber = -1;

    public MatchTeam(UUID initialMember) {
        this(Set.of(initialMember));
    }

    public MatchTeam(Collection<UUID> initialMembers) {
        this.allMembers = Set.copyOf(initialMembers);
        this.aliveMembers.addAll(initialMembers);
    }

    public boolean contains(UUID playerUuid) {
        return allMembers.contains(playerUuid);
    }

    public void markDead(UUID playerUuid) {
        aliveMembers.remove(playerUuid);
    }

    public String getName() {
        if (aliveMembers.size() == 1) {
            PlayerRef ref = Universe.get().getPlayer(aliveMembers.iterator().next());
            return ref != null ? ref.getUsername() : "Team " + teamNumber;
        } else {
            return "Team " + teamNumber;
        }
    }

    public boolean isAlive(UUID playerUuid) {
        return aliveMembers.contains(playerUuid);
    }

    public Set<UUID> getAliveMembers() {
        return Set.copyOf(aliveMembers);
    }

    public void messageAlive(Message message) {
        forEachAlive(playerRef -> playerRef.sendMessage(message));
    }

    private void forEachAlive(Consumer<PlayerRef> consumer) {
        for (UUID member : aliveMembers) {
            PlayerRef memberRef = Universe.get().getPlayer(member);

            if (memberRef != null) {
                consumer.accept(memberRef);
            }
        }
    }
}
