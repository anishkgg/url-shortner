package in.proofofconcept.url.shortner.repository;

import in.proofofconcept.url.shortner.model.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {
    List<ClickAnalytics> findByUrlId(Long urlId);
}
