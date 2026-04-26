package in.proofofconcept.url.shortner.dto.request;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
