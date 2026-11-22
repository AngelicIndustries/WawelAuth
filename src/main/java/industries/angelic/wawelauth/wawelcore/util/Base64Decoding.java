package industries.angelic.wawelauth.wawelcore.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Base64 decoding in plain Java, with the same results and errors as {@code java.util.Base64}'s basic and URL-safe
 * decoders.
 * <p>
 * Java 17.0.6 and later replace those decoders' inner loop with machine code on x86-64 processors with AVX-512 VBMI,
 * and that code corrupts memory on some malformed input (CVE-2026-114938, "Java4Shell"). Servers, providers and
 * players control much of what Wawel Auth decodes, so it never hands Base64 to the JDK's decoders. Encoding is not
 * affected.
 */
public final class Base64Decoding {

    private static final int[] BASIC = table("+/");
    private static final int[] URL = table("-_");
    private static final int PADDING = -2;

    private Base64Decoding() {}

    /** As {@code Base64.getDecoder().decode(text)}. */
    public static byte[] decode(String text) {
        return decode(text.getBytes(StandardCharsets.ISO_8859_1), BASIC);
    }

    /** As {@code Base64.getUrlDecoder().decode(text)}. */
    public static byte[] decodeUrl(String text) {
        return decode(text.getBytes(StandardCharsets.ISO_8859_1), URL);
    }

    private static byte[] decode(byte[] src, int[] alphabet) {
        byte[] dst = new byte[src.length / 4 * 3 + 2];
        int dp = 0;
        int bits = 0;
        int shift = 18;
        int sp = 0;
        while (sp < src.length) {
            int b = alphabet[src[sp++] & 0xff];
            if (b < 0) {
                if (b == PADDING) {
                    // "=" alone, "xx=" without its second "=", or "xx=" followed by something else.
                    if (shift == 18 || shift == 6 && (sp == src.length || src[sp++] != '=')) {
                        throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                    }
                    break;
                }
                throw new IllegalArgumentException("Illegal base64 character " + Integer.toString(src[sp - 1], 16));
            }
            bits |= b << shift;
            shift -= 6;
            if (shift < 0) {
                dst[dp++] = (byte) (bits >> 16);
                dst[dp++] = (byte) (bits >> 8);
                dst[dp++] = (byte) bits;
                shift = 18;
                bits = 0;
            }
        }
        if (shift == 6) {
            dst[dp++] = (byte) (bits >> 16);
        } else if (shift == 0) {
            dst[dp++] = (byte) (bits >> 16);
            dst[dp++] = (byte) (bits >> 8);
        } else if (shift == 12) {
            throw new IllegalArgumentException("Last unit does not have enough valid bits");
        }
        if (sp < src.length) {
            throw new IllegalArgumentException("Input byte array has incorrect ending byte at " + sp);
        }
        byte[] out = new byte[dp];
        System.arraycopy(dst, 0, out, 0, dp);
        return out;
    }

    private static int[] table(String lastTwo) {
        int[] table = new int[256];
        Arrays.fill(table, -1);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + lastTwo;
        for (int i = 0; i < alphabet.length(); i++) {
            table[alphabet.charAt(i)] = i;
        }
        table['='] = PADDING;
        return table;
    }
}
