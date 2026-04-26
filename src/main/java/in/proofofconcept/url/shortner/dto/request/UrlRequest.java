package in.proofofconcept.url.shortner.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UrlRequest {
    private String originalUrl;
    private String shortUrl; // Optional custom alias
    private LocalDateTime expiryDate;
    private String password;
    private boolean isOneTimeUse;
}
