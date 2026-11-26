package industries.angelic.wawelauth.client.capecreator;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.angellib.util.GifUtil;
import industries.angelic.wawelauth.wawelcore.config.ServerConfig;
import industries.angelic.wawelauth.wawelserver.TextureService;

public class CapeComposerTest {

    private static final int RED = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;
    private static final int GREEN = 0xFF00FF00;

    @Test
    public void scalesRunFrom64x32To2048x1024() {
        Assert.assertEquals(64, CapeLayout.width(1));
        Assert.assertEquals(32, CapeLayout.height(1));
        Assert.assertEquals(2048, CapeLayout.width(6));
        Assert.assertEquals(1024, CapeLayout.height(6));
        Assert.assertEquals(2048, CapeLayout.width(9));
    }

    @Test
    public void pictureCoversTheCapeBackAndColorTheRest() {
        BufferedImage cape = CapeComposer
            .compose(solid(5, 8, BLUE), null, new CapeComposer.Options(2, 0xFF0000, false));
        Assert.assertEquals(128, cape.getWidth());
        Assert.assertEquals(64, cape.getHeight());
        // The cape's back at scale 2: x 2..21, y 2..33.
        Assert.assertEquals(BLUE, cape.getRGB(2, 2));
        Assert.assertEquals(BLUE, cape.getRGB(21, 33));
        // Its sides and inside are the color.
        Assert.assertEquals(RED, cape.getRGB(0, 2));
        Assert.assertEquals(RED, cape.getRGB(30, 10));
        // Without the elytra option, the elytra's outside is the color too.
        Assert.assertEquals(RED, cape.getRGB(72 + 2, 4 + 2));
        // Outside the cape and the elytra, the texture stays empty.
        Assert.assertEquals(0, cape.getRGB(100, 60) >>> 24);
    }

    @Test
    public void elytraOptionPutsThePictureOnTheWings() {
        BufferedImage cape = CapeComposer.compose(solid(4, 8, BLUE), null, new CapeComposer.Options(1, 0xFF0000, true));
        Assert.assertEquals(BLUE, cape.getRGB(36, 2));
        Assert.assertEquals(BLUE, cape.getRGB(45, 21));
        Assert.assertEquals(RED, cape.getRGB(35, 2));
    }

    @Test
    public void wideAndTallPicturesAreCroppedToTheMiddle() {
        // Left third blue, middle green, right third blue: the tall cape back only shows the green middle.
        BufferedImage wide = new BufferedImage(30, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 30; x++) {
                wide.setRGB(x, y, x >= 10 && x < 20 ? GREEN : BLUE);
            }
        }
        BufferedImage cape = CapeComposer.compose(wide, null, new CapeComposer.Options(1, 0, false));
        for (int x = 1; x < 11; x++) {
            Assert.assertEquals("x=" + x, GREEN, cape.getRGB(x, 8));
        }
    }

    @Test
    public void backgroundReplacesTheColor() {
        BufferedImage background = solid(128, 64, GREEN);
        BufferedImage cape = CapeComposer.compose(null, background, new CapeComposer.Options(1, 0xFF0000, false));
        Assert.assertEquals(GREEN, cape.getRGB(0, 2));
        Assert.assertEquals(GREEN, cape.getRGB(60, 30));
    }

    @Test
    public void capeShapesAreTheServersOnes() {
        Assert.assertTrue(CapeSource.isCapeShape(64, 32));
        Assert.assertTrue(CapeSource.isCapeShape(2048, 1024));
        Assert.assertTrue(CapeSource.isCapeShape(22, 17));
        Assert.assertTrue(CapeSource.isCapeShape(44, 34));
        Assert.assertFalse(CapeSource.isCapeShape(64, 64));
        Assert.assertFalse(CapeSource.isCapeShape(96, 48));
    }

    @Test
    public void animatedCapesRoundTripThroughTheGifReader() throws IOException {
        CapeSource source = new CapeSource(
            new BufferedImage[] { solid(10, 16, BLUE), solid(10, 16, GREEN), solid(10, 16, BLUE) },
            new int[] { 50, 120, 200 });
        CapeExport export = CapeExport.create(source, null, new CapeComposer.Options(1, 0xFF0000, true), null);
        Assert.assertTrue(export.animated);
        Assert.assertTrue(CapeSource.isGif(export.data));

        GifUtil.GifData gif = GifUtil.readGif(export.data);
        Assert.assertEquals(3, gif.getFrameCount());
        Assert.assertEquals(50, gif.getDelayMs(0));
        Assert.assertEquals(120, gif.getDelayMs(1));
        BufferedImage second = gif.getFrame(1);
        Assert.assertEquals(64, second.getWidth());
        Assert.assertEquals(32, second.getHeight());
        Assert.assertEquals(GREEN, second.getRGB(5, 8));
        Assert.assertEquals(GREEN, second.getRGB(40, 10));
        Assert.assertEquals(RED, second.getRGB(0, 8));
        Assert.assertEquals("transparent outside the cape", 0, second.getRGB(60, 30) >>> 24);
        // The third frame replaces the second instead of drawing over it.
        Assert.assertEquals(
            BLUE,
            gif.getFrame(2)
                .getRGB(5, 8));
    }

    @Test
    public void wawelAuthServersAcceptTheAnimatedCape() throws Exception {
        CapeSource source = new CapeSource(
            new BufferedImage[] { solid(10, 16, BLUE), solid(10, 16, GREEN), solid(10, 16, RED) },
            new int[] { 100, 100, 100 });
        CapeExport export = CapeExport.create(source, null, new CapeComposer.Options(1, 0x336699, true), null);
        Method validate = TextureService.class
            .getDeclaredMethod("validateGifCape", byte[].class, ServerConfig.Textures.class);
        validate.setAccessible(true);
        int[] info = (int[]) validate.invoke(null, export.data, new ServerConfig.Textures());
        Assert.assertArrayEquals(new int[] { 64, 32, 3 }, info);
    }

    @Test
    public void animatedCapesCanBeApngs() throws Exception {
        CapeSource source = new CapeSource(
            new BufferedImage[] { solid(10, 16, BLUE), solid(10, 16, GREEN), solid(10, 16, BLUE) },
            new int[] { 50, 120, 200 });
        CapeExport export = CapeExport
            .create(source, null, new CapeComposer.Options(2, 0xFF0000, true), null, CapeExport.Format.APNG);
        Assert.assertTrue(export.isAnimated());
        Assert.assertEquals("png", export.extension());
        Assert.assertTrue(industries.angelic.angellib.util.ApngUtil.isAnimated(export.getData()));

        GifUtil.GifData apng = industries.angelic.angellib.util.ApngUtil.read(export.getData());
        Assert.assertEquals(3, apng.getFrameCount());
        Assert.assertEquals(120, apng.getDelayMs(1));
        Assert.assertEquals(
            GREEN,
            apng.getFrame(1)
                .getRGB(10, 16));
        Assert.assertEquals(
            RED,
            apng.getFrame(1)
                .getRGB(0, 16));
        Assert.assertEquals(
            0,
            apng.getFrame(1)
                .getRGB(120, 60) >>> 24);
        Assert.assertEquals(
            BLUE,
            apng.getFrame(2)
                .getRGB(10, 16));

        // Without APNG support, the first frame shows as a plain PNG.
        BufferedImage still = ImageIO.read(new ByteArrayInputStream(export.getData()));
        Assert.assertEquals(BLUE, still.getRGB(10, 16));

        int[] info = TextureService.validateApngCape(export.getData(), new ServerConfig.Textures());
        Assert.assertArrayEquals(new int[] { 128, 64, 3 }, info);
    }

    @Test
    public void stillPicturesMakeAPngCape() throws IOException {
        CapeSource source = new CapeSource(new BufferedImage[] { solid(10, 16, BLUE) }, new int[] { 100 });
        CapeExport export = CapeExport.create(source, null, new CapeComposer.Options(3, 0x00FF00, false), null);
        Assert.assertFalse(export.animated);
        BufferedImage png = ImageIO.read(new ByteArrayInputStream(export.data));
        Assert.assertEquals(256, png.getWidth());
        Assert.assertEquals(128, png.getHeight());
        Assert.assertEquals(BLUE, png.getRGB(4, 4));
    }

    @Test
    public void fewColorsSurviveQuantizing() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] colors = { RED, BLUE, GREEN, 0xFF123456, 0xFFABCDEF };
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, x == 0 ? 0 : colors[(x + y) % colors.length]);
            }
        }
        BufferedImage indexed = PaletteQuantizer.quantize(image);
        for (int y = 0; y < 16; y++) {
            for (int x = 1; x < 16; x++) {
                Assert.assertEquals(image.getRGB(x, y), indexed.getRGB(x, y));
            }
            Assert.assertEquals(0, indexed.getRGB(0, y) >>> 24);
        }
    }

    @Test
    public void manyColorsStayClose() {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                image.setRGB(x, y, 0xFF000000 | x << 16 | y << 8 | (x + y) / 2);
            }
        }
        BufferedImage indexed = PaletteQuantizer.quantize(image);
        long error = 0;
        for (int y = 0; y < 256; y += 3) {
            for (int x = 0; x < 256; x += 3) {
                int a = image.getRGB(x, y);
                int b = indexed.getRGB(x, y);
                error += Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF)) + Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF))
                    + Math.abs((a & 0xFF) - (b & 0xFF));
            }
        }
        double perChannel = error / (86.0 * 86.0 * 3.0);
        Assert.assertTrue("average error " + perChannel, perChannel < 12);
    }

    private static BufferedImage solid(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }
}
