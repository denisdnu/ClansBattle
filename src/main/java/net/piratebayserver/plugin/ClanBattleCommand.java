package net.piratebayserver.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.logging.Logger;

public class ClanBattleCommand implements CommandExecutor {
    private final ClanBattlePlugin plugin;
    private static final Logger log = Logger.getLogger("ClanBattleCommand");


    public ClanBattleCommand(ClanBattlePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args[0]!= null && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.loadConfig();
            sender.sendMessage(ChatColor.RED + "[" + ChatColor.GREEN + "ClanBattle" + ChatColor.RED + "]"
                    + ChatColor.GREEN + " Конфигурация перезагружена успешно.");
        }
        return true;
    }

}
