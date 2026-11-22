package industries.angelic.wawelauth.wawelcore.util;

import java.util.Base64;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class Base64DecodingTest {

    // Only valid input goes to the JDK here: its decoder is what CVE-2026-114938 is about.
    @Test
    public void decodesWhatTheJdkEncodes() {
        Random random = new Random(42L);
        for (int length = 0; length < 300; length++) {
            byte[] data = new byte[length];
            random.nextBytes(data);
            Assert.assertArrayEquals(
                data,
                Base64Decoding.decode(
                    Base64.getEncoder()
                        .encodeToString(data)));
            Assert.assertArrayEquals(
                data,
                Base64Decoding.decode(
                    Base64.getEncoder()
                        .withoutPadding()
                        .encodeToString(data)));
            Assert.assertArrayEquals(
                data,
                Base64Decoding.decodeUrl(
                    Base64.getUrlEncoder()
                        .encodeToString(data)));
            Assert.assertArrayEquals(
                data,
                Base64Decoding.decodeUrl(
                    Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(data)));
        }
    }

    @Test
    public void acceptsWhatTheJdkAccepts() {
        Assert.assertEquals(0, Base64Decoding.decode("").length);
        Assert.assertArrayEquals(new byte[] { 0 }, Base64Decoding.decode("AA"));
        Assert.assertArrayEquals(new byte[] { 0, 0 }, Base64Decoding.decode("AAA"));
        // Leftover bits in the last character are ignored, as the JDK does.
        Assert.assertArrayEquals(new byte[] { 0 }, Base64Decoding.decode("AB=="));
    }

    @Test
    public void rejectsMalformedInput() {
        StringBuilder sixtyFourAs = new StringBuilder();
        for (int i = 0; i < 64; i++) sixtyFourAs.append('A');
        String[] malformed = { "=", "==", "A", "A=", "AA=", "AA=A", "AAA=A", "AAAA=", "A===", "AA===", "AAAA====",
            sixtyFourAs + "=", sixtyFourAs + "A=", sixtyFourAs + "AA=", sixtyFourAs + "AA==A", sixtyFourAs + "===",
            "AA*A", "AAAA AAAA", "AAAA\n", "AA-_", "ÿÿAA" };
        for (String text : malformed) {
            assertRejected(text, false);
        }
        assertRejected("AA+/", true);
    }

    private static void assertRejected(String text, boolean url) {
        try {
            if (url) {
                Base64Decoding.decodeUrl(text);
            } else {
                Base64Decoding.decode(text);
            }
        } catch (IllegalArgumentException expected) {
            return;
        }
        Assert.fail("Accepted malformed Base64: " + text);
    }
}
