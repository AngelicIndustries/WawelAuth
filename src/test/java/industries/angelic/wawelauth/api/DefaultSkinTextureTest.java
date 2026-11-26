package industries.angelic.wawelauth.api;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

public class DefaultSkinTextureTest {

    /** Every box of a 64x64 skin with wide arms, base and outer layers: u, v, width, height, depth. */
    private static final int[][] BOXES = { { 0, 0, 8, 8, 8 }, { 32, 0, 8, 8, 8 }, { 16, 16, 8, 12, 4 },
        { 16, 32, 8, 12, 4 }, { 40, 16, 4, 12, 4 }, { 40, 32, 4, 12, 4 }, { 32, 48, 4, 12, 4 }, { 48, 48, 4, 12, 4 },
        { 0, 16, 4, 12, 4 }, { 0, 32, 4, 12, 4 }, { 16, 48, 4, 12, 4 }, { 0, 48, 4, 12, 4 } };

    /**
     * The default Steve leaves the texels no part shows transparent, as vanilla's does: packs such as Just Expressions
     * and Fresh Moves read some of them as eyes and brows, which opaque black there draws as black marks on the face.
     */
    @Test
    public void defaultSteveIsTransparentWhereNoPartShows() throws Exception {
        BufferedImage skin;
        try (InputStream input = getClass().getResourceAsStream("/assets/wawelauth/textures/steve_64.png")) {
            skin = ImageIO.read(input);
        }
        Assert.assertEquals(64, skin.getWidth());
        Assert.assertEquals(64, skin.getHeight());
        boolean[][] shown = new boolean[64][64];
        for (int[] box : BOXES) {
            int u = box[0], v = box[1], width = box[2], height = box[3], depth = box[4];
            mark(shown, u + depth, v, width, depth);
            mark(shown, u + depth + width, v, width, depth);
            mark(shown, u, v + depth, 2 * depth + 2 * width, height);
        }
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
            if (!shown[y][x]) Assert.assertEquals(x + "," + y, 0, skin.getRGB(x, y) >>> 24);
        // The base layer stays opaque where it shows.
        Assert.assertEquals(0xff, skin.getRGB(10, 12) >>> 24);
        Assert.assertEquals(0xff, skin.getRGB(20, 25) >>> 24);
    }

    private static void mark(boolean[][] shown, int x0, int y0, int width, int height) {
        for (int y = y0; y < y0 + height; y++) for (int x = x0; x < x0 + width; x++) shown[y][x] = true;
    }
}
