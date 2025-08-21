package in.proofofconcept.url.shortner.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import in.proofofconcept.url.shortner.dto.UrlDto;
import in.proofofconcept.url.shortner.expection.CustomException;
import org.hibernate.boot.model.naming.IllegalIdentifierException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.repository.UrlRepository;

@Service
public class UrlService {


	private final UrlRepository urlRepository;
	private final ModelMapper modelMapper;

	@Autowired
    UrlService(UrlRepository urlRepository, ModelMapper modelMapper) {
        this.urlRepository = urlRepository;
		this.modelMapper = modelMapper;
    }
	
	public Url findByShortUrl(String shortUrl) {
		Url url = urlRepository.findByShortUrl(shortUrl);

		if (url != null && url.getExpiryDate().isAfter(LocalDateTime.now())) {
			url.setClicks(url.getClicks() + 1);
			urlRepository.save(url);
			return url;
		} else {
			return null;
		}
	}
	public Url saveUrl(Url url) {
		if (url.getShortUrl() == null || url.getShortUrl().isBlank()) {
			// auto-generate short URL
			url.setShortUrl(generateShortUrl(url.getOriginalUrl()));
		} else {
			// custom alias provided
			if (urlRepository.findByShortUrl(url.getShortUrl()) != null) {
				throw new CustomException("Custom alias is already in use");
			}
		}

		if (url.getExpiryDate() == null) {
			// Set default expiry: 30 days
			url.setExpiryDate(LocalDateTime.now().plusDays(30));
		}
		url.setClicks(0L);

		return urlRepository.save(url);
	}

	public List<Url> saveMultipleUrls(List<Url> urls) {
		return urls.stream().map(this::saveUrl).toList();
	}

	public String generateShortUrl(String originalUrl) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(originalUrl.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			String encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
			String shortUrl = encoded.substring(0, 8);

			// In case of a hash collision, append a random character until it's unique
			while (urlRepository.findByShortUrl(shortUrl) != null) {
				shortUrl += (char) (new java.util.Random().nextInt(26) + 'a');
			}
			return shortUrl;
		} catch (java.security.NoSuchAlgorithmException e) {
			// This should never happen
			throw new RuntimeException("SHA-256 algorithm not found", e);
		}
	}

	public Url getOriginalUrlById(Long id) {
		return urlRepository.findById(id).orElse(null);
	}

	public Url updateUrl(Long id, Url updatedUrl) {
		Url existingUrl = urlRepository.findById(id).orElse(null);

		if(existingUrl != null) {
			existingUrl.setOriginalUrl(updatedUrl.getOriginalUrl());
			existingUrl.setShortUrl(updatedUrl.getShortUrl());
			existingUrl.setExpiryDate(updatedUrl.getExpiryDate());
			return urlRepository.save(existingUrl);
		}
		return null;
	}

	public boolean deleteUrl(Long id) {
		try {
			if(urlRepository.existsById(id)) {
				urlRepository.deleteById(id);
				return true;
			}
		} catch (Exception e) {
			System.out.println(e);
			throw new CustomException("this url is not valid");
		}
		return false;
	}





	public Url fromDto(UrlDto dto) {
		return modelMapper.map(dto, Url.class);
	}

	public UrlDto toDto(Url url) {
		return modelMapper.map(url, UrlDto.class);
	}

	public List<UrlDto> getAllDto() {
		return urlRepository.findAll()
				.stream()
				.map(this::toDto)
				.toList();
	}

	public void deleteAllUrls() {
		urlRepository.deleteAll();
	}

}
