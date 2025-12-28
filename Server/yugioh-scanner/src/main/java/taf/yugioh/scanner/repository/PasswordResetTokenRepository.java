package taf.yugioh.scanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import taf.yugioh.scanner.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Find token by token string
    Optional<PasswordResetToken> findByToken(String token);

    // Find valid (not used, not expired) token by token string
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.used = false AND t.expiryDate > :now")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    // Find the latest token for an email
    Optional<PasswordResetToken> findFirstByEmailOrderByCreatedAtDesc(String email);

    // Delete expired tokens (cleanup)
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    // Delete all tokens for an email (when password is reset)
    @Modifying
    void deleteByEmail(String email);

    // Check if a valid token exists for email
    @Query("SELECT COUNT(t) > 0 FROM PasswordResetToken t WHERE t.email = :email AND t.used = false AND t.expiryDate > :now")
    boolean hasValidToken(@Param("email") String email, @Param("now") LocalDateTime now);
}