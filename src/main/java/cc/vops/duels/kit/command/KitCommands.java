package cc.vops.duels.kit.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;


public class KitCommands extends AbstractCommandCollection {
    public KitCommands() {
        super("kit", "Manage kits.");
        this.requirePermission("duels.command.kit.admin");

        this.addSubCommand(new KitCreateCommand());
        this.addSubCommand(new KitSetNameCommand());
        this.addSubCommand(new KitSetInventoryCommand());
        this.addSubCommand(new KitCloneInventoryCommand());
        this.addSubCommand(new KitMenuCommand());
    }
}
