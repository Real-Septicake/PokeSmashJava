package models;

public class ServerInfoModel {

    @Col(name = "guildId", type = String.class)
    private String guildId;

    @Col(name = "pollCount", type = Byte.class)
    private Byte pollCount;

    @Col(name = "offset", type = Short.class)
    private Short offset;

    @Col(name = "smash", type = Short.class)
    private Short smashes;

    @Col(name = "pass", type = Short.class)
    private Short passes;

    @Col(name = "channel", type = String.class)
    private String channel;

    @Col(name = "name", type = String.class)
    private String name;

    public ServerInfoModel() {}

    public ServerInfoModel(String guildId, Byte pollCount, Short offset, Short smashes, Short passes, String channel, String name) {
        this.guildId = guildId;
        this.pollCount = pollCount;
        this.offset = offset;
        this.smashes = smashes;
        this.passes = passes;
        this.channel = channel;
        this.name = name;
    }

    public String getGuildId() {
        return guildId;
    }

    public Byte getPollCount() {
        return pollCount;
    }

    public Short getOffset() {
        return offset;
    }

    public Short getSmashes() {
        return smashes;
    }

    public Short getPasses() {
        return passes;
    }

    public String getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }
}
