package in.proofofconcept.url.shortner.util;

public final class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private Base62Encoder() {
        // Utility class
    }

    /**
     * Encodes a positive 64-bit integer ID into a Base62 alphanumeric string.
     */
    public static String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        // Use absolute value if negative
        long value = Math.abs(id);
        StringBuilder sb = new StringBuilder();

        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));
            value /= BASE;
        }

        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 alphanumeric string back into a 64-bit integer ID.
     */
    public static long decode(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Base62 string cannot be null or empty");
        }

        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit = ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
            }
            result = result * BASE + digit;
        }

        return result;
    }

    /**
     * Pads the encoded string with leading zero characters up to minLength.
     */
    public static String encodeWithPadding(long id, int minLength) {
        String encoded = encode(id);
        if (encoded.length() >= minLength) {
            return encoded;
        }
        return "0".repeat(minLength - encoded.length()) + encoded;
    }
}
