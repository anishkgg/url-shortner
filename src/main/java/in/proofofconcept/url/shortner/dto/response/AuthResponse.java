package in.proofofconcept.url.shortner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private String username;
    private String message;

    public AuthResponse(String username, String message) {
        this.username = username;
        this.message = message;
        this.tokenType = "Bearer";
    }
}
