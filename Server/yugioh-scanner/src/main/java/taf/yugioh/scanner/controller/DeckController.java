package taf.yugioh.scanner.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import taf.yugioh.scanner.dto.*;
import taf.yugioh.scanner.entity.DeckCard;
import taf.yugioh.scanner.entity.User;
import taf.yugioh.scanner.service.DeckService;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    @Autowired
    private DeckService deckService;

    // ==================== Deck CRUD Operations ====================

    /**
     * Create a new deck
     * POST /api/decks
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DeckResponse>> createDeck(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DeckRequest request) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        ApiResponse<DeckResponse> response = deckService.createDeck(user, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all decks for current user
     * GET /api/decks
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeckResponse>>> getUserDecks(
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        ApiResponse<List<DeckResponse>> response = deckService.getUserDecks(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific deck by ID
     * GET /api/decks/{deckId}
     */
    @GetMapping("/{deckId}")
    public ResponseEntity<ApiResponse<DeckResponse>> getDeck(
            @AuthenticationPrincipal User user,
            @PathVariable Long deckId,
            @RequestParam(defaultValue = "true") boolean includeCards) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        ApiResponse<DeckResponse> response = deckService.getDeck(user, deckId, includeCards);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * Update deck info (name, description, visibility)
     * PUT /api/decks/{deckId}
     */
    @PutMapping("/{deckId}")
    public ResponseEntity<ApiResponse<DeckResponse>> updateDeck(
            @AuthenticationPrincipal User user,
            @PathVariable Long deckId,
            @Valid @RequestBody DeckRequest request) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        ApiResponse<DeckResponse> response = deckService.updateDeck(user, deckId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * Delete a deck
     * DELETE /api/decks/{deckId}
     */
    @DeleteMapping("/{deckId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeck(
            @AuthenticationPrincipal User user,
            @PathVariable Long deckId) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        ApiResponse<Void> response = deckService.deleteDeck(user, deckId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(response);
        }
    }

    // ==================== Card Management ====================

    /**
     * Add a card to deck
     * POST /api/decks/{deckId}/cards
     */
    @PostMapping("/{deckId}/cards")
    public ResponseEntity<ApiResponse<DeckResponse>> addCardToDeck(
            @AuthenticationPrincipal User user,
            @PathVariable Long deckId,
            @Valid @RequestBody AddCardRequest request) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        ApiResponse<DeckResponse> response = deckService.addCardToDeck(user, deckId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove a card from deck
     * DELETE /api/decks/{deckId}/cards/{cardId}
     */
    @DeleteMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<ApiResponse<DeckResponse>> removeCardFromDeck(
            @AuthenticationPrincipal User user,
            @PathVariable Long deckId,
            @PathVariable Long cardId,
            @RequestParam(required = false) DeckCard.DeckType deckType,
            @RequestParam(required = false) Integer quantity) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        // Default to MAIN if not specified
        if (deckType == null) {
            deckType = DeckCard.DeckType.MAIN;
        }

        ApiResponse<DeckResponse> response = deckService.removeCardFromDeck(
                user, deckId, cardId, deckType, quantity
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Validation ====================

    /**
     * Validate deck against Yu-Gi-Oh rules
     * GET /api/decks/{deckId}/validate
     */
    @GetMapping("/{deckId}/validate")
    public ResponseEntity<ApiResponse<DeckResponse>> validateDeck(
            @AuthenticationPrincipal User user,
            @PathVariable Long deckId) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        ApiResponse<DeckResponse> response = deckService.validateDeck(user, deckId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(response);
        }
    }

    // ==================== Public Decks ====================

    /**
     * Get all public decks (no auth required)
     * GET /api/decks/public
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<DeckResponse>>> getPublicDecks() {
        ApiResponse<List<DeckResponse>> response = deckService.getPublicDecks();
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific public deck (no auth required)
     * GET /api/decks/public/{deckId}
     */
    @GetMapping("/public/{deckId}")
    public ResponseEntity<ApiResponse<DeckResponse>> getPublicDeck(@PathVariable Long deckId) {
        ApiResponse<DeckResponse> response = deckService.getPublicDeck(deckId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(response);
        }
    }
}