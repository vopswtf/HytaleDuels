package cc.vops.duels.duel;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.ArenaSchematic;
import cc.vops.duels.duel.match.Match;
import cc.vops.duels.duel.match.MatchTeam;
import cc.vops.duels.kit.Kit;
import cc.vops.duels.util.PlayerUtil;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import lombok.Getter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class DuelInvite {
    private final UUID inviteId = UUID.randomUUID();
    private final UUID challengerId;

    private final String challengerName;
    private final UUID challengedId;
    private final String challengedName;

    private final Kit kit;
    private final ArenaSchematic arenaSchematic;
    private final long expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);

    public DuelInvite(final PlayerRef challengerRef, final PlayerRef challengedRef, final Kit kit, final ArenaSchematic arenaSchematic) {
        this.challengerId = challengerRef.getUuid();
        this.challengedId = challengedRef.getUuid();
        this.challengerName = challengerRef.getUsername();
        this.challengedName = challengedRef.getUsername();
        this.kit = kit;
        this.arenaSchematic = arenaSchematic;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public void accept() {
        if (isExpired()) return;

        HytaleDuels.getInstance().getDuelManager().removeInvite(this);

        World world = Universe.get().getDefaultWorld();
        assert world != null;

        PlayerRef challengerRef = PlayerUtil.getPlayer(p -> p.getUuid().equals(challengerId), world);
        PlayerRef challengedRef = PlayerUtil.getPlayer(p -> p.getUuid().equals(challengedId), world);

        if (challengerRef == null || !challengerRef.isValid()) {
            if (challengedRef != null && challengedRef.isValid()) {
                challengedRef.sendMessage(Message.raw("The player who challenged you has disconnected.").color(Color.RED));
            }
            return;
        }

        if (challengedRef == null || !challengedRef.isValid()) {
            if (challengerRef.isValid()) {
                challengerRef.sendMessage(Message.raw("The player you challenged has disconnected.").color(Color.RED));
            }
            return;
        }

        ItemWithAllMetadata icon = new ItemStack(kit.getCoverItem().getItemId(), 1).toPacket();
        NotificationUtil.sendNotification(
                challengerRef.getPacketHandler(),
                Message.raw(challengedName).color(Color.GREEN),
                Message.raw("has accepted your duel!").color(Color.GRAY),
                icon,
                NotificationStyle.Success
        );

        Match match = new Match(kit, Arrays.asList(new MatchTeam(challengerId), new MatchTeam(challengedId)), false);

        HytaleDuels.getInstance().getArenaManager().createInstance(arenaSchematic, match).thenAccept(match::onLoad).exceptionally(ex -> {
            ex.printStackTrace();

            NotificationUtil.sendNotification(
                    challengerRef.getPacketHandler(),
                    Message.raw("Error during arena generation!").color(Color.RED),
                    NotificationStyle.Danger
            );

            NotificationUtil.sendNotification(
                    challengedRef.getPacketHandler(),
                    Message.raw("Error during arena generation!").color(Color.RED),
                    NotificationStyle.Danger
            );

            return null;
        });
    }
}
