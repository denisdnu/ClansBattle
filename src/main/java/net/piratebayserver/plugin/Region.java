package net.piratebayserver.plugin;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Region implements Serializable {
    private static transient final long serialVersionUID = -1681012206529286320L;
    private final String name;
    private final String alias;
    private String candidate = null;
    private String owner = null;
    private String defeated = null;
    private final List<String> allClansWinners = new ArrayList<>();
    private final Map<String, Set<String>> clanPlayersMap = new ConcurrentHashMap<>();
    private LocalDateTime lastCandidateVisitTime = null;
    private LocalDateTime lastOwnerVisitTime = null;
    private LocalDateTime lastPayoutTime = null;

    public Region(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public REGION_STATUS getStatus() {
        if (this.owner != null) {
            return REGION_STATUS.CAPTURED;
        }
        if (candidate != null) {
            return REGION_STATUS.CAPTURING;
        }
        return REGION_STATUS.FREE;
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public String getCandidate() {
        return candidate;
    }

    public String getOwner() {
        return owner;
    }

    public String getDefeated() {
        return defeated;
    }

    public List<String> getAllClansWinners() {
        return allClansWinners;
    }

    public Map<String, Set<String>> getClanPlayersMap() {
        return clanPlayersMap;
    }

    public LocalDateTime getLastOwnerVisitTime() {
        return lastOwnerVisitTime;
    }

    public void setLastOwnerVisitTime(LocalDateTime lastOwnerVisitTime) {
        this.lastOwnerVisitTime = lastOwnerVisitTime;
    }

    public LocalDateTime getLastCandidateVisitTime() {
        return lastCandidateVisitTime;
    }

    public LocalDateTime getLastPayoutTime() {
        return lastPayoutTime;
    }

    public void setLastPayoutTime(LocalDateTime lastPayoutTime) {
        this.lastPayoutTime = lastPayoutTime;
    }

    public void setLastCandidateVisitTime(LocalDateTime lastCandidateVisitTime) {
        this.lastCandidateVisitTime = lastCandidateVisitTime;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
        this.lastCandidateVisitTime = LocalDateTime.now();
    }

    public void removeCandidate() {
        this.candidate = null;
        this.lastCandidateVisitTime = null;
    }

    public void setCaptured() {
        this.lastOwnerVisitTime = LocalDateTime.now();
        this.defeated = this.owner;
        this.owner = this.candidate;
        this.candidate = null;
        this.lastCandidateVisitTime = null;
        this.allClansWinners.add(this.owner);
        this.lastPayoutTime = null;
    }

    public void setFree() {
        this.lastOwnerVisitTime = null;
        this.defeated = this.owner;
        this.owner = null;
    }
}
