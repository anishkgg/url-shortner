package in.proofofconcept.url.shortner.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "urls", indexes = {
    @Index(name = "idx_urls_short_url", columnList = "shortUrl", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "short_url", unique = true, nullable = false, length = 100)
    private String shortUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "redirect_type", nullable = false)
    @Builder.Default
    private RedirectType redirectType = RedirectType.TEMPORARY_302;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "max_clicks")
    private Long maxClicks;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    private String password; // Hashed with BCrypt

    @Column(name = "is_one_time_use", nullable = false)
    @Builder.Default
    private boolean isOneTimeUse = false;

    private String summary;
    private String category;

    @Column(name = "is_safe", nullable = false)
    @Builder.Default
    private boolean isSafe = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClickAnalytics> analytics;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.clicks == null) {
            this.clicks = 0L;
        }
        if (this.redirectType == null) {
            this.redirectType = RedirectType.TEMPORARY_302;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
