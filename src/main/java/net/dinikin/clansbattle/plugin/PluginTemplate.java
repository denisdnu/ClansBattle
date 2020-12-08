package net.dinikin.clansbattle.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class PluginTemplate extends JavaPlugin {

    public void onEnable() {
        loadConfig();
        registerCommands();
    }

    void loadConfig() {

    }

    private void registerCommands() {
    }


}
