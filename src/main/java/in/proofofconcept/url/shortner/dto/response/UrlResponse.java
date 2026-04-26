package in.proofofconcept.url.shortner.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UrlResponse {
    private Long id;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime expiryDate;
    private Long clicks;
    private String qrCodeBase64;
    private boolean isPasswordProtected;
    private boolean isOneTimeUse;
    private String summary;
    private String category;
    private boolean isSafe;
}
