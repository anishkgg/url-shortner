package in.proofofconcept.url.shortner.dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UrlDto {
    @Id
    private Long id;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime expiryDate;
}
