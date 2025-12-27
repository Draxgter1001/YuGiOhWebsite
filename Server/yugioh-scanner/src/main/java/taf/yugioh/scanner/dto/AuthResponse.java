package taf.yugioh.scanner.dto;

public class AuthResponse {

    private boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
    private UserDTO user;

    // Constructors
    public AuthResponse() {}

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, String accessToken, String refreshToken, UserDTO user) {
        this.success = success;
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // Static factory methods for common responses
    public static AuthResponse success(String message, String accessToken, String refreshToken, UserDTO user) {
        return new AuthResponse(true, message, accessToken, refreshToken, user);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}