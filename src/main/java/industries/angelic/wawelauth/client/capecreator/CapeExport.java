package industries.angelic.wawelauth.client.capecreator;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import industries.angelic.angellib.util.ApngUtil;

/**
 * The finished cape file: for a picture with several frames an animated GIF or APNG, for a still one a PNG. Wawel Auth
 * servers take animated capes only when they have at least two frames.
 */
public final class CapeExport {

    /** How an animated cape is written. APNG keeps every color and soft edges, and shows its first frame anywhere. */
    public enum Format {
        APNG,
        GIF
    }

    final byte[] data;
    final boolean animated;
    final Format format;
    final int width;
    final int height;
    final int frameCount;

    private CapeExport(byte[] data, boolean animated, Format format, int width, int height, int frameCount) {
        this.data = data;
        this.animated = animated;
        this.format = format;
        this.width = width;
        this.height = height;
        this.frameCount = frameCount;
    }

    /** "gif" for GIFs; APNGs are PNGs, which programs without APNG support show as their first frame. */
    public String extension() {
        return animated && format == Format.GIF ? "gif" : "png";
    }

    public byte[] getData() {
        return data;
    }

    public boolean isAnimated() {
        return animated;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrameCount() {
        return frameCount;
    }

    static CapeExport create(CapeSource source, BufferedImage background, CapeComposer.Options options,
        GifWriter.Progress progress) throws IOException {
        return create(source, background, options, progress, Format.GIF);
    }

    static CapeExport create(CapeSource source, BufferedImage background, CapeComposer.Options options,
        GifWriter.Progress progress, Format format) throws IOException {
        int width = CapeLayout.width(options.scale);
        int height = CapeLayout.height(options.scale);
        if (!source.animated()) {
            BufferedImage cape = CapeComposer.compose(source.frames[0], background, options);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(cape, "png", out)) {
                throw new IOException("This Java has no PNG writer.");
            }
            if (progress != null) progress.frameWritten(1, 1);
            return new CapeExport(out.toByteArray(), false, format, width, height, 1);
        }
        if (format == Format.APNG) {
            byte[] apng = ApngUtil.write(source.frameCount(), index -> {
                BufferedImage frame = CapeComposer.compose(source.frames[index], background, options);
                if (progress != null) progress.frameWritten(index + 1, source.frameCount());
                return frame;
            }, source.delaysMs, 0);
            return new CapeExport(apng, true, format, width, height, source.frameCount());
        }
        byte[] gif = GifWriter.write(new GifWriter.Frames() {

            @Override
            public int count() {
                return source.frameCount();
            }

            @Override
            public BufferedImage frame(int index) {
                return CapeComposer.compose(source.frames[index], background, options);
            }

            @Override
            public int delayMs(int index) {
                return source.delaysMs[index];
            }
        }, progress);
        return new CapeExport(gif, true, format, width, height, source.frameCount());
    }
}
