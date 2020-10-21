package net.piratebayserver.plugin.placeholders;

import net.piratebayserver.plugin.ClanBattleData;
import net.piratebayserver.plugin.ClanBattlePlugin;
import net.piratebayserver.plugin.Region;
import org.bukkit.entity.Player;

public class CandidatePlayersInRegionPlaceholdersExpansion extends AbstractPlaceholdersExpansion {


    public CandidatePlayersInRegionPlaceholdersExpansion(ClanBattlePlugin plugin) {
        super(plugin);
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier() {
        return "clanbattlecandidateplayers";
    }

    @Override
    public String onPlaceholderRequest(Player player, String regionName) {

        ClanBattleData clanBattleData = plugin.getClanBattleData();
        Region region = clanBattleData.getRegionsMap().get(regionName);
        if (region != null) {
            String candidate = region.getCandidate();
            if (candidate != null) {
                return String.valueOf(region.getClanPlayersMap().get(candidate).size());
            }
        }
        return "0";
    }
}
