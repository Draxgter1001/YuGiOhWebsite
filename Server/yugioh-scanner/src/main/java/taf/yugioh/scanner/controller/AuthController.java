package taf.yugioh.scanner.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import taf.yugioh.scanner.dto.*;
import taf.yugioh.scanner.entity.User;
import taf.yugioh.scanner.service.PasswordResetService;
import taf.yugioh.scanner.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Login with username/email and password
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * Refresh access token using refresh token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = userService.refreshToken(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * Get current authenticated user info
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }

        UserDTO userDTO = userService.getCurrentUser(user);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userDTO));
    }

    /**
     * Validate if token is still valid (for frontend checks)
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success("Token invalid", false));
        }
        return ResponseEntity.ok(ApiResponse.success("Token valid", true));
    }

    // ==================== Password Reset Endpoints ====================

    /**
     * Request password reset - sends email with reset link
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ApiResponse<Void> response = passwordResetService.requestPasswordReset(request.getEmail());

        // Always return 200 to prevent email enumeration
        return ResponseEntity.ok(response);
    }

    /**
     * Verify if reset token is valid
     * GET /api/auth/verify-reset-token
     */
    @GetMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> verifyResetToken(@RequestParam String token) {
        ApiResponse<Boolean> response = passwordResetService.verifyResetToken(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reset password using token
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ApiResponse<Void> response = passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Username Recovery Endpoint ====================

    /**
     * Request username reminder - sends email with username
     * POST /api/auth/forgot-username
     */
    @PostMapping("/forgot-username")
    public ResponseEntity<ApiResponse<Void>> forgotUsername(@Valid @RequestBody ForgotUsernameRequest request) {
        ApiResponse<Void> response = passwordResetService.sendUsernameReminder(request.getEmail());

        // Always return 200 to prevent email enumeration
        return ResponseEntity.ok(response);
    }
}