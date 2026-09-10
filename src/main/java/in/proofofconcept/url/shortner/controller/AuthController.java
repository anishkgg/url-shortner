package in.proofofconcept.url.shortner.controller;

import in.proofofconcept.url.shortner.dto.request.AuthRequest;
import in.proofofconcept.url.shortner.dto.request.LoginRequest;
import in.proofofconcept.url.shortner.dto.response.AuthResponse;
import in.proofofconcept.url.shortner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }
}
