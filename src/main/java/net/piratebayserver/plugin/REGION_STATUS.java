package net.piratebayserver.plugin;

public enum REGION_STATUS {
    FREE("СВОБОДЕН"), CAPTURING("ПОД АТАКОЙ"), CAPTURED("ЗАХВАЧЕН");
    private final String name;

    REGION_STATUS(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
