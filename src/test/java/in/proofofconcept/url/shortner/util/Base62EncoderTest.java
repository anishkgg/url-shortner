package in.proofofconcept.url.shortner.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void testEncodeZero() {
        assertEquals("0", Base62Encoder.encode(0));
    }

    @Test
    void testEncodeAndDecodeReversibility() {
        long[] testValues = {0L, 1L, 61L, 62L, 1000L, 123456789L, 9876543210123L, Long.MAX_VALUE / 2};

        for (long value : testValues) {
            String encoded = Base62Encoder.encode(value);
            assertNotNull(encoded);
            assertFalse(encoded.isEmpty());
            long decoded = Base62Encoder.decode(encoded);
            assertEquals(value, decoded, "Decoded value must equal original value: " + value);
        }
    }

    @Test
    void testEncodeWithPadding() {
        String padded = Base62Encoder.encodeWithPadding(5L, 6);
        assertEquals(6, padded.length());
        assertTrue(padded.startsWith("00000"));

        String longer = Base62Encoder.encodeWithPadding(1234567890123L, 4);
        assertTrue(longer.length() >= 4);
    }

    @Test
    void testDecodeInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("invalid!@#"));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(""));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(null));
    }
}
