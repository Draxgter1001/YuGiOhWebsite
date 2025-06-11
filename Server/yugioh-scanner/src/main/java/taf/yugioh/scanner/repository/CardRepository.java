package taf.yugioh.scanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import taf.yugioh.scanner.entity.Card;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    /**
     * Find card by card_id (the Yu-Gi-Oh card ID, not the database primary key)
     */
    Optional<Card> findByCardId(Long cardId);
    
    /**
     * Check if a card exists by card_id
     */
    boolean existsByCardId(Long cardId);
    
    /**
     * Find card by name (case-insensitive)
     */
    @Query("SELECT c FROM Card c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Card> findByNameIgnoreCase(@Param("name") String name);
    
    /**
     * Find cards by type
     */
    @Query("SELECT c FROM Card c WHERE c.type = :type")
    java.util.List<Card> findByType(@Param("type") String type);
    
    /**
     * Find cards by attribute
     */
    @Query("SELECT c FROM Card c WHERE c.attribute = :attribute")
    java.util.List<Card> findByAttribute(@Param("attribute") String attribute);
    
    /**
     * Find cards by race
     */
    @Query("SELECT c FROM Card c WHERE c.race = :race")
    java.util.List<Card> findByRace(@Param("race") String race);
    
    /**
     * Search cards by name containing (case-insensitive)
     */
    @Query("SELECT c FROM Card c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    java.util.List<Card> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // REMOVED: findByCardIdWithImage method that was causing the error
    // Since we removed the cardImage relationship from Card entity,
    // we'll query CardImage separately when needed
}