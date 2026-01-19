package cc.vops.duels.kit;

import cc.vops.duels.HytaleDuels;
import cc.vops.duels.kit.command.KitCommands;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class KitManager {
    private final HashMap<String, Kit> byId = new HashMap<>();

    public KitManager() {
        HytaleDuels.getInstance().getCommandRegistry().registerCommand(new KitCommands());
        reload();
    }

    public void reload() {
        this.byId.clear();

        Path kitsPath = HytaleDuels.getInstance().getDataDirectory().resolve("kits");
        File kitsDir = kitsPath.toFile();
        if (!kitsDir.exists()) {
            kitsDir.mkdirs();
        }

        for (File file : Objects.requireNonNull(kitsDir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                try {
                    String contents = Files.readString(file.toPath());
                    Kit kit = Kit.fromJson(HytaleDuels.GSON.fromJson(contents, JsonObject.class));
                    registerKit(kit);
                    System.out.println("Loaded kit: " + kit.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void save() {
        Path kitsPath = HytaleDuels.getInstance().getDataDirectory().resolve("kits");
        File kitsDir = kitsPath.toFile();
        if (!kitsDir.exists()) {
            kitsDir.mkdirs();
        }

        for (Kit kit : byId.values()) {
            try {
                Path kitPath = kitsPath.resolve(kit.getId() + ".json");
                Files.writeString(kitPath, HytaleDuels.GSON.toJson(kit.toJson()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<Kit> getAllKits() {
        return new ArrayList<>(byId.values());
    }

    public void registerKit(Kit kit) {
        byId.put(kit.getId().toLowerCase(), kit);
    }

    public Kit getKitById(String name) {
        return byId.get(name.toLowerCase());
    }
}
