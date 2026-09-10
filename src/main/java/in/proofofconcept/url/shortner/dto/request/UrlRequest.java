package in.proofofconcept.url.shortner.dto.request;

import in.proofofconcept.url.shortner.model.RedirectType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlRequest {

    @NotBlank(message = "Original URL cannot be blank")
    @URL(message = "Must be a valid URL (e.g., https://example.com)")
    private String originalUrl;

    // Optional custom alias
    private String shortUrl;
    private String customAlias;

    private LocalDateTime expiryDate;
    private Long maxClicks;
    private RedirectType redirectType;
    private String password;
    private boolean isOneTimeUse;

    public String getEffectiveAlias() {
        if (customAlias != null && !customAlias.isBlank()) {
            return customAlias.trim();
        }
        if (shortUrl != null && !shortUrl.isBlank()) {
            return shortUrl.trim();
        }
        return null;
    }
}
