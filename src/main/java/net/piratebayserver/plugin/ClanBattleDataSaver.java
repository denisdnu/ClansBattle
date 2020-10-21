package net.piratebayserver.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClanBattleDataSaver {
    public static final String DIR = "plugins/ClanBattle/";
    public static final String FILE_PATH = "ClanBattleData.json";
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ClanBattleData loadData() {
        ClanBattleData data;
        try {
            JsonReader reader = new JsonReader(new FileReader(DIR + FILE_PATH));
            data = GSON.fromJson(reader, ClanBattleData.class);
            return data;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            data = saveData(new ClanBattleData());
        }
        return data;
    }

    public static ClanBattleData saveData(ClanBattleData data) {
        File directory = new File(DIR);
        if (!directory.exists()) {
            directory.mkdir();
        }
        try {
            String json = GSON.toJson(data);
            BufferedWriter writer = new BufferedWriter(new FileWriter(DIR + FILE_PATH));
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return data;
    }
}
