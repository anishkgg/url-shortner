package in.proofofconcept.url.shortner.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ClickAnalyticsResponse {
    private Long id;
    private LocalDateTime clickTimestamp;
    private String userAgent;
    private String ipAddress;
    private String referer;
    private String browser;
    private String operatingSystem;
    private String deviceType;
}
