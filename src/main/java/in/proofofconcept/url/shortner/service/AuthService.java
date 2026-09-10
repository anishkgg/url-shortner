package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.dto.request.AuthRequest;
import in.proofofconcept.url.shortner.dto.request.LoginRequest;
import in.proofofconcept.url.shortner.dto.response.AuthResponse;
import in.proofofconcept.url.shortner.exception.CustomException;
import in.proofofconcept.url.shortner.model.User;
import in.proofofconcept.url.shortner.repository.UserRepository;
import in.proofofconcept.url.shortner.util.JwtUtil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new CustomException("Username cannot be empty");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new CustomException("Password must be at least 6 characters long");
        }
        if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
            throw new CustomException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs())
                .username(user.getUsername())
                .message("User registered successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new CustomException("Username and password are required");
        }

        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new CustomException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs())
                .username(user.getUsername())
                .message("Login successful")
                .build();
    }

    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new CustomException("Authentication is required");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found in context"));
    }

    public Map<String, Object> getCurrentUserProfile() {
        User user = getCurrentAuthenticatedUser();
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole());
        profile.put("createdAt", user.getCreatedAt());
        return profile;
    }
}
