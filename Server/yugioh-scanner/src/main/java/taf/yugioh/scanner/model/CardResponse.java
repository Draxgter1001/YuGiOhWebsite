package taf.yugioh.scanner.model;

public class CardResponse {
    private Long id;
    private String name;
    private String type;
    private String frameType;
    private String desc;
    private Integer atk;
    private Integer def;
    private Integer level;
    private String race;
    private String attribute;
    private String imageUrl;
    private String imageUrlSmall;
    private Object[] cardSets;
    private Object cardPrices;

    // Constructors
    public CardResponse() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFrameType() { return frameType; }
    public void setFrameType(String frameType) { this.frameType = frameType; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public Integer getAtk() { return atk; }
    public void setAtk(Integer atk) { this.atk = atk; }

    public Integer getDef() { return def; }
    public void setDef(Integer def) { this.def = def; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageUrlSmall() { return imageUrlSmall; }
    public void setImageUrlSmall(String imageUrlSmall) { this.imageUrlSmall = imageUrlSmall; }

    public Object[] getCardSets() { return cardSets; }
    public void setCardSets(Object[] cardSets) { this.cardSets = cardSets; }

    public Object getCardPrices() { return cardPrices; }
    public void setCardPrices(Object cardPrices) { this.cardPrices = cardPrices; }
}
