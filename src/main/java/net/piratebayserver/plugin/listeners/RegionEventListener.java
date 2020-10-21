package net.piratebayserver.plugin.listeners;

import com.mewin.WGRegionEvents.events.RegionEnterEvent;
import com.mewin.WGRegionEvents.events.RegionLeaveEvent;
import me.jose.finalclans.AdvancedClans;
import me.jose.finalclans.ClansAPI;
import me.jose.finalclans.listeners.custom.AsyncClanUpdateEvent;
import me.jose.finalclans.listeners.custom.AsyncPlayerDataLoadEvent;
import me.jose.finalclans.objects.ClanPlayer;
import net.piratebayserver.plugin.ClanBattlePlugin;
import net.piratebayserver.plugin.REGION_STATUS;
import net.piratebayserver.plugin.Region;
import net.piratebayserver.plugin.RegionConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RegionEventListener implements Listener {
    ClanBattlePlugin plugin;
    private static Set<String> playersDataLoaded = new HashSet<>();

    public RegionEventListener(ClanBattlePlugin clanBattlePlugin) {
        this.plugin = clanBattlePlugin;
    }

    @EventHandler
    void onRegionEnter(RegionEnterEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        String regionName = event.getRegion().getId();
        if (regionConfigMap.containsKey(regionName)) {
            RegionConfig regionConfig = regionConfigMap.get(regionName);
            if (plugin.getRegionsMap().containsKey(regionName) && ClansAPI.hasClan(event.getPlayer(), true)) {
                Region region = plugin.getRegionsMap().get(regionName);
                AdvancedClans advancedClans = AdvancedClans.getInstance();
                Map<String, ClanPlayer> clanPlayersMap = advancedClans.getPlayers().stream()
                        .collect(Collectors.toMap(ClanPlayer::getName, Function.identity()));
                advancedClans.onLoad();
                Player player = event.getPlayer();
                String playerName = player.getName();
                if (clanPlayersMap.containsKey(playerName)) {
                    ClanPlayer clanPlayer = clanPlayersMap.get(playerName);
                    if (clanPlayer.getClan() == null) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            wailForPlayerClanDataLoad(playerName);
                            if (clanPlayer.getClan() != null) {
                                registerPlayerInRegion(event, regionName, regionConfig, region, playerName, clanPlayer);
                            }
                        });
                    }
                    if (clanPlayer.getClan() != null) {
                        registerPlayerInRegion(event, regionName, regionConfig, region, playerName, clanPlayer);
                    }

                }
            }
        }

    }

    private void registerPlayerInRegion(PlayerEvent event, String regionName,
                                        RegionConfig regionConfig, Region region,
                                        String playerName, ClanPlayer clanPlayer) {
        String clanName = clanPlayer.getClan().getName();
        addPlayerToRegion(event, regionName, clanName);
        Map<String, Set<String>> regionClanPlayersMap = region.getClanPlayersMap();
        regionClanPlayersMap.putIfAbsent(clanName, new HashSet<>());
        regionClanPlayersMap.get(clanName).add(playerName);
        String candidate = findCandidate(regionClanPlayersMap, regionConfig);
        updateRegionState(region, clanName, candidate);
    }

    private void wailForPlayerClanDataLoad(String playerName) {
        while (!playersDataLoaded.contains(playerName)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        playersDataLoaded.remove(playerName);
    }

    private String findCandidate(Map<String, Set<String>> regionClanPlayersMap, RegionConfig regionConfig) {
        int playersToCapture = regionConfig.getMinClanPlayers();
        Set<String> candidate = regionClanPlayersMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= playersToCapture)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (candidate.size() == 1) {
            return candidate.iterator().next();
        }
        return null;

    }

    @EventHandler
    void onRegionLeave(RegionLeaveEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        String regionName = event.getRegion().getId();
        if (regionConfigMap.containsKey(regionName)) {
            onPlayerLeave(event, regionConfigMap, regionName);
        }

    }

    private void onPlayerLeave(PlayerEvent event, Map<String, RegionConfig> regionConfigMap, String regionName) {
        RegionConfig regionConfig = regionConfigMap.get(regionName);
        if (plugin.getRegionsMap().containsKey(regionName)) {
            Region region = plugin.getRegionsMap().get(regionName);
            AdvancedClans advancedClans = AdvancedClans.getInstance();
            Map<String, ClanPlayer> clanPlayersMap = advancedClans.getPlayers().stream()
                    .collect(Collectors.toMap(ClanPlayer::getName, Function.identity()));
            String playerName = event.getPlayer().getName();
            if (clanPlayersMap.containsKey(playerName)) {
                ClanPlayer clanPlayer = clanPlayersMap.get(playerName);
                if (clanPlayer.getClan() == null) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        wailForPlayerClanDataLoad(playerName);
                        if (clanPlayer.getClan() != null) {
                            unregisterPlayerInRegion(event, regionName, regionConfig, region, playerName, clanPlayer);
                        }
                    });
                }
                if (clanPlayer.getClan() != null) {
                    unregisterPlayerInRegion(event, regionName, regionConfig, region, playerName, clanPlayer);
                }
            }
        }
    }

    @EventHandler
    void onPlayerLeaveLeave(PlayerQuitEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        regionConfigMap.keySet().forEach(regionName -> {
            if (plugin.getRegionsMap().containsKey(regionName)) {
                onPlayerLeave(event, regionConfigMap, regionName);
            }
        });
    }

    private void unregisterPlayerInRegion(PlayerEvent event, String regionName, RegionConfig regionConfig,
                                          Region region, String playerName, ClanPlayer clanPlayer) {
        String clanName = clanPlayer.getClan().getName();
        removePlayerFromRegion(event, regionName, clanName);
        Map<String, Set<String>> regionClanPlayersMap = region.getClanPlayersMap();
        regionClanPlayersMap.putIfAbsent(clanName, new HashSet<>());
        regionClanPlayersMap.get(clanName).remove(playerName);
        String candidate = findCandidate(regionClanPlayersMap, regionConfig);
        updateRegionState(region, clanName, candidate);
    }

    @EventHandler
    public void onDataLoad(AsyncPlayerDataLoadEvent event) {
        playersDataLoaded.add(event.getPlayer().getName());
    }

    @EventHandler
    public void onClanDelete(AsyncClanUpdateEvent event) {
        playersDataLoaded.add(event.getPlayer().getName());
    }

    private void updateRegionState(Region region, String clanName, String candidate) {
        if (candidate != null) {
            if (!candidate.equalsIgnoreCase(region.getCandidate()) && !candidate.equalsIgnoreCase(region.getOwner())) {
                region.setCandidate(candidate);
            }
        } else {
            region.removeCandidate();
        }
        if (clanName.equalsIgnoreCase(region.getCandidate())) {
            region.setLastCandidateVisitTime(LocalDateTime.now());
        }
        if (region.getOwner() != null && clanName.equalsIgnoreCase(region.getOwner())) {
            region.setLastOwnerVisitTime(LocalDateTime.now());
        }
    }


    private void addPlayerToRegion(PlayerEvent event, String regionName, String clanName) {
        Map<String, Region> regionsMap = plugin.getRegionsMap();
        if (regionsMap.containsKey(regionName)) {
            Region region = regionsMap.get(regionName);
            Map<String, Set<String>> clanPlayersMap = region.getClanPlayersMap();
            clanPlayersMap.putIfAbsent(clanName, new HashSet<>());
            Set<String> players = clanPlayersMap.get(clanName);
            String playerName = event.getPlayer().getName();
            players.add(playerName);
            updateVisitTime(clanName, region);
        }
    }

    private void updateVisitTime(String clanName, Region region) {
        if (region.getOwner() != null && clanName.equalsIgnoreCase(region.getOwner())) {
            region.setLastCandidateVisitTime(LocalDateTime.now());
        }
    }

    private void removePlayerFromRegion(PlayerEvent event, String regionName, String clanName) {
        Map<String, Region> regionsMap = plugin.getRegionsMap();
        if (regionsMap.containsKey(regionName)) {
            Region region = regionsMap.get(regionName);
            Map<String, Set<String>> clanPlayersMap = region.getClanPlayersMap();
            clanPlayersMap.putIfAbsent(clanName, new HashSet<>());
            Set<String> players = clanPlayersMap.get(clanName);
            String playerName = event.getPlayer().getName();
            players.remove(playerName);
            updateVisitTime(clanName, region);
        }
    }

}
