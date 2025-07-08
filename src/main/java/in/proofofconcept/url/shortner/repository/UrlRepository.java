package in.proofofconcept.url.shortner.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import in.proofofconcept.url.shortner.model.Url;

import java.util.Optional;
import java.util.UUID;

public interface UrlRepository extends JpaRepository<Url, Long> {
	Url findByShortUrl(String shortUrl);

}
