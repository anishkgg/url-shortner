package in.proofofconcept.url.shortner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import in.proofofconcept.url.shortner.model.Url;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Url findByShortUrl(String shortUrl);

    boolean existsByShortUrl(String shortUrl);

    List<Url> findAllByUserId(Long userId);

    List<Url> findAllByExpiryDateBefore(LocalDateTime dateTime);

    List<Url> findAllByExpiryDateBeforeAndIsActiveTrue(LocalDateTime dateTime);
}
