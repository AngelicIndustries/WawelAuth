package industries.angelic.wawelauth.client.capecreator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The cape creator's choices: the picture, an optional background cape, the color, the scale and whether the elytra
 * shows the picture too. Change it on the client thread; {@link #snapshot()} hands a frozen copy to a worker thread,
 * which makes the preview or the finished file from it.
 */
public final class CapeCreator {

    public static final int MIN_SCALE = CapeLayout.MIN_SCALE;
    public static final int MAX_SCALE = CapeLayout.MAX_SCALE;
    /** minecraftcapes.net's creator starts on red too. */
    public static final int DEFAULT_COLOR = 0xFF0000;
    /** The preview stops at 256x128: enough to see the detail, without holding hundreds of large frames. */
    private static final int PREVIEW_MAX_SCALE = 3;

    private CapeSource source;
    private String imageName;
    private BufferedImage background;
    private String backgroundName;
    private int scale = MIN_SCALE;
    private int color = DEFAULT_COLOR;
    private boolean onElytra = true;

    /** Reads a picture on a worker thread, for {@link #setImage}. */
    public static CapeSource loadImage(File file) throws IOException {
        return CapeSource.load(file);
    }

    /** Reads a background cape on a worker thread, for {@link #setBackground}. */
    public static BufferedImage loadBackground(File file) throws IOException {
        return CapeSource.loadBackground(file);
    }

    /** "64 / 32" up to "2048 / 1024", as minecraftcapes.net shows its scale. */
    public static String sizeLabel(int scale) {
        return CapeLayout.width(scale) + " / " + CapeLayout.height(scale);
    }

    public static int width(int scale) {
        return CapeLayout.width(scale);
    }

    public static int height(int scale) {
        return CapeLayout.height(scale);
    }

    public void setImage(CapeSource source, String name) {
        this.source = source;
        this.imageName = name;
    }

    public void setBackground(BufferedImage background, String name) {
        this.background = background;
        this.backgroundName = name;
    }

    public void clearBackground() {
        setBackground(null, null);
    }

    public boolean hasImage() {
        return source != null;
    }

    public String getImageName() {
        return imageName;
    }

    public String getBackgroundName() {
        return backgroundName;
    }

    /** Frames of the picture; 0 without one. */
    public int getFrameCount() {
        return source != null ? source.frameCount() : 0;
    }

    public boolean isAnimated() {
        return source != null && source.animated();
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = CapeLayout.clampScale(scale);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public boolean isOnElytra() {
        return onElytra;
    }

    public void setOnElytra(boolean onElytra) {
        this.onElytra = onElytra;
    }

    public Snapshot snapshot() {
        return new Snapshot(source, background, new CapeComposer.Options(scale, color, onElytra));
    }

    /** The choices at one moment, safe to use on another thread. */
    public static final class Snapshot {

        private final CapeSource source;
        private final BufferedImage background;
        private final CapeComposer.Options options;

        private Snapshot(CapeSource source, BufferedImage background, CapeComposer.Options options) {
            this.source = source;
            this.background = background;
            this.options = options;
        }

        public boolean hasImage() {
            return source != null;
        }

        /** Every frame at up to 256x128, as pixels for {@link CapePreviewTexture}. */
        public PreviewFrames preview() {
            int previewScale = Math.min(options.scale, PREVIEW_MAX_SCALE);
            int width = CapeLayout.width(previewScale);
            int height = CapeLayout.height(previewScale);
            int count = source != null ? source.frameCount() : 1;
            int[][] frames = new int[count][];
            int[] delays = new int[count];
            for (int i = 0; i < count; i++) {
                BufferedImage frame = CapeComposer.compose(
                    source != null ? source.frames[i] : null,
                    background,
                    options.color,
                    options.onElytra,
                    previewScale);
                frames[i] = frame.getRGB(0, 0, width, height, null, 0, width);
                delays[i] = source != null ? source.delaysMs[i] : CapeSource.DEFAULT_DELAY_MS;
            }
            return new PreviewFrames(width, height, frames, delays);
        }

        /** The finished cape at full size: animated in the given format with several frames, a PNG with one. */
        public CapeExport export(GifWriter.Progress progress, CapeExport.Format format) throws IOException {
            if (source == null) {
                throw new IOException("No picture chosen.");
            }
            return CapeExport.create(source, background, options, progress, format);
        }
    }

    /** A preview's frames as ARGB pixels, each {@code width} x {@code height}. */
    public static final class PreviewFrames {

        final int width;
        final int height;
        final int[][] pixels;
        final int[] delaysMs;

        PreviewFrames(int width, int height, int[][] pixels, int[] delaysMs) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
            this.delaysMs = delaysMs;
        }
    }
}
