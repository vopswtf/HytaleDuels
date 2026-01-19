package cc.vops.duels;

import cc.vops.duels.arena.ArenaInstance;
import cc.vops.duels.arena.ArenaManager;
import cc.vops.duels.duel.DuelManager;
import cc.vops.duels.kit.KitManager;
import cc.vops.duels.kit.command.KitCommands;
import cc.vops.duels.util.PlayerUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Getter
public class HytaleDuels extends JavaPlugin {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

    @Getter
    private static HytaleDuels instance;

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private DuelManager duelManager;

    public HytaleDuels(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        getDataDirectory().toFile().mkdirs();

        arenaManager = new ArenaManager();
        kitManager = new KitManager();
        duelManager = new DuelManager();

        HytaleDuels.getInstance().getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> event.setBroadcastJoinMessage(false));
    }

    @Override
    protected void shutdown() {
        arenaManager.save();
        kitManager.save();

        for (ArenaInstance instance : arenaManager.getActiveArenas().values()) {
            if (instance.getWorld() != null) {
                Universe.get().removeWorld(instance.getWorld().getName());
            }
        }
    }
}