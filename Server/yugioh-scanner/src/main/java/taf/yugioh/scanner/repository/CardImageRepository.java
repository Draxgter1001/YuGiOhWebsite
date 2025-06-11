package taf.yugioh.scanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import taf.yugioh.scanner.entity.CardImage;

import java.util.Optional;

@Repository
public interface CardImageRepository extends JpaRepository<CardImage, Long> {
    
    /**
     * Find card image by card_id (the Yu-Gi-Oh card ID)
     */
    Optional<CardImage> findByCardId(Long cardId);
    
    /**
     * Check if a card image exists by card_id
     */
    boolean existsByCardId(Long cardId);
    
    /**
     * Delete card image by card_id
     */
    void deleteByCardId(Long cardId);
    
    /**
     * Get only the image data for a specific card (for serving images)
     */
    @Query("SELECT ci FROM CardImage ci WHERE ci.cardId = :cardId")
    Optional<CardImage> findImageDataByCardId(@Param("cardId") Long cardId);
    
    /**
     * Get only the small image data for a specific card
     */
    @Query("SELECT ci.imageSmallData FROM CardImage ci WHERE ci.cardId = :cardId")
    Optional<byte[]> findSmallImageDataByCardId(@Param("cardId") Long cardId);
    
    /**
     * Get image statistics - count all images
     */
    @Query("SELECT COUNT(ci) FROM CardImage ci")
    long countAllImages();
    
    /**
     * Find images by content type
     */
    @Query("SELECT ci FROM CardImage ci WHERE ci.contentType = :contentType")
    java.util.List<CardImage> findByContentType(@Param("contentType") String contentType);
    
    /**
     * Find images larger than a specific size
     */
    @Query("SELECT ci FROM CardImage ci WHERE ci.fileSize > :size")
    java.util.List<CardImage> findByFileSizeGreaterThan(@Param("size") Integer size);
    
    /**
     * Find all card IDs that have images
     */
    @Query("SELECT ci.cardId FROM CardImage ci")
    java.util.List<Long> findAllCardIdsWithImages();
}