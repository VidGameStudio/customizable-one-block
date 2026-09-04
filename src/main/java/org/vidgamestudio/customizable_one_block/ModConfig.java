package org.vidgamestudio.customizable_one_block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static ConfigData INSTANCE = new ConfigData();

    public static class ConfigData {
        public Map<String, Category> categories = new HashMap<>();
    }

    public static class Category {
        public String color = "#FFFFFF";
        public List<Phase> phases = new ArrayList<>();
    }


    public static class Phase {
        public int blocks_required;
        public List<String> blocks = new ArrayList<>();
        public List<String> loot_chest = new ArrayList<>();
        // ДОБАВЛЯЕМ СПИСОК МОБОВ:
        public List<String> monsters = new ArrayList<>();
    }



    public static void load() {
        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "customizable_one_block.json");
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
                ConfigData defaultData = new ConfigData();

                // Дефолтный Майнкрафт с новыми шансами
                Category mcCat = new Category();
                mcCat.color = "#FFFFFF";

                Phase phase0 = new Phase();
                phase0.blocks_required = 0;
                phase0.blocks.add("minecraft:stone:50");
                phase0.blocks.add("minecraft:dirt:20");
                phase0.blocks.add("minecraft:oak_log:20");
                phase0.blocks.add("minecraft:chest:5"); // Админ сам заносит сундук в пул блоков!
                phase0.loot_chest.add("1;minecraft:apple;3:100"); // Мин 1, Макс 3, Шанс 100
                phase0.loot_chest.add("1;minecraft:coal;5:50");
                mcCat.phases.add(phase0);

                defaultData.categories.put("minecraft", mcCat);

                try (FileWriter writer = new FileWriter(configFile)) {
                    GSON.toJson(defaultData, writer);
                }
                INSTANCE = defaultData;
            } else {
                try (FileReader reader = new FileReader(configFile)) {
                    INSTANCE = GSON.fromJson(reader, ConfigData.class);
                    if (INSTANCE == null || INSTANCE.categories == null) INSTANCE = new ConfigData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new ConfigData();
        }
    }
}
