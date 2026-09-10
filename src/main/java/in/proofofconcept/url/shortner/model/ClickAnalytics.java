package in.proofofconcept.url.shortner.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_analytics", indexes = {
    @Index(name = "idx_click_analytics_url_id", columnList = "url_id"),
    @Index(name = "idx_click_analytics_timestamp", columnList = "clickTimestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    private LocalDateTime clickTimestamp;
    
    @Column(length = 1024)
    private String userAgent;
    
    private String ipAddress;
    
    @Column(length = 2048)
    private String referer;
    
    // Extracted telemetry dimensions
    private String browser;
    private String operatingSystem;
    private String deviceType;
    private String country;
}
