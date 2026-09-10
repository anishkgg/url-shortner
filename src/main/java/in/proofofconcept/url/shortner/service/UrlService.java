package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.dto.request.UrlRequest;
import in.proofofconcept.url.shortner.dto.response.UrlResponse;
import in.proofofconcept.url.shortner.exception.CustomException;
import in.proofofconcept.url.shortner.model.RedirectType;
import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.model.User;
import in.proofofconcept.url.shortner.repository.UrlRepository;
import in.proofofconcept.url.shortner.repository.UserRepository;
import in.proofofconcept.url.shortner.util.Base62Encoder;
import in.proofofconcept.url.shortner.util.ReservedKeywordsValidator;
import in.proofofconcept.url.shortner.util.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ReservedKeywordsValidator reservedKeywordsValidator;
    private final SafeBrowsingService safeBrowsingService;
    private final QrCodeService qrCodeService;
    private final ChatModel chatModel;
    private final String baseUrl;

    @Autowired
    public UrlService(
            UrlRepository urlRepository,
            UserRepository userRepository,
            SnowflakeIdGenerator snowflakeIdGenerator,
            ReservedKeywordsValidator reservedKeywordsValidator,
            SafeBrowsingService safeBrowsingService,
            QrCodeService qrCodeService,
            @Autowired(required = false) ChatModel chatModel,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.reservedKeywordsValidator = reservedKeywordsValidator;
        this.safeBrowsingService = safeBrowsingService;
        this.qrCodeService = qrCodeService;
        this.chatModel = chatModel;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new CustomException("Authentication is required");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found in context"));
    }

    /**
     * Creates and saves a shortened URL.
     */
    @Transactional
    public Url createUrl(UrlRequest request) {
        User currentUser = getCurrentUser();

        // 1. Threat Intelligence & Malicious URL Scanner
        safeBrowsingService.validateUrlSafety(request.getOriginalUrl());

        // 2. Resolve short slug / alias
        String shortSlug;
        String requestedAlias = request.getEffectiveAlias();

        if (requestedAlias != null && !requestedAlias.isBlank()) {
            // Validate custom alias against reserved keywords and regex
            reservedKeywordsValidator.validateSlug(requestedAlias);

            if (urlRepository.existsByShortUrl(requestedAlias)) {
                throw new CustomException("Custom alias '" + requestedAlias + "' is already in use. Please choose another one.");
            }
            shortSlug = requestedAlias;
        } else {
            // Generate unique Base62 token backed by distributed Snowflake ID generator
            long uniqueId = snowflakeIdGenerator.nextId();
            shortSlug = Base62Encoder.encodeWithPadding(uniqueId, 6);

            // Safety check against collision
            while (urlRepository.existsByShortUrl(shortSlug)) {
                uniqueId = snowflakeIdGenerator.nextId();
                shortSlug = Base62Encoder.encodeWithPadding(uniqueId, 6);
            }
        }

        // 3. Password Hashing
        String hashedPassword = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            hashedPassword = BCrypt.hashpw(request.getPassword().trim(), BCrypt.gensalt(10));
        }

        // 4. Expiry & Click threshold setup
        LocalDateTime expiry = request.getExpiryDate();
        if (expiry == null) {
            // Default 30-day TTL if not specified
            expiry = LocalDateTime.now().plusDays(30);
        }

        Long maxClicks = request.getMaxClicks();
        if (request.isOneTimeUse()) {
            maxClicks = 1L;
        }

        RedirectType redirectType = request.getRedirectType() != null
                ? request.getRedirectType()
                : RedirectType.TEMPORARY_302;

        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl().trim())
                .shortUrl(shortSlug)
                .redirectType(redirectType)
                .expiryDate(expiry)
                .clicks(0L)
                .maxClicks(maxClicks)
                .isActive(true)
                .password(hashedPassword)
                .isOneTimeUse(request.isOneTimeUse())
                .isSafe(true)
                .user(currentUser)
                .build();

        // 5. Optional AI Enrichment
        enrichUrlWithAI(url);

        return urlRepository.save(url);
    }

    public List<Url> createMultipleUrls(List<UrlRequest> requests) {
        return requests.stream().map(this::createUrl).toList();
    }

    /**
     * Resolves and validates a short URL for redirection.
     * Increments click count and verifies TTL and click threshold.
     */
    @Transactional
    public Url resolveAndIncrementClicks(String shortUrl) {
        Url url = urlRepository.findByShortUrl(shortUrl);
        if (url == null) {
            return null;
        }

        // Check Click Threshold Expiration
        if (url.getMaxClicks() != null && url.getClicks() >= url.getMaxClicks()) {
            url.setActive(false);
            urlRepository.save(url);
            throw new CustomException("This link has reached its maximum click limit");
        }

        // Check TTL Expiration
        if (url.getExpiryDate() != null && LocalDateTime.now().isAfter(url.getExpiryDate())) {
            url.setActive(false);
            urlRepository.save(url);
            throw new CustomException("This link has expired");
        }

        // Check if explicitly deactivated
        if (!url.isActive()) {
            throw new CustomException("This link has been deactivated");
        }

        // Increment Click Count
        url.setClicks(url.getClicks() + 1);

        // Auto-deactivate if threshold is reached on this click
        if (url.isOneTimeUse() || (url.getMaxClicks() != null && url.getClicks() >= url.getMaxClicks())) {
            url.setActive(false);
        }

        return urlRepository.save(url);
    }

    public Url findByShortUrl(String shortUrl) {
        return urlRepository.findByShortUrl(shortUrl);
    }

    public boolean verifyPassword(Url url, String rawPassword) {
        if (url.getPassword() == null) return true;
        if (rawPassword == null || rawPassword.isBlank()) return false;
        try {
            return BCrypt.checkpw(rawPassword.trim(), url.getPassword());
        } catch (Exception e) {
            return false;
        }
    }

    public List<UrlResponse> getUserUrls() {
        User currentUser = getCurrentUser();
        return urlRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Url getOriginalUrlById(Long id) {
        return urlRepository.findById(id).orElse(null);
    }

    @Transactional
    public Url updateUrl(Long id, UrlRequest request) {
        Url existing = urlRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }

        // Verify Ownership
        User currentUser = getCurrentUser();
        if (existing.getUser() == null || !existing.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException("You do not have permission to modify this URL");
        }

        if (request.getOriginalUrl() != null && !request.getOriginalUrl().isBlank()) {
            safeBrowsingService.validateUrlSafety(request.getOriginalUrl());
            existing.setOriginalUrl(request.getOriginalUrl().trim());
        }

        String requestedAlias = request.getEffectiveAlias();
        if (requestedAlias != null && !requestedAlias.equalsIgnoreCase(existing.getShortUrl())) {
            reservedKeywordsValidator.validateSlug(requestedAlias);
            if (urlRepository.existsByShortUrl(requestedAlias)) {
                throw new CustomException("Custom alias '" + requestedAlias + "' is already in use.");
            }
            existing.setShortUrl(requestedAlias);
        }

        if (request.getExpiryDate() != null) {
            existing.setExpiryDate(request.getExpiryDate());
        }

        if (request.getMaxClicks() != null) {
            existing.setMaxClicks(request.getMaxClicks());
        }

        if (request.getRedirectType() != null) {
            existing.setRedirectType(request.getRedirectType());
        }

        if (request.getPassword() != null) {
            if (request.getPassword().isBlank()) {
                existing.setPassword(null); // Clear password
            } else {
                existing.setPassword(BCrypt.hashpw(request.getPassword().trim(), BCrypt.gensalt(10)));
            }
        }

        return urlRepository.save(existing);
    }

    @Transactional
    public Url toggleStatus(Long id) {
        Url url = urlRepository.findById(id).orElse(null);
        if (url == null) {
            return null;
        }

        User currentUser = getCurrentUser();
        if (url.getUser() == null || !url.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException("You do not have permission to modify this URL");
        }

        url.setActive(!url.isActive());
        return urlRepository.save(url);
    }

    @Transactional
    public boolean deleteUrl(Long id) {
        Url url = urlRepository.findById(id).orElse(null);
        if (url == null) {
            return false;
        }

        User currentUser = getCurrentUser();
        if (url.getUser() == null || !url.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException("You do not have permission to delete this URL");
        }

        urlRepository.delete(url);
        return true;
    }

    public UrlResponse toResponse(Url url) {
        String fullShortUrl = baseUrl + "/r/" + url.getShortUrl();
        String qrDownloadUrl = baseUrl + "/api/v1/url/" + url.getShortUrl() + "/qr";
        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(fullShortUrl, 250, 250);

        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(url.getShortUrl())
                .fullShortUrl(fullShortUrl)
                .redirectType(url.getRedirectType())
                .expiryDate(url.getExpiryDate())
                .clicks(url.getClicks())
                .maxClicks(url.getMaxClicks())
                .isActive(url.isActive())
                .qrCodeBase64(qrCodeBase64)
                .qrCodeDownloadUrl(qrDownloadUrl)
                .isPasswordProtected(url.getPassword() != null)
                .isOneTimeUse(url.isOneTimeUse())
                .summary(url.getSummary())
                .category(url.getCategory())
                .isSafe(url.isSafe())
                .createdAt(url.getCreatedAt())
                .build();
    }

    private void enrichUrlWithAI(Url url) {
        if (chatModel == null) {
            url.setSafe(true);
            url.setSummary("AI enrichment unavailable");
            url.setCategory("Uncategorized");
            return;
        }

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
            log.debug("AI analysis skipped or failed: {}", e.getMessage());
            url.setSafe(true);
            url.setSummary("No summary available");
            url.setCategory("Uncategorized");
        }
    }
}
