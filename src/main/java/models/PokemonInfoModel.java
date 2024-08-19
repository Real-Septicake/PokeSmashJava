package models;

public class PokemonInfoModel {

    @Col(name = "pokeId", type = Short.class)
    private Short pokeId;

    @Col(name = "smashCount", type = Integer.class)
    private Integer smashCount;

    @Col(name = "passCount", type = Integer.class)
    private Integer passCount;

    @Col(name = "smashVotes", type = Integer.class)
    private Integer smashVotes;

    @Col(name = "passVotes", type = Integer.class)
    private Integer passVotes;

    @Col(name = "place", type = Short.class)
    private Short place;

    public PokemonInfoModel() {}

    public PokemonInfoModel(Short pokeId, Integer smashCount, Integer passCount, Integer smashVotes, Integer passVotes, Short place) {
        this.pokeId = pokeId;
        this.smashCount = smashCount;
        this.passCount = passCount;
        this.smashVotes = smashVotes;
        this.passVotes = passVotes;
        this.place = place;
    }

    public Short getPokeId() {
        return pokeId;
    }

    public Integer getSmashCount() {
        return smashCount;
    }

    public Integer getPassCount() {
        return passCount;
    }

    public Integer getSmashVotes() {
        return smashVotes;
    }

    public Integer getPassVotes() {
        return passVotes;
    }

    public Short getPlace() {
        return place;
    }
}
