package cc.vops.duels.duel;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.duel.listener.DuelBreakBlock;
import cc.vops.duels.duel.listener.DuelDeath;
import cc.vops.duels.duel.listener.DuelPlaceBlock;
import cc.vops.duels.duel.match.Match;
import cc.vops.duels.kit.Kit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DuelManager {
    private final List<DuelInvite> invites = new ArrayList<>();
    private final List<Match> matches = new ArrayList<>();

    // yay fast lookup
    private final HashMap<UUID, Match> playerMatches = new HashMap<>();

    public DuelManager() {
        HytaleDuels.getInstance().getCommandRegistry().registerCommand(new DuelCommand());

        HytaleDuels.getInstance().getEntityStoreRegistry().registerSystem(new DuelBreakBlock());
        HytaleDuels.getInstance().getEntityStoreRegistry().registerSystem(new DuelPlaceBlock());
        HytaleDuels.getInstance().getEntityStoreRegistry().registerSystem(new DuelDeath());
    }

    public void addInvite(DuelInvite invite) {
        invites.add(invite);
    }

    public void registerMatch(Match match) {
        matches.add(match);
        for (UUID uuid : match.getEveryPlayer()) {
            playerMatches.put(uuid, match);
        }
    }

    public void unregisterMatch(Match match) {
        matches.remove(match);
        for (UUID uuid : match.getEveryPlayer()) {
            // incase they join another match after leaving this one (since others can still be alive in old)
            if (playerMatches.get(uuid) == match) {
                playerMatches.remove(uuid);
            }
        }
    }

    public Match getMatchByPlayer(UUID playerId) {
        return playerMatches.get(playerId);
    }

    public Match getMatchByWorld(World world) {
        for (Match match : matches) {
            if (match.getArenaInstance() != null && match.getArenaInstance().getWorld() != null && match.getArenaInstance().getWorld().equals(world)) {
                return match;
            }
        }
        return null;
    }

    public DuelInvite getSentInvite(UUID challengerId, UUID challengedId, Kit kit) {
        for (DuelInvite invite : invites) {
            if (invite.getChallengerId().equals(challengerId) && invite.getChallengedId().equals(challengedId) && invite.getKit().equals(kit)) {
                return invite;
            }
        }
        return null;
    }

    public DuelInvite getReceivedInvite(UUID challengedId, UUID challengerId, Kit kit) {
        for (DuelInvite invite : invites) {
            if (invite.getChallengedId().equals(challengedId) && invite.getChallengerId().equals(challengerId) && invite.getKit().equals(kit)) {
                return invite;
            }
        }
        return null;
    }

    public DuelInvite getInviteById(UUID id) {
        for (DuelInvite invite : invites) {
            if (invite.getInviteId().equals(id)) {
                return invite;
            }
        }
        return null;
    }

    public void removeInvite(DuelInvite invite) {
        invites.remove(invite);
    }
}
