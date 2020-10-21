package net.piratebayserver.plugin;

public enum ClanBattleCommandsEnum {
    BUY("buy"),
    PUT("put"),
    PREVIEW("preview"),
    LIST("list"),
    PURCHASED("purchased"),
    HELP("help");

    private String name;

    ClanBattleCommandsEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
