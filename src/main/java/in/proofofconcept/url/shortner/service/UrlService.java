package in.proofofconcept.url.shortner.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.repository.UrlRepository;

@Service
public class UrlService {
	
	private final UrlRepository urlRepository;

    UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }
	
	public Url findByShortUrl(String shortUrl) {
		Url url = urlRepository.findByShortUrl(shortUrl);
		if (url != null && url.getExpiryDate().isAfter(LocalDateTime.now())) {
			return url;
		} else {
			return null;
		}
	}
	public Url saveUrl(Url url) {
		return urlRepository.save(url);
	}
}
