package taf.yugioh.scanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taf.yugioh.scanner.dto.*;
import taf.yugioh.scanner.entity.User;
import taf.yugioh.scanner.repository.UserRepository;
import taf.yugioh.scanner.security.JwtService;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.error("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email is already registered");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Save user
        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser.getId(), savedUser.getUsername());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId(), savedUser.getUsername());

        // Return response
        return AuthResponse.success(
                "Registration successful",
                accessToken,
                refreshToken,
                UserDTO.fromEntity(savedUser)
        );
    }

    /**
     * Login user with username or email
     */
    public AuthResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsernameOrEmail().trim();

        // Find user by username or email
        Optional<User> userOptional;
        if (usernameOrEmail.contains("@")) {
            userOptional = userRepository.findByEmail(usernameOrEmail.toLowerCase());
        } else {
            userOptional = userRepository.findByUsername(usernameOrEmail);
        }

        if (userOptional.isEmpty()) {
            return AuthResponse.error("Invalid credentials");
        }

        User user = userOptional.get();

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return AuthResponse.error("Invalid credentials");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.success(
                "Login successful",
                accessToken,
                refreshToken,
                UserDTO.fromEntity(user)
        );
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        String validationError = jwtService.validateToken(refreshToken);
        if (validationError != null) {
            return AuthResponse.error("Invalid refresh token: " + validationError);
        }

        // Check if it's a refresh token
        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            return AuthResponse.error("Invalid token type");
        }

        // Extract user info
        String username = jwtService.extractUsername(refreshToken);
        Long userId = jwtService.extractUserId(refreshToken);

        // Verify user still exists
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return AuthResponse.error("User not found");
        }

        User user = userOptional.get();

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(userId, username);

        return AuthResponse.success(
                "Token refreshed successfully",
                newAccessToken,
                refreshToken, // Return same refresh token
                UserDTO.fromEntity(user)
        );
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get current user info
     */
    public UserDTO getCurrentUser(User user) {
        return UserDTO.fromEntity(user);
    }
}