package net.piratebayserver.plugin;

import java.util.HashMap;
import java.util.Map;

public class PluginConfig {
    private final Map<String, RegionConfig> regionConfigMap = new HashMap<>();

    public PluginConfig() {
    }

    public void addRegionConfig(RegionConfig regionConfig) {
        regionConfigMap.put(regionConfig.getRegion(), regionConfig);
    }

    public Map<String, RegionConfig> getRegionConfigMap() {
        return regionConfigMap;
    }


}
