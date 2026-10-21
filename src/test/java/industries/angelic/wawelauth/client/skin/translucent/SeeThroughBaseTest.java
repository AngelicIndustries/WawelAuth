package industries.angelic.wawelauth.client.skin.translucent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.math.BigInteger;

import org.junit.Test;

import industries.angelic.wawelauth.wawelcore.data.SkinModel;

public class SeeThroughBaseTest {

    private static final int[][] ALFALFA_REGIONS = { { 8, 0, 24, 8 }, { 0, 8, 8, 16 }, { 16, 8, 32, 16 },
        { 4, 16, 12, 20 }, { 20, 16, 36, 20 }, { 44, 16, 52, 20 }, { 0, 20, 56, 32 }, { 20, 48, 28, 52 },
        { 36, 48, 44, 52 }, { 16, 52, 48, 64 } };

    @Test
    public void onlySemiTransparentBaseFaceTexelsGetTheirAlphaBack() {
        BufferedImage original = opaqueSkin();
        // The face's front, a texel of the head's unused corner and a hole in the body's front.
        original.setRGB(10, 10, 0x80112233);
        original.setRGB(1, 1, 0x80445566);
        original.setRGB(21, 22, 0x00778899);
        BufferedImage normalized = forcedOpaque(original);

        BufferedImage restored = SeeThroughBase.restored(original, normalized, SkinModel.CLASSIC);
        assertNotNull(restored);
        assertEquals(0x80112233, restored.getRGB(10, 10));
        assertEquals(normalized.getRGB(1, 1), restored.getRGB(1, 1));
        // A fully transparent base texel stays opaque, as vanilla has it.
        assertEquals(normalized.getRGB(21, 22), restored.getRGB(21, 22));
        assertEquals(0xFF, restored.getRGB(21, 22) >>> 24);
    }

    @Test
    public void opaqueAndLegacySkinsHaveNone() {
        BufferedImage opaque = opaqueSkin();
        assertNull(SeeThroughBase.restored(opaque, forcedOpaque(opaque), SkinModel.CLASSIC));

        BufferedImage legacy = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        legacy.setRGB(10, 10, 0x80112233);
        assertNull(SeeThroughBase.restored(legacy, opaqueSkin(), SkinModel.CLASSIC));
    }

    @Test
    public void slimArmsLeaveTheUnusedColumnOut() {
        BufferedImage original = opaqueSkin();
        // The right arm's back face ends a texel earlier on slim skins.
        original.setRGB(55, 25, 0x80112233);
        assertNotNull(SeeThroughBase.restored(original, forcedOpaque(original), SkinModel.CLASSIC));
        assertNull(SeeThroughBase.restored(original, forcedOpaque(original), SkinModel.SLIM));
    }

    @Test
    public void earsDataInTheBaseAlphaKeepsItOpaque() {
        BufferedImage ears = opaqueSkin();
        alfalfa(ears, new byte[] { (byte) 0xEA, 0x1F, (byte) 0xA1, (byte) 0xFA, 1, 0x40, 0x00, 0x05, 'h', 'e', 'l' });
        assertTrue(SeeThroughBase.alfalfa(ears));
        assertNull(SeeThroughBase.restored(ears, forcedOpaque(ears), SkinModel.CLASSIC));

        BufferedImage glass = opaqueSkin();
        for (int x = 8; x < 16; x++) glass.setRGB(x, 10, 0x6E50C8FF);
        assertFalse(SeeThroughBase.alfalfa(glass));
        assertNotNull(SeeThroughBase.restored(glass, forcedOpaque(glass), SkinModel.CLASSIC));
    }

    @Test
    public void vertexColorTintScalesGreenAndBlueInEitherByteOrder() {
        // Little-endian: alpha, blue, green, red from the top byte down.
        assertEquals(0xFF3C3CFF, TessellatorRecord.tinted(0xFF6464FF, 0.6F));
        // Big-endian: red, green, blue, alpha.
        assertEquals(0x10787820, TessellatorRecord.tinted(0x10C8C820, 0.6F));
        assertEquals(0x80000020, TessellatorRecord.tinted(0x80FFFF20, 0.0F));
    }

    private static BufferedImage opaqueSkin() {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) skin.setRGB(x, y, 0xFF808080);
        return skin;
    }

    /** As the skin is uploaded: the base's alpha forced opaque, as vanilla's setAreaOpaque does. */
    private static BufferedImage forcedOpaque(BufferedImage original) {
        BufferedImage normalized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            int argb = original.getRGB(x, y);
            boolean base = y < 16 && x < 32 || y >= 16 && y < 32 || y >= 48 && x >= 16 && x < 48;
            normalized.setRGB(x, y, base ? argb | 0xFF000000 : argb);
        }
        return normalized;
    }

    /** Writes bytes as Ears' Alfalfa does: 7 bits per texel, alpha 255 minus the bits. */
    private static void alfalfa(BufferedImage image, byte[] bytes) {
        BigInteger data = new BigInteger(1, bytes);
        int piece = 0;
        for (int[] region : ALFALFA_REGIONS)
            for (int x = region[0]; x < region[2]; x++) for (int y = region[1]; y < region[3]; y++) {
                int bits = data.shiftRight(7 * piece++)
                    .and(BigInteger.valueOf(127))
                    .intValue();
                image.setRGB(x, y, 255 - bits << 24 | image.getRGB(x, y) & 0xFFFFFF);
            }
    }
}
