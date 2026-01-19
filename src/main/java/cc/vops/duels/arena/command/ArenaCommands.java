package cc.vops.duels.arena.command;

import cc.vops.duels.arena.page.ArenasPage;
import cc.vops.duels.util.AbstractRootCommandCollection;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ArenaCommands extends AbstractRootCommandCollection {
    public ArenaCommands() {
        super("arena", "Manage arenas.");
        this.requirePermission("duels.command.arena.admin");
        this.addAliases("arenas");
        this.addSubCommand(new ArenaCreateCommand());
        this.addSubCommand(new ArenaTestInstanceCommand());
    }

    @Override
    protected void executeRoot(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        final Player playerComponent = store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        playerComponent.getPageManager().openCustomPage(ref, store, new ArenasPage(playerRef));
    }
}
