package in.proofofconcept.url.shortner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.proofofconcept.url.shortner.dto.request.AuthRequest;
import in.proofofconcept.url.shortner.dto.request.LoginRequest;
import in.proofofconcept.url.shortner.dto.request.UrlRequest;
import in.proofofconcept.url.shortner.dto.request.VerifyPasswordRequest;
import in.proofofconcept.url.shortner.model.RedirectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private final String testUsername = "developer_" + System.currentTimeMillis();

    @BeforeEach
    void setUp() throws Exception {
        // Register test user
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword("securePassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Login to get JWT
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testUsername)
                .password("securePassword123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        jwtToken = responseNode.get("token").asText();
        assertNotNull(jwtToken);
    }

    @Test
    void testCreateShortUrlWithBase62AutoGeneration() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://spring.io/projects/spring-boot")
                .redirectType(RedirectType.TEMPORARY_302)
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl").value("https://spring.io/projects/spring-boot"))
                .andExpect(jsonPath("$.qrCodeBase64").isNotEmpty())
                .andExpect(jsonPath("$.fullShortUrl").isNotEmpty());
    }

    @Test
    void testCreateCustomAlias() throws Exception {
        String customSlug = "spring-boot-docs-" + System.currentTimeMillis();
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://docs.spring.io")
                .customAlias(customSlug)
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value(customSlug));
    }

    @Test
    void testReservedKeywordsAreBlockedForCustomAlias() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("admin")
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("reserved system keyword")));
    }

    @Test
    void testRedirectionEngine302() throws Exception {
        String slug = "redir-test-" + System.currentTimeMillis();
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://github.com")
                .customAlias(slug)
                .redirectType(RedirectType.TEMPORARY_302)
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Perform redirect
        mockMvc.perform(get("/r/" + slug))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://github.com"))
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void testRedirectionEngine301Permanent() throws Exception {
        String slug = "perm-test-" + System.currentTimeMillis();
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://wikipedia.org")
                .customAlias(slug)
                .redirectType(RedirectType.PERMANENT_301)
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Perform redirect
        mockMvc.perform(get("/r/" + slug))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://wikipedia.org"))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    void testPasswordProtectedAccessAndInterstitialGate() throws Exception {
        String slug = "secret-" + System.currentTimeMillis();
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://confidential.example.com")
                .customAlias(slug)
                .password("VaultKey#2026")
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPasswordProtected").value(true));

        // 1. Unauthorized when calling directly without password
        mockMvc.perform(get("/r/" + slug).header("Accept", "application/json"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("password protected")));

        // 2. Interstitial Gate Page served for browser requests
        mockMvc.perform(get("/gate/" + slug))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Password Protected Link")));

        // 3. Verify via API
        VerifyPasswordRequest verifyReq = new VerifyPasswordRequest("VaultKey#2026");
        mockMvc.perform(post("/api/v1/url/" + slug + "/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.originalUrl").value("https://confidential.example.com"));

        // 4. Access with X-Password header
        mockMvc.perform(get("/r/" + slug).header("X-Password", "VaultKey#2026"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://confidential.example.com"));
    }

    @Test
    void testClickThresholdExpiration() throws Exception {
        String slug = "one-click-" + System.currentTimeMillis();
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://example.com/one-time")
                .customAlias(slug)
                .maxClicks(1L)
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // First click succeeds
        mockMvc.perform(get("/r/" + slug))
                .andExpect(status().isFound());

        // Second click fails due to threshold reached
        mockMvc.perform(get("/r/" + slug))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message", containsString("maximum click limit")));
    }

    @Test
    void testQrCodeImageDownloadEndpoint() throws Exception {
        String slug = "qr-test-" + System.currentTimeMillis();
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://example.com/qr")
                .customAlias(slug)
                .build();

        mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/url/" + slug + "/qr?download=true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Content-Disposition", containsString("attachment;")));
    }

    @Test
    void testUserLinkManagementAndAnalyticsSummary() throws Exception {
        String slug = "manage-test-" + System.currentTimeMillis();
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://analytics.example.com")
                .customAlias(slug)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/url/save")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long urlId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Check user's links
        mockMvc.perform(get("/api/v1/url/my-links")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // Click the URL once
        mockMvc.perform(get("/r/" + slug)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://news.ycombinator.com"))
                .andExpect(status().isFound());

        // Check Analytics Summary
        mockMvc.perform(get("/api/v1/url/" + urlId + "/analytics/summary")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlId").value(urlId))
                .andExpect(jsonPath("$.browsers").isMap())
                .andExpect(jsonPath("$.deviceTypes").isMap());

        // Toggle active status
        mockMvc.perform(patch("/api/v1/url/" + urlId + "/toggle-status")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // When inactive, redirect must fail
        mockMvc.perform(get("/r/" + slug))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message", containsString("deactivated")));

        // Delete URL
        mockMvc.perform(delete("/api/v1/url/delete/" + urlId)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}
