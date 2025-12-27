package taf.yugioh.scanner.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (we use JWT tokens)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Set session management to stateless (JWT-based)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/cards/**").permitAll()
                        .requestMatchers("/api/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/decks/public/**").permitAll()

                        // Health check endpoints
                        .requestMatchers("/actuator/health").permitAll()

                        // Protected endpoints - authentication required
                        .requestMatchers("/api/decks/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 (good balance of security and performance)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins from config
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Exposed headers (headers the client can access)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // How long the browser should cache the CORS config (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}