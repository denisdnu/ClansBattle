package net.piratebayserver.plugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClanBattleData implements Serializable {
    private static transient final long serialVersionUID = -1681012206529286330L;
    private final Map<String, Region> regionsMap = new HashMap<>();

    public ClanBattleData() {
    }

    public ClanBattleData(ClanBattleData loadedData) {
        regionsMap.putAll(loadedData.getRegionsMap());
    }

    public Map<String, Region> getRegionsMap() {
        return regionsMap;
    }
}
