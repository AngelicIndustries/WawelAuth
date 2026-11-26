package industries.angelic.wawelauth.client.capecreator;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import industries.angelic.angellib.util.GifUtil;

/**
 * Puts a picture on a cape texture, as minecraftcapes.net's animated cape creator does: the picture fills the cape's
 * back, and with {@link Options#onElytra} the outside of the elytra wings too. The rest of the cape and the elytra is
 * the chosen color, or a cape file given as background. The picture keeps its proportions and is cropped to fit.
 */
final class CapeComposer {

    private CapeComposer() {}

    static final class Options {

        final int scale;
        /** RGB of the cape's sides and inside, used without a background. */
        final int color;
        final boolean onElytra;

        Options(int scale, int color, boolean onElytra) {
            this.scale = CapeLayout.clampScale(scale);
            this.color = color & 0xFFFFFF;
            this.onElytra = onElytra;
        }
    }

    /** One frame of the cape at the options' scale. */
    static BufferedImage compose(BufferedImage picture, BufferedImage background, Options options) {
        return compose(picture, background, options.color, options.onElytra, options.scale);
    }

    /** One frame at another scale than the options', for a smaller preview. */
    static BufferedImage compose(BufferedImage picture, BufferedImage background, int color, boolean onElytra,
        int scale) {
        int factor = CapeLayout.factor(scale);
        BufferedImage cape = new BufferedImage(
            CapeLayout.width(scale),
            CapeLayout.height(scale),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cape.createGraphics();
        try {
            if (background != null) {
                drawBackground(g, background, factor);
            } else {
                int argb = 0xFF000000 | color & 0xFFFFFF;
                fill(cape, CapeLayout.CAPE, factor, argb);
                fill(cape, CapeLayout.ELYTRA, factor, argb);
            }
            if (picture != null) {
                drawCovering(g, picture, CapeLayout.CAPE_FRONT, factor);
                if (onElytra) {
                    drawCovering(g, picture, CapeLayout.ELYTRA_FRONT, factor);
                }
            }
        } finally {
            g.dispose();
        }
        return cape;
    }

    /** A cape file stretched to the texture, keeping its pixels square: old 22x17 capes cover only the cape box. */
    private static void drawBackground(Graphics2D g, BufferedImage background, int factor) {
        int width = background.getWidth();
        int height = background.getHeight();
        boolean legacy = width * CapeLayout.LEGACY_HEIGHT == height * CapeLayout.LEGACY_WIDTH
            && width % CapeLayout.LEGACY_WIDTH == 0;
        int targetWidth = (legacy ? CapeLayout.LEGACY_WIDTH : CapeLayout.BASE_WIDTH) * factor;
        int targetHeight = (legacy ? CapeLayout.LEGACY_HEIGHT : CapeLayout.BASE_HEIGHT) * factor;
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            targetWidth >= width ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(background, 0, 0, targetWidth, targetHeight, null);
    }

    private static void fill(BufferedImage cape, CapeLayout.Region region, int factor, int argb) {
        int x0 = region.x * factor;
        int y0 = region.y * factor;
        for (int y = y0; y < y0 + region.height * factor; y++) {
            for (int x = x0; x < x0 + region.width * factor; x++) {
                cape.setRGB(x, y, argb);
            }
        }
    }

    /** The picture over the whole region, its middle part when its proportions differ from the region's. */
    private static void drawCovering(Graphics2D g, BufferedImage picture, CapeLayout.Region region, int factor) {
        int targetWidth = region.width * factor;
        int targetHeight = region.height * factor;
        int sourceWidth = picture.getWidth();
        int sourceHeight = picture.getHeight();
        int cropWidth = sourceWidth;
        int cropHeight = sourceHeight;
        // Compare targetWidth / targetHeight with sourceWidth / sourceHeight without rounding.
        if ((long) sourceWidth * targetHeight > (long) sourceHeight * targetWidth) {
            cropWidth = Math.max(1, (int) Math.round((double) sourceHeight * targetWidth / targetHeight));
        } else {
            cropHeight = Math.max(1, (int) Math.round((double) sourceWidth * targetHeight / targetWidth));
        }
        BufferedImage cropped = picture
            .getSubimage((sourceWidth - cropWidth) / 2, (sourceHeight - cropHeight) / 2, cropWidth, cropHeight);
        BufferedImage scaled = scale(cropped, targetWidth, targetHeight);
        g.drawImage(scaled, region.x * factor, region.y * factor, null);
    }

    /** Pixel art stays sharp when enlarged; photos and big GIFs are shrunk smoothly. */
    private static BufferedImage scale(BufferedImage image, int width, int height) {
        if (image.getWidth() == width && image.getHeight() == height) {
            return image;
        }
        if (width >= image.getWidth() && height >= image.getHeight()) {
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            try {
                g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(image, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }
            return out;
        }
        return GifUtil.scaleFrame(toArgb(image), width, height);
    }

    private static BufferedImage toArgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return image;
        }
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}
