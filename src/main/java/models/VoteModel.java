package models;

public class VoteModel {

    @Col(name = "guildId", type = String.class)
    private String guildId;

    @Col(name = "pokeId", type = Short.class)
    private Short pokeId;

    @Col(name = "smashes", type = Integer.class)
    private Integer smashes;

    @Col(name = "passes", type = Integer.class)
    private Integer passes;

    @Col(name = "result", type = Boolean.class)
    private Boolean result;

    public VoteModel() {}

    public VoteModel(String guildId, Short pokeId, Integer smashes, Integer passes, Boolean result) {
        this.guildId = guildId;
        this.pokeId = pokeId;
        this.smashes = smashes;
        this.passes = passes;
        this.result = result;
    }

    public String getGuildId() {
        return guildId;
    }

    public Short getPokeId() {
        return pokeId;
    }

    public Integer getSmashes() {
        return smashes;
    }

    public Integer getPasses() {
        return passes;
    }

    public Boolean getResult() {
        return result;
    }
}
