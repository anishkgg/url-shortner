package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.dto.request.AuthRequest;
import in.proofofconcept.url.shortner.dto.response.AuthResponse;
import in.proofofconcept.url.shortner.exception.CustomException;
import in.proofofconcept.url.shortner.model.User;
import in.proofofconcept.url.shortner.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new CustomException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        return new AuthResponse(user.getUsername(), "User registered successfully");
    }
}
