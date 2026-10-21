package industries.angelic.wawelauth.client.skin.translucent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.Test;

public class TranslucentTexelsTest {

    @Test
    public void onlySemiTransparentFaceTexelsCount() {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        // The hat's faces are opaque and empty; its unused corner carries data, as Ears' does.
        fill(skin, 32, 8, 32, 8, 0xFF336699);
        skin.setRGB(32, 0, 0x803F23D8);
        assertFalse(TranslucentTexels.present(skin, 32, 0, 8, 8, 8));
        // A half-transparent visor on the front of the hat.
        skin.setRGB(42, 11, 0x80FFFFFF);
        assertTrue(TranslucentTexels.present(skin, 32, 0, 8, 8, 8));
        // The jacket elsewhere is unaffected.
        assertFalse(TranslucentTexels.present(skin, 16, 32, 8, 12, 4));
    }

    @Test
    public void hdSkinsScaleTheirLayout() {
        BufferedImage skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        assertFalse(TranslucentTexels.present(skin, 40, 32, 4, 12, 4));
        // The right sleeve's front face, at twice the resolution.
        skin.setRGB(2 * 45, 2 * 40, 0x40000000);
        assertTrue(TranslucentTexels.present(skin, 40, 32, 4, 12, 4));
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, int argb) {
        for (int j = y; j < y + height; j++) for (int i = x; i < x + width; i++) image.setRGB(i, j, argb);
    }
}
