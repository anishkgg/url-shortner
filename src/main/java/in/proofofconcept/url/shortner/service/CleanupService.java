package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CleanupService {

    private final UrlRepository urlRepository;

    public CleanupService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Runs every hour to mark expired URLs as inactive and log cleanup statistics.
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Starting scheduled scan for expired active URLs...");

        LocalDateTime now = LocalDateTime.now();
        List<Url> expiredActiveUrls = urlRepository.findAllByExpiryDateBeforeAndIsActiveTrue(now);

        if (!expiredActiveUrls.isEmpty()) {
            log.info("Found {} active URLs that have passed their expiration timestamp. Deactivating...", expiredActiveUrls.size());
            for (Url url : expiredActiveUrls) {
                url.setActive(false);
            }
            urlRepository.saveAll(expiredActiveUrls);
            log.info("Deactivated {} expired URLs successfully.", expiredActiveUrls.size());
        } else {
            log.info("No expired active URLs found.");
        }
    }
}
