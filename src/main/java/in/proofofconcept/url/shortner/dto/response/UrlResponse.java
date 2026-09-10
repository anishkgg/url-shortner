package in.proofofconcept.url.shortner.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import in.proofofconcept.url.shortner.model.RedirectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponse {
    private Long id;
    private String originalUrl;
    private String shortUrl;
    private String fullShortUrl;
    private RedirectType redirectType;
    private LocalDateTime expiryDate;
    private Long clicks;
    private Long maxClicks;

    @JsonProperty("isActive")
    private boolean isActive;

    private String qrCodeBase64;
    private String qrCodeDownloadUrl;

    @JsonProperty("isPasswordProtected")
    private boolean isPasswordProtected;

    @JsonProperty("isOneTimeUse")
    private boolean isOneTimeUse;

    private String summary;
    private String category;

    @JsonProperty("isSafe")
    private boolean isSafe;

    private LocalDateTime createdAt;
}
