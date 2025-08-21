package in.proofofconcept.url.shortner.repository;

import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;

import in.proofofconcept.url.shortner.model.Url;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
	Url findByShortUrl(String shortUrl);
}
