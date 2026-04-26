package in.proofofconcept.url.shortner.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import in.proofofconcept.url.shortner.exception.CustomException;
import org.hibernate.boot.model.naming.IllegalIdentifierException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.proofofconcept.url.shortner.dto.request.UrlRequest;
import in.proofofconcept.url.shortner.dto.response.UrlResponse;
import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.repository.UrlRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.ai.chat.model.ChatModel;

@Service
public class UrlService {


	private final UrlRepository urlRepository;
	private final ModelMapper modelMapper;
	private final ChatModel chatModel;
	private final QrCodeService qrCodeService;

	@Autowired
    UrlService(UrlRepository urlRepository, ModelMapper modelMapper, ChatModel chatModel, 
	           QrCodeService qrCodeService) {
        this.urlRepository = urlRepository;
		this.modelMapper = modelMapper;
		this.chatModel = chatModel;
		this.qrCodeService = qrCodeService;
    }
	
	public String generateSmartSlug(String originalUrl) {
		String prompt = "Generate a short, 2-word, hyphenated slug for this URL: " + originalUrl + 
		                ". Examples: 'quick-bake', 'travel-tips'. Return only the slug and nothing else.";
		try {
			String slug = chatModel.call(prompt).toLowerCase().trim();
			// Basic cleanup in case AI adds extra text
			slug = slug.replaceAll("[^a-z0-9-]", "");
			
			// Collision check
			String finalSlug = slug;
			int counter = 1;
			while (urlRepository.findByShortUrl(finalSlug) != null) {
				finalSlug = slug + "-" + counter++;
			}
			return finalSlug;
		} catch (Exception e) {
			// Fallback to standard hash-based short URL if AI fails
			return generateShortUrl(originalUrl);
		}
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
		if (url.getPassword() != null && !url.getPassword().isBlank()) {
			url.setPassword(BCrypt.hashpw(url.getPassword(), BCrypt.gensalt()));
		}

		// Perform AI Enhancements
		enrichUrlWithAI(url);
		
		if (!url.isSafe()) {
			throw new CustomException("Security Alert: The AI has flagged this URL as potentially unsafe or malicious.");
		}
		
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





	public Url fromRequest(UrlRequest request) {
		return modelMapper.map(request, Url.class);
	}

	public UrlResponse toResponse(Url url) {
		String qrCode = qrCodeService.generateQrCodeBase64(url.getShortUrl(), 250, 250);
		return UrlResponse.builder()
				.id(url.getId())
				.originalUrl(url.getOriginalUrl())
				.shortUrl(url.getShortUrl())
				.expiryDate(url.getExpiryDate())
				.clicks(url.getClicks())
				.qrCodeBase64(qrCode)
				.isPasswordProtected(url.getPassword() != null)
				.isOneTimeUse(url.isOneTimeUse())
				.summary(url.getSummary())
				.category(url.getCategory())
				.isSafe(url.isSafe())
				.build();
	}

	private void enrichUrlWithAI(Url url) {
		String prompt = """
				Analyze this URL: %s
				Provide a response in the following EXACT format:
				Safe: [Yes/No]
				Summary: [1-sentence description]
				Category: [Work/Shopping/Social/Education/Entertainment/Other]
				""".formatted(url.getOriginalUrl());

		try {
			String response = chatModel.call(prompt);
			String[] lines = response.split("\n");
			
			for (String line : lines) {
				if (line.toLowerCase().startsWith("safe:")) {
					url.setSafe(line.toLowerCase().contains("yes"));
				} else if (line.toLowerCase().startsWith("summary:")) {
					url.setSummary(line.substring(line.indexOf(":") + 1).trim());
				} else if (line.toLowerCase().startsWith("category:")) {
					url.setCategory(line.substring(line.indexOf(":") + 1).trim());
				}
			}
		} catch (Exception e) {
			// Fallback if AI fails
			url.setSafe(true); 
			url.setSummary("No summary available");
			url.setCategory("Uncategorized");
		}
	}

	public boolean verifyPassword(Url url, String rawPassword) {
		if (url.getPassword() == null) return true;
		if (rawPassword == null) return false;
		return BCrypt.checkpw(rawPassword, url.getPassword());
	}

	public void handleOneTimeUse(Url url) {
		if (url.isOneTimeUse()) {
			urlRepository.delete(url);
		}
	}

	public List<UrlResponse> getAllResponses() {
		return urlRepository.findAll()
				.stream()
				.map(this::toResponse)
				.toList();
	}

	public void deleteAllUrls() {
		urlRepository.deleteAll();
	}

}
