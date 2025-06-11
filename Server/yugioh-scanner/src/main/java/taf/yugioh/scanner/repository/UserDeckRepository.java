package taf.yugioh.scanner.repository;

import taf.yugioh.scanner.entity.UserDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeckRepository extends JpaRepository<UserDeck, Long> {
    
    List<UserDeck> findByUserId(Long userId);
    
    List<UserDeck> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<UserDeck> findByIsPublicTrueOrderByCreatedAtDesc();
    
    Optional<UserDeck> findByIdAndUserId(Long deckId, Long userId);
    
    @Query("SELECT COUNT(dc) FROM DeckCard dc WHERE dc.deck.id = :deckId")
    Integer countCardsByDeckId(@Param("deckId") Long deckId);
    
    @Query("SELECT d FROM UserDeck d WHERE d.name ILIKE %:searchTerm% AND d.isPublic = true")
    List<UserDeck> searchPublicDecks(@Param("searchTerm") String searchTerm);
}
