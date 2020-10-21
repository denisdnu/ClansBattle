package net.piratebayserver.plugin.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.piratebayserver.plugin.ClanBattlePlugin;
import org.bukkit.entity.Player;

public abstract class AbstractPlaceholdersExpansion extends PlaceholderExpansion {

    ClanBattlePlugin plugin;

    public AbstractPlaceholdersExpansion(ClanBattlePlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean persist() {
        return true;
    }

    /**
     * This method should always return true unless we
     * have a dependency we need to make sure is on the server
     * for our placeholders to work!
     *
     * @return always true since we do not have any dependencies.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }


    /**
     * This is the version of this expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion() {
        return "1.0.0";
    }

}
