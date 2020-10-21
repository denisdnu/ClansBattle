package net.piratebayserver.plugin;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import me.jose.finalclans.AdvancedClans;
import me.jose.finalclans.objects.Clan;
import me.jose.finalclans.objects.ClanPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.piratebayserver.plugin.listeners.RegionEventListener;
import net.piratebayserver.plugin.placeholders.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClanBattlePlugin extends JavaPlugin implements Listener {

    private ClanBattleData clanBattleData = null;
    private static Economy economy = null;
    private static LuckPerms luckPerms = null;
    private static final Logger log = Logger.getLogger("Minecraft");
    private final PluginConfig pluginConfig = new PluginConfig();

    public void onEnable() {
        clanBattleData = new ClanBattleData(ClanBattleDataSaver.loadData());
        loadConfig();
        loadRegions();
        registerCommands();
        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
        }
        setupLuckPerms();
        registerPlaceholdersApi();
        Bukkit.getPluginManager().registerEvents(new RegionEventListener(this), this);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (getServer().getPluginManager().isPluginEnabled("ClanBattle")) {
                try {
                    findWinnersInRegions();
                    ClanBattleDataSaver.saveData(clanBattleData);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.severe(e.getMessage());
                }
            }
        });

    }

    private void registerPlaceholdersApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            /*
             * We register the EventListeneres here, when PlaceholderAPI is installed.
             * Since all events are in the main class (this class), we simply use "this"
             */
            new OwnerPlaceholdersExpansion(this).register();
            new DefeatedPlaceholdersExpansion(this).register();
            new RegionNamePlaceholdersExpansion(this).register();
            new OwnerPlayersInRegionPlaceholdersExpansion(this).register();
            new RegionStatusPlaceholdersExpansion(this).register();
            new CandidatePlayersInRegionPlaceholdersExpansion(this).register();
        } else {
            throw new RuntimeException("Could not find PlaceholderAPI!! Plugin can not work without it!");
        }
    }


    private void findWinnersInRegions() {
        Map<String, Region> regionsMap = clanBattleData.getRegionsMap();
        AdvancedClans advancedClans = AdvancedClans.getInstance();
        Map<String, ClanPlayer> clanPlayersMap = advancedClans.getPlayers().stream()
                .collect(Collectors.toMap(ClanPlayer::getName, Function.identity()));
        Set<String> configuredRegions = getPluginConfig().getRegionConfigMap().keySet();
        configuredRegions.forEach(regionName -> {
            String regionAlias = pluginConfig.getRegionConfigMap().get(regionName).getAlias();
            Region region = regionsMap.get(regionName);
            if (region != null) {
                long timeToCapture = pluginConfig.getRegionConfigMap().get(region.getName()).getTimeToCapture();
                int minPlayers = pluginConfig.getRegionConfigMap().get(region.getName()).getMinClanPlayers();
                int candidatePlayersNumber = region.getCandidate() == null ? 0 : region.getClanPlayersMap().get(region.getCandidate()).size();
                boolean regionIsFull = candidatePlayersNumber >= minPlayers;
                RegionConfig regionConfig = pluginConfig.getRegionConfigMap().get(region.getName());
                if (region.getLastOwnerVisitTime() != null) {
                    Duration duration = Duration.between(region.getLastOwnerVisitTime(), LocalDateTime.now());
                    long sinceCaptured = duration.getSeconds();
                    long timeToKeep = pluginConfig.getRegionConfigMap().get(region.getName()).getTimeToKeep();
                    long timeToLoose = timeToKeep - sinceCaptured;
                    boolean noOwnerInRegion = region.getOwner() == null || region.getClanPlayersMap().get(region.getOwner()).isEmpty();
                    if (timeToLoose <= timeToKeep && timeToLoose > 0 && region.getStatus() == REGION_STATUS.CAPTURED && noOwnerInRegion) {
                        notifyLoosingRegion(regionAlias, region, timeToLoose);
                    }
                    if (sinceCaptured > timeToKeep && region.getStatus() != REGION_STATUS.FREE && noOwnerInRegion) {
                        region.setFree();
                        String prefix = regionConfig.getMsgPrefix();
                        Bukkit.broadcastMessage(prefix + ChatColor.GREEN + " Регион " + ChatColor.RED + regionAlias + ChatColor.GREEN
                                + " больше ни кем не захвачен.");
                        removePermission(clanPlayersMap, region, regionConfig);
                        return;
                    }
                }
                if (region.getLastCandidateVisitTime() != null) {
                    Duration duration = Duration.between(region.getLastCandidateVisitTime(), LocalDateTime.now());
                    long sinceVisited = duration.getSeconds();
                    if ((region.getStatus() == REGION_STATUS.CAPTURING || region.getStatus() == REGION_STATUS.CAPTURED) && regionIsFull) {
                        notifyCapturingRegion(regionAlias, region, sinceVisited, candidatePlayersNumber, timeToCapture);
                        if (sinceVisited >= timeToCapture) {
                            region.setCaptured();
                            String prefix = regionConfig.getMsgPrefix();
                            Bukkit.broadcastMessage(prefix + ChatColor.GREEN + " Клан " + ChatColor.RED + region.getOwner() + ChatColor.GREEN
                                    + " захватил регион " + ChatColor.AQUA + regionAlias);
                            removePermission(clanPlayersMap, region, regionConfig);
                            setPermission(clanPlayersMap, region, regionConfig);
                            payAllClanPlayers(clanPlayersMap, region, regionConfig);

                        }

                    }
                }
                if (region.getOwner() != null) {
                    if (region.getLastPayoutTime() == null) {
                        payAllClanPlayers(clanPlayersMap, region, regionConfig);
                    } else {
                        Duration duration = Duration.between(region.getLastPayoutTime(), LocalDateTime.now());
                        long lastPayed = duration.getSeconds();
                        if (lastPayed >= regionConfig.getPeriod()) {
                            payAllClanPlayers(clanPlayersMap, region, regionConfig);
                        }
                    }
                    setPermission(clanPlayersMap, region, regionConfig);
                }

            }
        });
    }

    private void removePermission(Map<String, ClanPlayer> clanPlayersMap, Region region, RegionConfig regionConfig) {
        clanPlayersMap.values().forEach(clanPlayer -> {
            Clan clan = clanPlayer.getClan();
            if (clan != null && region.getDefeated() != null && clan.getName().equalsIgnoreCase(region.getDefeated())) {
                if (luckPerms != null) {
                    UserManager userManager = luckPerms.getUserManager();
                    CompletableFuture<User> userFuture = userManager.loadUser(clanPlayer.getUUID());
                    userFuture.thenAcceptAsync(user -> {
                        Node nodePermTrue = Node.builder(regionConfig.getPermToSet()).value(true).build();
                        Node nodePermFalse = Node.builder(regionConfig.getPermToSet()).value(false).build();
                        Node nodeSuffix = Node.builder("suffix.100." + regionConfig.getOwnerSuffix()).build();
                        user.data().remove(nodePermTrue);
                        user.data().add(nodePermFalse);
                        user.data().remove(nodeSuffix);
                        userManager.saveUser(user);
                    });
                }

            }
        });
    }

    public void setPermission(Map<String, ClanPlayer> clanPlayersMap, Region region, RegionConfig regionConfig) {
        clanPlayersMap.values().forEach(clanPlayer -> {
            Clan clan = clanPlayer.getClan();
            if (clan != null && clan.getName().equalsIgnoreCase(region.getOwner())) {
                if (luckPerms != null) {
                    UserManager userManager = luckPerms.getUserManager();
                    CompletableFuture<User> userFuture = userManager.loadUser(clanPlayer.getUUID());
                    userFuture.thenAcceptAsync(user -> {
                        Node nodePermTrue = Node.builder(regionConfig.getPermToSet()).value(true).build();
                        Node nodePermFalse = Node.builder(regionConfig.getPermToSet()).value(false).build();
                        Node nodeSuffix = Node.builder("suffix.100." + regionConfig.getOwnerSuffix()).build();
                        user.data().remove(nodePermFalse);
                        user.data().add(nodePermTrue);
                        user.data().add(nodeSuffix);
                        userManager.saveUser(user);
                    });
                }

            }
        });
    }

    private void notifyCapturingRegion(String regionAlias, Region region, long diff,
                                       int candidatePlayersNumber, long timeToCapture) {
        long timeLeft = timeToCapture - diff;
        if (diff <= timeToCapture && timeLeft > 0) {
            if (timeLeft % 60 == 0) {
                printCapturingRegionMsg(regionAlias, region, candidatePlayersNumber, timeLeft / 60, "мин");
            }
            if (timeLeft < 60 && timeLeft % 10 == 0) {
                printCapturingRegionMsg(regionAlias, region, candidatePlayersNumber, timeLeft, "сек");
            }
            if (timeLeft < 10) {
                printCapturingRegionMsg(regionAlias, region, candidatePlayersNumber, timeLeft, "сек");
            }
        }
    }

    private void printCapturingRegionMsg(String regionAlias, Region region, int candidatePlayersNumber, long timeLeftToCapture,
                                         String unit) {
        RegionConfig regionConfig = pluginConfig.getRegionConfigMap().get(region.getName());
        String prefix = regionConfig.getMsgPrefix();
        Bukkit.broadcastMessage(prefix + ChatColor.RED + " В регионе " + ChatColor.GREEN + regionAlias + ChatColor.AQUA + " "
                + candidatePlayersNumber + ChatColor.RED + " игроков клана "
                + ChatColor.GREEN + region.getCandidate() + ChatColor.RED + ". До захвата осталось продержаться "
                + ChatColor.AQUA + timeLeftToCapture + ChatColor.RED + " " + unit + ".");
    }

    private void notifyLoosingRegion(String regionAlias, Region region, long timeToLoose) {
        if (timeToLoose > 3600 && timeToLoose % 3600 == 0) {
            printLoosingRegionMsg(regionAlias, region, timeToLoose / 3600, "час");
        }
        if (timeToLoose > 60 && timeToLoose < 600 && timeToLoose % 60 == 0) {
            printLoosingRegionMsg(regionAlias, region, timeToLoose / 60, "мин");
        }
        if (timeToLoose < 60 && timeToLoose % 10 == 0 || timeToLoose < 10) {
            printLoosingRegionMsg(regionAlias, region, timeToLoose, "сек");
        }
    }

    private void printLoosingRegionMsg(String regionAlias, Region region, long timeToLoose, String unit) {
        RegionConfig regionConfig = pluginConfig.getRegionConfigMap().get(region.getName());
        String prefix = regionConfig.getMsgPrefix();
        Bukkit.broadcastMessage(prefix + ChatColor.RED + " Недостаточно игроков в регионе " + ChatColor.AQUA + regionAlias
                + ChatColor.RED + ". До потери региона кланом " + ChatColor.GREEN + region.getOwner() + ChatColor.RED
                + " осталось " + ChatColor.AQUA + timeToLoose + ChatColor.RED + " " + unit + ".");
    }

    private void payAllClanPlayers(Map<String, ClanPlayer> clanPlayersMap, Region region, RegionConfig regionConfig) {
        List<Integer> payoutsList = new ArrayList<>();
        String prefix = regionConfig.getMsgPrefix();
        getServer().getOnlinePlayers().forEach(player -> {
            if (clanPlayersMap.containsKey(player.getName())) {
                ClanPlayer clanPlayer = clanPlayersMap.get(player.getName());
                if (clanPlayer != null && clanPlayer.getClan() != null && clanPlayer.getClan().getName().equalsIgnoreCase(region.getOwner())) {
                    EconomyResponse r = economy.depositPlayer(player, regionConfig.getPlayerPayout());
                    if (r.transactionSuccess()) {
                        player.sendMessage(String.format(prefix + ChatColor.YELLOW + " Вы заработали " + ChatColor.GREEN + "%s%s"
                                        + ChatColor.YELLOW + " за владением регионом " + ChatColor.RED + "%s",
                                economy.format(r.amount),
                                economy.currencyNamePlural(), region.getAlias()));
                    }
                    clanPlayer.getClan().addMoney(regionConfig.getClanPayout());
                    payoutsList.add(regionConfig.getClanPayout());
                }
            }
        });
        int clanPayout = payoutsList.stream().mapToInt(Integer::intValue).sum();
        if (clanPayout > 0) {
            Bukkit.broadcastMessage(String.format(prefix + ChatColor.YELLOW + " Клан " + ChatColor.GREEN + "%s"
                            + ChatColor.YELLOW + " заработал " + ChatColor.GREEN + " %s%s"
                            + ChatColor.YELLOW + " за владением регионом " + ChatColor.RED + "%s",
                    region.getOwner(),
                    economy.format(clanPayout),
                    economy.currencyNamePlural(), region.getAlias()));
            region.setLastPayoutTime(LocalDateTime.now());
        }
    }

    private void loadRegions() {
        Map<String, Region> regionsMap = clanBattleData.getRegionsMap();
        RegionContainer container = WGBukkit.getPlugin().getRegionContainer();
        container.getLoaded().forEach(rm -> rm.getRegions().keySet().forEach(region -> {
            if (pluginConfig.getRegionConfigMap().containsKey(region)) {
                String regionName = region.toLowerCase();
                String regionAlias = pluginConfig.getRegionConfigMap().get(regionName).getAlias();
                regionsMap.putIfAbsent(regionName, new Region(regionName, regionAlias));
            }
        }));

    }

    public Map<String, Region> getRegionsMap() {
        return clanBattleData.getRegionsMap();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }
    }

    public static Economy getEconomy() {
        return economy;
    }

    void loadConfig() {
        pluginConfig.getRegionConfigMap().clear();
        ConfigurationSection configSection = this.getConfig().getConfigurationSection("regions");
        configSection.getKeys(false).forEach(key -> {
            ConfigurationSection configurationSection = configSection.getConfigurationSection(key);
            int timeToCapture = configurationSection.getInt("time_to_capture");
            int timeToKeep = configurationSection.getInt("time_to_keep");
            int minClanPlayers = configurationSection.getInt("min_clan_players");
            int playerPayout = configurationSection.getInt("player_payout");
            int clanPayout = configurationSection.getInt("clan_payout_per_player");
            int period = configurationSection.getInt("period");
            String region = configurationSection.getString("region");
            String regionAlias = configurationSection.getString("name");
            String permToSet = configurationSection.getString("perm_to_set");
            String messagePrefix = configurationSection.getString("msg_prefix");
            String ownerSuffix = configurationSection.getString("owner_suffix");
            pluginConfig.addRegionConfig(new RegionConfig(timeToCapture, timeToKeep, minClanPlayers, permToSet, playerPayout, clanPayout,
                    period, region, regionAlias, messagePrefix, ownerSuffix));
        });
    }

    private void registerCommands() {
        Bukkit.getPluginCommand("cb").setExecutor(new ClanBattleCommand(this));
        Bukkit.getPluginCommand("clanbattle").setExecutor(new ClanBattleCommand(this));
    }

    public ClanBattleData getClanBattleData() {
        return clanBattleData;
    }

}
