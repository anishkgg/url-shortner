package in.proofofconcept.url.shortner.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {
    private Long urlId;
    private String shortUrl;
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String countryHint;
    private LocalDateTime timestamp;
}
