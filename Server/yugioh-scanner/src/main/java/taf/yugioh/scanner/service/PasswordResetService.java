package taf.yugioh.scanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taf.yugioh.scanner.dto.ApiResponse;
import taf.yugioh.scanner.entity.PasswordResetToken;
import taf.yugioh.scanner.entity.User;
import taf.yugioh.scanner.repository.PasswordResetTokenRepository;
import taf.yugioh.scanner.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    /**
     * Request password reset - generates token and sends email
     */
    public ApiResponse<Void> requestPasswordReset(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        // Always return success to prevent email enumeration attacks
        if (userOptional.isEmpty()) {
            return ApiResponse.success("If an account exists with this email, a reset link has been sent.", null);
        }

        User user = userOptional.get();

        // Generate unique token
        String token = generateUniqueToken();

        // Create and save reset token
        PasswordResetToken resetToken = new PasswordResetToken(token, normalizedEmail);
        tokenRepository.save(resetToken);

        // Send reset email
        try {
            emailService.sendPasswordResetEmail(normalizedEmail, token);
        } catch (Exception e) {
            // Log the error but don't expose it to user
            System.err.println("Failed to send password reset email: " + e.getMessage());
            return ApiResponse.error("Failed to send reset email. Please try again later.");
        }

        return ApiResponse.success("If an account exists with this email, a reset link has been sent.", null);
    }

    /**
     * Verify if a reset token is valid
     */
    public ApiResponse<Boolean> verifyResetToken(String token) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findValidToken(token, LocalDateTime.now());

        if (tokenOptional.isEmpty()) {
            return ApiResponse.error("Invalid or expired reset token.");
        }

        return ApiResponse.success("Token is valid.", true);
    }

    /**
     * Reset password using token
     */
    public ApiResponse<Void> resetPassword(String token, String newPassword) {
        // Find valid token
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findValidToken(token, LocalDateTime.now());

        if (tokenOptional.isEmpty()) {
            return ApiResponse.error("Invalid or expired reset token.");
        }

        PasswordResetToken resetToken = tokenOptional.get();

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(resetToken.getEmail());

        if (userOptional.isEmpty()) {
            return ApiResponse.error("User not found.");
        }

        User user = userOptional.get();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Send confirmation email
        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            // Log error but don't fail the password reset
            System.err.println("Failed to send password changed confirmation: " + e.getMessage());
        }

        return ApiResponse.success("Password has been reset successfully.", null);
    }

    /**
     * Send username reminder email
     */
    public ApiResponse<Void> sendUsernameReminder(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        // Always return success to prevent email enumeration
        if (userOptional.isEmpty()) {
            return ApiResponse.success("If an account exists with this email, your username has been sent.", null);
        }

        User user = userOptional.get();

        // Send username reminder email
        try {
            emailService.sendUsernameReminderEmail(normalizedEmail, user.getUsername());
        } catch (Exception e) {
            System.err.println("Failed to send username reminder: " + e.getMessage());
            return ApiResponse.error("Failed to send email. Please try again later.");
        }

        return ApiResponse.success("If an account exists with this email, your username has been sent.", null);
    }

    /**
     * Generate a unique token
     */
    private String generateUniqueToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Cleanup expired tokens (can be called by a scheduled task)
     */
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}