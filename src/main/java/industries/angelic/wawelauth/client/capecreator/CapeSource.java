package industries.angelic.wawelauth.client.capecreator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import javax.imageio.ImageIO;

import industries.angelic.angellib.util.GifUtil;

/** The picture a cape is made from: the frames of a GIF with their delays, or a single still image. */
public final class CapeSource {

    /** Used when a GIF frame has no delay, as browsers do. */
    static final int DEFAULT_DELAY_MS = 100;
    private static final int MIN_DELAY_MS = 20;

    final BufferedImage[] frames;
    final int[] delaysMs;

    CapeSource(BufferedImage[] frames, int[] delaysMs) {
        if (frames.length == 0 || frames.length != delaysMs.length) {
            throw new IllegalArgumentException("frames and delays don't match");
        }
        this.frames = frames;
        this.delaysMs = delaysMs;
    }

    public int frameCount() {
        return frames.length;
    }

    public boolean animated() {
        return frames.length > 1;
    }

    /** Reads a GIF, PNG or JPEG. */
    static CapeSource load(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        if (isGif(data)) {
            GifUtil.GifData gif = GifUtil.readGif(data);
            if (gif == null || gif.getFrameCount() == 0) {
                throw new IOException("The GIF has no frames.");
            }
            BufferedImage[] frames = new BufferedImage[gif.getFrameCount()];
            int[] delays = new int[frames.length];
            for (int i = 0; i < frames.length; i++) {
                frames[i] = gif.getFrame(i);
                int delay = gif.getDelayMs(i);
                delays[i] = delay <= 0 ? DEFAULT_DELAY_MS : Math.max(MIN_DELAY_MS, delay);
            }
            return new CapeSource(frames, delays);
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Not an image Minecraft can read: " + file.getName());
        }
        return new CapeSource(new BufferedImage[] { image }, new int[] { DEFAULT_DELAY_MS });
    }

    /** A cape file to draw on instead of the solid color: the 64x32 layout at any size, or an old 22x17 cape. */
    static BufferedImage loadBackground(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Not an image Minecraft can read: " + file.getName());
        }
        if (!isCapeShape(image.getWidth(), image.getHeight())) {
            throw new IOException(
                "Not a cape file: " + image.getWidth()
                    + "x"
                    + image.getHeight()
                    + " is neither a multiple of 64x32 nor of 22x17.");
        }
        return image;
    }

    static boolean isCapeShape(int width, int height) {
        return width > 0 && height > 0
            && (width * CapeLayout.BASE_HEIGHT == height * CapeLayout.BASE_WIDTH && width % CapeLayout.BASE_WIDTH == 0
                || width * CapeLayout.LEGACY_HEIGHT == height * CapeLayout.LEGACY_WIDTH
                    && width % CapeLayout.LEGACY_WIDTH == 0);
    }

    static boolean isGif(byte[] data) {
        return data.length >= 6 && data[0] == 'G'
            && data[1] == 'I'
            && data[2] == 'F'
            && data[3] == '8'
            && (data[4] == '7' || data[4] == '9')
            && data[5] == 'a';
    }

    static boolean isGifName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT)
            .endsWith(".gif");
    }
}
