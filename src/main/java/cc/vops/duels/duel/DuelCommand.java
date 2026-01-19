package cc.vops.duels.duel;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.ArenaSchematic;
import cc.vops.duels.arena.page.ArenasPage;
import cc.vops.duels.duel.page.IncomingDuelPage;
import cc.vops.duels.kit.Kit;
import cc.vops.duels.kit.page.SelectKitPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class DuelCommand extends AbstractPlayerCommand {
    @Nonnull
    private final RequiredArg<PlayerRef> targetRefArg;

    public DuelCommand() {
        super("duel", "Challenge another player to a duel.");
        this.targetRefArg = this.withRequiredArg("player", "Target Player", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store, @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        PlayerRef targetRef = this.targetRefArg.get(context);
        if (targetRef.getUuid().equals(playerRef.getUuid()) && !targetRef.getUsername().equals("vops")) {
            context.sendMessage(Message.raw("You cannot duel yourself.").color(Color.RED));
            return;
        }

        final Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        player.getPageManager().openCustomPage(ref, store, new SelectKitPage(playerRef, CustomPageLifetime.CanDismiss, HytaleDuels.getInstance().getKitManager().getAllKits(), (kit) -> {
            DuelInvite existingInvite = HytaleDuels.getInstance().getDuelManager().getSentInvite(playerRef.getUuid(), targetRef.getUuid(), kit);
            if (existingInvite != null) {
                context.sendMessage(Message.raw("You have already sent a duel invite to " + targetRef.getUsername() + ".").color(Color.RED));
                return;
            }

            DuelInvite reverseInvite = HytaleDuels.getInstance().getDuelManager().getReceivedInvite(playerRef.getUuid(), targetRef.getUuid(), kit);
            if (reverseInvite != null) {
                context.sendMessage(Message.raw(targetRef.getUsername() + " has already challenged you to a duel. Please respond to their invite.").color(Color.RED));
                return;
            }

            if (!targetRef.isValid() || !playerRef.isValid() || targetRef.getReference() == null) {
                context.sendMessage(Message.raw("The target player is not online.").color(Color.RED));
                return;
            }

            // TODO: arena selection
            ArenaSchematic arena = HytaleDuels.getInstance().getArenaManager().getRandomArena();

            final DuelInvite duelInvite = new DuelInvite(playerRef, targetRef, kit, arena);
            HytaleDuels.getInstance().getDuelManager().addInvite(duelInvite);


            context.sendMessage(Message.raw("You have challenged " + targetRef.getUsername() + " to a duel using the " + kit.getName() + " kit!").color(Color.GREEN));

            Player targetPlayer = store.getComponent(targetRef.getReference(), Player.getComponentType());
            assert targetPlayer != null;

            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                targetPlayer.getWorld().execute(() -> {
                    targetPlayer.getPageManager().openCustomPage(targetRef.getReference(), targetRef.getReference().getStore(), new IncomingDuelPage(targetRef, CustomPageLifetime.CantClose, duelInvite));
                });
            }, 400, TimeUnit.MILLISECONDS);

        }));
    }
}
