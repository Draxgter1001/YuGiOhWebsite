package taf.yugioh.scanner.dto;

/**
 * DTO for card price information from various marketplaces
 */
public class CardPrices {

    private String cardmarketPrice;
    private String tcgplayerPrice;
    private String ebayPrice;
    private String amazonPrice;
    private String coolstuffincPrice;

    // Marketplace URLs for linking
    public static final String CARDMARKET_BASE_URL = "https://www.cardmarket.com/en/YuGiOh/Products/Search?searchString=";
    public static final String TCGPLAYER_BASE_URL = "https://www.tcgplayer.com/search/yugioh/product?productLineName=yugioh&q=";
    public static final String COOLSTUFFINC_BASE_URL = "https://www.coolstuffinc.com/main_search.php?pa=searchOnName&page=1&resultsPerPage=25&q=";

    // Constructors
    public CardPrices() {}

    public CardPrices(String cardmarketPrice, String tcgplayerPrice, String ebayPrice,
                      String amazonPrice, String coolstuffincPrice) {
        this.cardmarketPrice = cardmarketPrice;
        this.tcgplayerPrice = tcgplayerPrice;
        this.ebayPrice = ebayPrice;
        this.amazonPrice = amazonPrice;
        this.coolstuffincPrice = coolstuffincPrice;
    }

    // Getters and Setters
    public String getCardmarketPrice() { return cardmarketPrice; }
    public void setCardmarketPrice(String cardmarketPrice) { this.cardmarketPrice = cardmarketPrice; }

    public String getTcgplayerPrice() { return tcgplayerPrice; }
    public void setTcgplayerPrice(String tcgplayerPrice) { this.tcgplayerPrice = tcgplayerPrice; }

    public String getEbayPrice() { return ebayPrice; }
    public void setEbayPrice(String ebayPrice) { this.ebayPrice = ebayPrice; }

    public String getAmazonPrice() { return amazonPrice; }
    public void setAmazonPrice(String amazonPrice) { this.amazonPrice = amazonPrice; }

    public String getCoolstuffincPrice() { return coolstuffincPrice; }
    public void setCoolstuffincPrice(String coolstuffincPrice) { this.coolstuffincPrice = coolstuffincPrice; }

    // Helper methods to get numeric values for calculations
    public double getCardmarketPriceValue() {
        return parsePrice(cardmarketPrice);
    }

    public double getTcgplayerPriceValue() {
        return parsePrice(tcgplayerPrice);
    }

    public double getCoolstuffincPriceValue() {
        return parsePrice(coolstuffincPrice);
    }

    private double parsePrice(String price) {
        if (price == null || price.isEmpty() || price.equals("0")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Check if any price is available
    public boolean hasPrices() {
        return (cardmarketPrice != null && !cardmarketPrice.equals("0")) ||
                (tcgplayerPrice != null && !tcgplayerPrice.equals("0")) ||
                (coolstuffincPrice != null && !coolstuffincPrice.equals("0"));
    }
}