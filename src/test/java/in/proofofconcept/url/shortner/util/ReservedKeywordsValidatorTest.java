package in.proofofconcept.url.shortner.util;

import in.proofofconcept.url.shortner.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ReservedKeywordsValidatorTest {

    private ReservedKeywordsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReservedKeywordsValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"api", "auth", "admin", "login", "register", "v1", "swagger", "health", "metrics", "qr", "gate", "verify"})
    void testReservedKeywordsAreRejected(String keyword) {
        CustomException exception = assertThrows(CustomException.class, () -> validator.validateSlug(keyword));
        assertTrue(exception.getMessage().contains("reserved system keyword"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-promo", "summer_sale_2026", "discount-50", "campaign-A", "deal123"})
    void testValidSlugsAreAccepted(String validSlug) {
        assertDoesNotThrow(() -> validator.validateSlug(validSlug));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "invalid!slug", "has spaces", "slug/with/slash", "slug?query"})
    void testMalformedSlugsAreRejected(String invalidSlug) {
        assertThrows(CustomException.class, () -> validator.validateSlug(invalidSlug));
    }

    @Test
    void testEmptySlugRejected() {
        assertThrows(CustomException.class, () -> validator.validateSlug(""));
        assertThrows(CustomException.class, () -> validator.validateSlug("   "));
        assertThrows(CustomException.class, () -> validator.validateSlug(null));
    }
}
