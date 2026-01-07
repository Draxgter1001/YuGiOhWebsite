package taf.yugioh.scanner.repository;

import taf.yugioh.scanner.entity.DeckCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeckCardRepository extends JpaRepository<DeckCard, Long> {

    List<DeckCard> findByDeckId(Long deckId);

    List<DeckCard> findByDeckIdAndDeckType(Long deckId, DeckCard.DeckType deckType);

    Optional<DeckCard> findByDeckIdAndCardIdAndDeckType(Long deckId, Long cardId, DeckCard.DeckType deckType);

    @Query("SELECT SUM(dc.quantity) FROM DeckCard dc WHERE dc.deck.id = :deckId AND dc.deckType = :deckType")
    Integer countCardsByDeckIdAndType(@Param("deckId") Long deckId, @Param("deckType") DeckCard.DeckType deckType);

    @Query("SELECT dc FROM DeckCard dc JOIN FETCH dc.card WHERE dc.deck.id = :deckId")
    List<DeckCard> findByDeckIdWithCards(@Param("deckId") Long deckId);

    void deleteByDeckIdAndCardIdAndDeckType(Long deckId, Long cardId, DeckCard.DeckType deckType);

    /**
     * Optimized query to count total copies of a specific card in a deck
     * Replaces the Java-side filtering in DeckService.getTotalCardCopies()
     */
    @Query("SELECT COALESCE(SUM(dc.quantity), 0) FROM DeckCard dc WHERE dc.deck.id = :deckId AND dc.cardId = :cardId")
    Integer countTotalCardCopies(@Param("deckId") Long deckId, @Param("cardId") Long cardId);
}