package in.proofofconcept.url.shortner.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import in.proofofconcept.url.shortner.dto.UrlDto;
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
			return url;
		} else {
			return null;
		}
	}
	public Url saveUrl(Url url) {
		if (url.getShortUrl() == null || url.getShortUrl().isBlank()) {
			// auto-generate short URL
			url.setShortUrl(generateShortUrl());
		}

		if (url.getExpiryDate() == null) {
			// Set default expiry: 30 days
			url.setExpiryDate(LocalDateTime.now().plusDays(30));
		}

		return urlRepository.save(url);
	}

	public String generateShortUrl() {
		int length = 8;
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder sb = new StringBuilder();
		Random random = new Random();

		for (int i = 0; i < length; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}

		// Optional: check uniqueness
		while (urlRepository.findByShortUrl(sb.toString()) != null) {
			sb.setCharAt(random.nextInt(length), chars.charAt(random.nextInt(chars.length())));
		}

		return sb.toString();
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
		if(urlRepository.existsById(id)) {
			urlRepository.deleteById(id);
			return true;
		} else
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
