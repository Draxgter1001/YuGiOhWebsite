package taf.yugioh.scanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taf.yugioh.scanner.dto.*;
import taf.yugioh.scanner.entity.*;
import taf.yugioh.scanner.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeckService {

    // Yu-Gi-Oh Deck Rules Constants
    private static final int MAIN_DECK_MIN = 40;
    private static final int MAIN_DECK_MAX = 60;
    private static final int EXTRA_DECK_MAX = 15;
    private static final int SIDE_DECK_MAX = 15;
    private static final int MAX_COPIES_PER_CARD = 3;

    // Extra Deck card types (frameType values)
    private static final Set<String> EXTRA_DECK_TYPES = Set.of(
            "fusion",
            "synchro",
            "xyz",
            "link",
            "synchro_pendulum",
            "xyz_pendulum",
            "fusion_pendulum"
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Autowired
    private UserDeckRepository userDeckRepository;

    @Autowired
    private DeckCardRepository deckCardRepository;

    @Autowired
    private CardRepository cardRepository;

    /**
     * Create a new deck for user
     */
    public ApiResponse<DeckResponse> createDeck(User user, DeckRequest request) {
        UserDeck deck = new UserDeck();
        deck.setUser(user);
        deck.setName(request.getName().trim());
        deck.setDescription(request.getDescription());
        deck.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);

        UserDeck savedDeck = userDeckRepository.save(deck);

        DeckResponse response = buildDeckResponse(savedDeck, false);
        return ApiResponse.success("Deck created successfully", response);
    }

    /**
     * Get all decks for user
     */
    public ApiResponse<List<DeckResponse>> getUserDecks(User user) {
        List<UserDeck> decks = userDeckRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<DeckResponse> responses = decks.stream()
                .map(deck -> buildDeckResponse(deck, false))
                .collect(Collectors.toList());

        return ApiResponse.success("Decks retrieved successfully", responses);
    }

    /**
     * Get a specific deck by ID
     */
    public ApiResponse<DeckResponse> getDeck(User user, Long deckId, boolean includeCards) {
        Optional<UserDeck> deckOptional = userDeckRepository.findByIdAndUserId(deckId, user.getId());

        if (deckOptional.isEmpty()) {
            return ApiResponse.error("Deck not found");
        }

        DeckResponse response = buildDeckResponse(deckOptional.get(), includeCards);
        return ApiResponse.success("Deck retrieved successfully", response);
    }

    /**
     * Get a public deck by ID (no auth required)
     */
    public ApiResponse<DeckResponse> getPublicDeck(Long deckId) {
        Optional<UserDeck> deckOptional = userDeckRepository.findById(deckId);

        if (deckOptional.isEmpty()) {
            return ApiResponse.error("Deck not found");
        }

        UserDeck deck = deckOptional.get();
        if (!deck.getIsPublic()) {
            return ApiResponse.error("Deck is not public");
        }

        DeckResponse response = buildDeckResponse(deck, true);
        return ApiResponse.success("Deck retrieved successfully", response);
    }

    /**
     * Update deck info (name, description, visibility)
     */
    public ApiResponse<DeckResponse> updateDeck(User user, Long deckId, DeckRequest request) {
        Optional<UserDeck> deckOptional = userDeckRepository.findByIdAndUserId(deckId, user.getId());

        if (deckOptional.isEmpty()) {
            return ApiResponse.error("Deck not found");
        }

        UserDeck deck = deckOptional.get();
        deck.setName(request.getName().trim());
        deck.setDescription(request.getDescription());
        if (request.getIsPublic() != null) {
            deck.setIsPublic(request.getIsPublic());
        }

        UserDeck savedDeck = userDeckRepository.save(deck);
        DeckResponse response = buildDeckResponse(savedDeck, false);
        return ApiResponse.success("Deck updated successfully", response);
    }

    /**
     * Delete a deck
     */
    public ApiResponse<Void> deleteDeck(User user, Long deckId) {
        Optional<UserDeck> deckOptional = userDeckRepository.findByIdAndUserId(deckId, user.getId());

        if (deckOptional.isEmpty()) {
            return ApiResponse.error("Deck not found");
        }

        userDeckRepository.delete(deckOptional.get());
        return ApiResponse.success("Deck deleted successfully");
    }

    /**
     * Add a card to deck with validation
     */
    public ApiResponse<DeckResponse> addCardToDeck(User user, Long deckId, AddCardRequest request) {
        // Get deck
        Optional<UserDeck> deckOptional = userDeckRepository.findByIdAndUserId(deckId, user.getId());
        if (deckOptional.isEmpty()) {
            return ApiResponse.error("Deck not found");
        }
        UserDeck deck = deckOptional.get();

        // Verify card exists in database
        Optional<Card> cardOptional = cardRepository.findByCardId(request.getCardId());
        if (cardOptional.isEmpty()) {
            return ApiResponse.error("Card not found in database. Please search for the card first.");
        }
        Card card = cardOptional.get();

        // Determine correct deck type based on card frameType
        DeckCard.DeckType targetDeckType = request.getDeckType();
        if (targetDeckType == DeckCard.DeckType.MAIN || targetDeckType == DeckCard.DeckType.EXTRA) {
            // Auto-correct deck type based on card type
            if (isExtraDeckCard(card.getFrameType())) {
                targetDeckType = DeckCard.DeckType.EXTRA;
            } else if (targetDeckType == DeckCard.DeckType.EXTRA) {
                // Card is not an Extra Deck card but user tried to add to Extra Deck
                return ApiResponse.error("This card cannot be added to the Extra Deck. Only Fusion, Synchro, XYZ, and Link monsters can be in the Extra Deck.");
            }
        }

        // Validate total copies of this card across all deck sections
        int totalCopies = getTotalCardCopies(deckId, request.getCardId());
        int newTotal = totalCopies + request.getQuantity();
        if (newTotal > MAX_COPIES_PER_CARD) {
            return ApiResponse.error("Cannot add more copies. Maximum " + MAX_COPIES_PER_CARD +
                    " copies of any card allowed. You already have " + totalCopies + " copies.");
        }

        // Validate deck size limits
        String sizeError = validateDeckSizeForAddition(deckId, targetDeckType, request.getQuantity());
        if (sizeError != null) {
            return ApiResponse.error(sizeError);
        }

        // Check if card already exists in this deck section
        Optional<DeckCard> existingCard = deckCardRepository.findByDeckIdAndCardIdAndDeckType(
                deckId, request.getCardId(), targetDeckType
        );

        if (existingCard.isPresent()) {
            // Update quantity
            DeckCard deckCard = existingCard.get();
            int newQuantity = deckCard.getQuantity() + request.getQuantity();
            if (newQuantity > MAX_COPIES_PER_CARD) {
                return ApiResponse.error("Cannot exceed " + MAX_COPIES_PER_CARD + " copies of this card");
            }
            deckCard.setQuantity(newQuantity);
            deckCardRepository.save(deckCard);
        } else {
            // Add new card entry
            DeckCard deckCard = new DeckCard(deck, request.getCardId(), request.getQuantity(), targetDeckType);
            deckCardRepository.save(deckCard);
        }

        // Return updated deck
        DeckResponse response = buildDeckResponse(deck, true);
        return ApiResponse.success("Card added to deck", response);
    }

    /**
     * Remove a card from deck
     */
    public ApiResponse<DeckResponse> removeCardFromDeck(User user, Long deckId, Long cardId,
                                                        DeckCard.DeckType deckType, Integer quantity) {
        // Get deck
        Optional<UserDeck> deckOptional = userDeckRepository.findByIdAndUserId(deckId, user.getId());
        if (deckOptional.isEmpty()) {
            return ApiResponse.error("Deck not found");
        }
        UserDeck deck = deckOptional.get();

        // Find card in deck
        Optional<DeckCard> deckCardOptional = deckCardRepository.findByDeckIdAndCardIdAndDeckType(
                deckId, cardId, deckType
        );

        if (deckCardOptional.isEmpty()) {
            return ApiResponse.error("Card not found in this deck section");
        }

        DeckCard deckCard = deckCardOptional.get();

        if (quantity == null || quantity >= deckCard.getQuantity()) {
            // Remove entirely
            deckCardRepository.delete(deckCard);
        } else {
            // Reduce quantity
            deckCard.setQuantity(deckCard.getQuantity() - quantity);
            deckCardRepository.save(deckCard);
        }

        DeckResponse response = buildDeckResponse(deck, true);
        return ApiResponse.success("Card removed from deck", response);
    }

    /**
     * Validate entire deck against Yu-Gi-Oh rules
     */
    public ApiResponse<DeckResponse> validateDeck(User user, Long deckId) {
        Optional<UserDeck> deckOptional = userDeckRepository.findByIdAndUserId(deckId, user.getId());
        if (deckOptional.isEmpty()) {
            return ApiResponse.error("Deck not found");
        }

        DeckResponse response = buildDeckResponse(deckOptional.get(), true);
        return ApiResponse.success("Deck validation complete", response);
    }

    /**
     * Get public decks for browsing
     */
    public ApiResponse<List<DeckResponse>> getPublicDecks() {
        List<UserDeck> decks = userDeckRepository.findByIsPublicTrueOrderByCreatedAtDesc();

        List<DeckResponse> responses = decks.stream()
                .map(deck -> buildDeckResponse(deck, false))
                .collect(Collectors.toList());

        return ApiResponse.success("Public decks retrieved", responses);
    }

    // ==================== Helper Methods ====================

    /**
     * Build complete deck response with validation and prices
     */
    private DeckResponse buildDeckResponse(UserDeck deck, boolean includeCards) {
        DeckResponse response = DeckResponse.fromEntity(deck);

        // Get all cards in deck
        List<DeckCard> deckCards = deckCardRepository.findByDeckIdWithCards(deck.getId());

        // Separate by deck type
        List<DeckCard> mainDeckCards = new ArrayList<>();
        List<DeckCard> extraDeckCards = new ArrayList<>();
        List<DeckCard> sideDeckCards = new ArrayList<>();

        int mainCount = 0;
        int extraCount = 0;
        int sideCount = 0;

        // Price totals
        double cardmarketTotal = 0.0;
        double tcgplayerTotal = 0.0;
        double coolstuffincTotal = 0.0;

        for (DeckCard dc : deckCards) {
            switch (dc.getDeckType()) {
                case MAIN:
                    mainDeckCards.add(dc);
                    mainCount += dc.getQuantity();
                    break;
                case EXTRA:
                    extraDeckCards.add(dc);
                    extraCount += dc.getQuantity();
                    break;
                case SIDE:
                    sideDeckCards.add(dc);
                    sideCount += dc.getQuantity();
                    break;
            }

            // Calculate prices
            Card card = dc.getCard();
            if (card != null && card.getCardPrices() != null) {
                CardPrices prices = parsePricesFromJson(card.getCardPrices());
                if (prices != null) {
                    int qty = dc.getQuantity();
                    cardmarketTotal += prices.getCardmarketPriceValue() * qty;
                    tcgplayerTotal += prices.getTcgplayerPriceValue() * qty;
                    coolstuffincTotal += prices.getCoolstuffincPriceValue() * qty;
                }
            }
        }

        response.setMainDeckCount(mainCount);
        response.setExtraDeckCount(extraCount);
        response.setSideDeckCount(sideCount);
        response.setTotalCards(mainCount + extraCount + sideCount);

        // Set total prices
        DeckResponse.DeckPrices totalPrices = new DeckResponse.DeckPrices(
                Math.round(cardmarketTotal * 100.0) / 100.0,
                Math.round(tcgplayerTotal * 100.0) / 100.0,
                Math.round(coolstuffincTotal * 100.0) / 100.0
        );
        response.setTotalPrices(totalPrices);

        // Validate deck
        List<String> errors = validateDeckRules(mainCount, extraCount, sideCount, deckCards);
        response.setValid(errors.isEmpty());
        response.setValidationErrors(errors);

        // Include card details if requested
        if (includeCards) {
            response.setMainDeck(mainDeckCards.stream()
                    .map(dc -> DeckCardDTO.fromEntity(dc, backendUrl))
                    .collect(Collectors.toList()));
            response.setExtraDeck(extraDeckCards.stream()
                    .map(dc -> DeckCardDTO.fromEntity(dc, backendUrl))
                    .collect(Collectors.toList()));
            response.setSideDeck(sideDeckCards.stream()
                    .map(dc -> DeckCardDTO.fromEntity(dc, backendUrl))
                    .collect(Collectors.toList()));
        }

        return response;
    }

    /**
     * Parse CardPrices from stored JSON string
     */
    private CardPrices parsePricesFromJson(String jsonString) {
        try {
            JsonNode priceNode = objectMapper.readTree(jsonString);
            CardPrices prices = new CardPrices();

            if (priceNode.has("cardmarket_price") && !priceNode.get("cardmarket_price").isNull()) {
                prices.setCardmarketPrice(priceNode.get("cardmarket_price").asText());
            }
            if (priceNode.has("tcgplayer_price") && !priceNode.get("tcgplayer_price").isNull()) {
                prices.setTcgplayerPrice(priceNode.get("tcgplayer_price").asText());
            }
            if (priceNode.has("ebay_price") && !priceNode.get("ebay_price").isNull()) {
                prices.setEbayPrice(priceNode.get("ebay_price").asText());
            }
            if (priceNode.has("amazon_price") && !priceNode.get("amazon_price").isNull()) {
                prices.setAmazonPrice(priceNode.get("amazon_price").asText());
            }
            if (priceNode.has("coolstuffinc_price") && !priceNode.get("coolstuffinc_price").isNull()) {
                prices.setCoolstuffincPrice(priceNode.get("coolstuffinc_price").asText());
            }

            return prices;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate deck against Yu-Gi-Oh rules
     */
    private List<String> validateDeckRules(int mainCount, int extraCount, int sideCount,
                                           List<DeckCard> deckCards) {
        List<String> errors = new ArrayList<>();

        // Main Deck size validation
        if (mainCount < MAIN_DECK_MIN) {
            errors.add("Main Deck must have at least " + MAIN_DECK_MIN + " cards (currently " + mainCount + ")");
        }
        if (mainCount > MAIN_DECK_MAX) {
            errors.add("Main Deck cannot exceed " + MAIN_DECK_MAX + " cards (currently " + mainCount + ")");
        }

        // Extra Deck size validation
        if (extraCount > EXTRA_DECK_MAX) {
            errors.add("Extra Deck cannot exceed " + EXTRA_DECK_MAX + " cards (currently " + extraCount + ")");
        }

        // Side Deck size validation
        if (sideCount > SIDE_DECK_MAX) {
            errors.add("Side Deck cannot exceed " + SIDE_DECK_MAX + " cards (currently " + sideCount + ")");
        }

        // Check card copy limits (max 3 of same card across all sections)
        Map<Long, Integer> cardCounts = new HashMap<>();
        for (DeckCard dc : deckCards) {
            cardCounts.merge(dc.getCardId(), dc.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : cardCounts.entrySet()) {
            if (entry.getValue() > MAX_COPIES_PER_CARD) {
                // Get card name if possible
                String cardName = "Card ID " + entry.getKey();
                Optional<Card> card = cardRepository.findByCardId(entry.getKey());
                if (card.isPresent()) {
                    cardName = card.get().getName();
                }
                errors.add("Too many copies of '" + cardName + "' (" + entry.getValue() + "/" + MAX_COPIES_PER_CARD + ")");
            }
        }

        // Validate card placement (Extra Deck cards in Extra Deck, etc.)
        for (DeckCard dc : deckCards) {
            Card card = dc.getCard();
            if (card != null) {
                boolean isExtraDeckCard = isExtraDeckCard(card.getFrameType());

                if (isExtraDeckCard && dc.getDeckType() == DeckCard.DeckType.MAIN) {
                    errors.add("'" + card.getName() + "' should be in Extra Deck, not Main Deck");
                }
                if (!isExtraDeckCard && dc.getDeckType() == DeckCard.DeckType.EXTRA) {
                    errors.add("'" + card.getName() + "' cannot be in Extra Deck");
                }
            }
        }

        return errors;
    }

    /**
     * Check if card type belongs in Extra Deck
     */
    private boolean isExtraDeckCard(String frameType) {
        if (frameType == null) return false;
        return EXTRA_DECK_TYPES.contains(frameType.toLowerCase());
    }

    /**
     * Get total copies of a card across all deck sections
     * Uses optimized database query instead of loading all cards
     */
    private int getTotalCardCopies(Long deckId, Long cardId) {
        Integer count = deckCardRepository.countTotalCardCopies(deckId, cardId);
        return count != null ? count : 0;
    }

    /**
     * Validate deck size before adding cards
     */
    private String validateDeckSizeForAddition(Long deckId, DeckCard.DeckType deckType, int quantity) {
        Integer currentCount = deckCardRepository.countCardsByDeckIdAndType(deckId, deckType);
        if (currentCount == null) currentCount = 0;

        int newCount = currentCount + quantity;

        switch (deckType) {
            case MAIN:
                if (newCount > MAIN_DECK_MAX) {
                    return "Cannot add cards. Main Deck would exceed " + MAIN_DECK_MAX + " cards (" + newCount + ")";
                }
                break;
            case EXTRA:
                if (newCount > EXTRA_DECK_MAX) {
                    return "Cannot add cards. Extra Deck would exceed " + EXTRA_DECK_MAX + " cards (" + newCount + ")";
                }
                break;
            case SIDE:
                if (newCount > SIDE_DECK_MAX) {
                    return "Cannot add cards. Side Deck would exceed " + SIDE_DECK_MAX + " cards (" + newCount + ")";
                }
                break;
        }

        return null; // No error
    }
}