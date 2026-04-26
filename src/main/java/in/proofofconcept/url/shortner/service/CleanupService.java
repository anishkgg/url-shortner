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
     * Runs every day at 2:00 AM to remove expired URLs from the database.
     * Cron format: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Starting scheduled cleanup of expired URLs...");
        
        LocalDateTime now = LocalDateTime.now();
        List<Url> expiredUrls = urlRepository.findAllByExpiryDateBefore(now);
        
        if (!expiredUrls.isEmpty()) {
            log.info("Found {} expired URLs. Deleting...", expiredUrls.size());
            urlRepository.deleteAll(expiredUrls);
            log.info("Cleanup completed successfully.");
        } else {
            log.info("No expired URLs found to clean up.");
        }
    }
}
