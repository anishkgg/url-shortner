package in.proofofconcept.url.shortner.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import in.proofofconcept.url.shortner.model.Url;

public interface UrlRepository extends JpaRepository<Url, Long>{
	Url findByShortUrl(String shortUrl);
}
