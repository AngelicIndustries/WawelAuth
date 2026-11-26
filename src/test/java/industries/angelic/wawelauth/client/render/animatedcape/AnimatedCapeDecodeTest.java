package industries.angelic.wawelauth.client.render.animatedcape;

import java.awt.image.BufferedImage;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.angellib.util.ApngUtil;

public class AnimatedCapeDecodeTest {

    @Test
    public void bigApngCapesAreDecodedSmaller() throws Exception {
        BufferedImage[] frames = new BufferedImage[3];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_ARGB);
            frames[i].setRGB(i * 100, 10, 0xFFFF0000);
        }
        byte[] apng = ApngUtil.write(frames, new int[] { 60, 60, 60 }, 0);

        AnimatedCapeTexture.DecodedGif decoded = AnimatedCapeTexture.decodeAnimated(apng);
        Assert.assertNotNull(decoded);
        Assert.assertEquals(3, decoded.frameCount);
        Assert.assertEquals(60, decoded.frameDelayMs);
        Assert.assertEquals(512, decoded.frameWidth);
        Assert.assertEquals(256, decoded.frameHeight);
        Assert.assertEquals(512 * 3 * 256, decoded.spriteSheetPixels.length);
    }

    @Test
    public void stillPngsAreNotAnimated() throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", out);
        Assert.assertNull(AnimatedCapeTexture.decodeAnimated(out.toByteArray()));
    }
}
