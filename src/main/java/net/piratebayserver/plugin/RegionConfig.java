package net.piratebayserver.plugin;

import org.bukkit.ChatColor;

public class RegionConfig {
    private final int timeToCapture;
    private final int timeToKeep;
    private final int minClanPlayers;
    private final int playerPayout;
    private final int clanPayout;
    private final int period;
    private final String permToSet;
    private final String region;
    private final String alias;
    private final String msgPrefix;
    private final String ownerSuffix;

    public RegionConfig(int timeToCapture, int timeToKeep, int minClanPlayers, String permToSet, int playerPayout, int clanPayout,
                        int period, String region, String alias, String msgPrefix, String ownerSuffix) {
        this.timeToCapture = timeToCapture;
        this.timeToKeep = timeToKeep;
        this.minClanPlayers = minClanPlayers;
        this.permToSet = permToSet;
        this.playerPayout = playerPayout;
        this.clanPayout = clanPayout;
        this.period = period;
        this.region = region;
        this.alias = alias;
        this.msgPrefix = msgPrefix;
        this.ownerSuffix = ownerSuffix;

    }

    public String getAlias() {
        return alias;
    }

    public String getRegion() {
        return region;
    }

    public String getMsgPrefix() {
        return msgPrefix;
    }

    public String getOwnerSuffix() {
        return ownerSuffix;
    }

    public int getTimeToCapture() {
        return timeToCapture;
    }

    public int getTimeToKeep() {
        return timeToKeep;
    }

    public int getMinClanPlayers() {
        return minClanPlayers;
    }

    public String getPermToSet() {
        return permToSet;
    }

    public int getPlayerPayout() {
        return playerPayout;
    }

    public int getClanPayout() {
        return clanPayout;
    }

    public int getPeriod() {
        return period;
    }
}