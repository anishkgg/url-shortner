package in.proofofconcept.url.shortner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id")
    private Url url;

    private LocalDateTime clickTimestamp;
    private String userAgent;
    private String ipAddress;
    private String referer;
    
    // Extracted for easier querying later
    private String browser;
    private String operatingSystem;
    private String deviceType;
    private String country;
}
