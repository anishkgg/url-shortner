package in.proofofconcept.url.shortner.util;

import in.proofofconcept.url.shortner.exception.CustomException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ReservedKeywordsValidator {

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "api",
            "auth",
            "admin",
            "administrator",
            "login",
            "logout",
            "signin",
            "signout",
            "register",
            "signup",
            "v1",
            "v2",
            "v3",
            "actuator",
            "swagger",
            "swagger-ui",
            "api-docs",
            "health",
            "metrics",
            "info",
            "env",
            "static",
            "public",
            "assets",
            "css",
            "js",
            "images",
            "favicon.ico",
            "robots.txt",
            "r",
            "qr",
            "gate",
            "verify",
            "error",
            "urls",
            "url",
            "analytics",
            "shortner",
            "shortener",
            "dashboard",
            "manage",
            "account",
            "profile"
    );

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");

    /**
     * Validates that the custom alias is well-formed and not reserved.
     * @param slug the custom alias
     * @throws CustomException if alias is reserved or invalid
     */
    public void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new CustomException("Custom alias cannot be empty");
        }

        String normalized = slug.trim().toLowerCase();

        if (RESERVED_KEYWORDS.contains(normalized)) {
            throw new CustomException("The alias '" + slug + "' is a reserved system keyword and cannot be used");
        }

        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new CustomException("Custom alias must be 3-50 characters long and contain only letters, numbers, hyphens, and underscores");
        }
    }

    public boolean isReserved(String slug) {
        if (slug == null) return false;
        return RESERVED_KEYWORDS.contains(slug.trim().toLowerCase());
    }
}
