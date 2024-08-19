package models;

public class WhitelistModel {

    @Col(name = "userId", type = String.class)
    private String userId;

    @Col(name = "guildId", type = String.class)
    private String guildId;

    public WhitelistModel() {}

    public WhitelistModel(String userId, String guildId) {
        this.userId = userId;
        this.guildId = guildId;
    }

    public String getUserId() {
        return userId;
    }

    public String getGuildId() {
        return guildId;
    }
}
