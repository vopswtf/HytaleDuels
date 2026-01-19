package cc.vops.duels.kit.command;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.arena.page.ArenasPage;
import cc.vops.duels.kit.Kit;
import cc.vops.duels.kit.KitManager;
import cc.vops.duels.kit.page.SelectKitPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;

public class KitMenuCommand extends AbstractPlayerCommand {

    public KitMenuCommand() {
        super("menu", "Open the kit selection menu.");
        this.requirePermission("duels.command.kit.menu");
    }

    @Nonnull
    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store, @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final Player playerComponent = store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        playerComponent.getPageManager().openCustomPage(ref, store, new SelectKitPage(playerRef, CustomPageLifetime.CanDismiss, HytaleDuels.getInstance().getKitManager().getAllKits(), (kit) -> {
            context.sendMessage(Message.raw("You selected kit: " + kit.getName()).color(Color.GREEN));
        }));
    }
}
