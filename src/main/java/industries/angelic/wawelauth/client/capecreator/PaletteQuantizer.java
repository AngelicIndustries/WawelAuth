package industries.angelic.wawelauth.client.capecreator;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Reduces an image to the 256 colors a GIF frame can hold, by median cut: one entry stays for transparency, the
 * others are averages of the image's colors. Images with 255 colors or fewer keep them, unless two of them differ in
 * the lowest three bits of every channel.
 */
final class PaletteQuantizer {

    static final int TRANSPARENT_INDEX = 255;
    private static final int MAX_COLORS = 255;
    private static final int KEY_COUNT = 1 << 15;

    private PaletteQuantizer() {}

    /** An indexed copy of the image; pixels less than half opaque become transparent. */
    static BufferedImage quantize(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);

        long[] counts = new long[KEY_COUNT];
        long[] sumR = new long[KEY_COUNT];
        long[] sumG = new long[KEY_COUNT];
        long[] sumB = new long[KEY_COUNT];
        for (int pixel : argb) {
            if (pixel >>> 24 < 0x80) continue;
            int key = key(pixel);
            counts[key]++;
            sumR[key] += pixel >> 16 & 0xFF;
            sumG[key] += pixel >> 8 & 0xFF;
            sumB[key] += pixel & 0xFF;
        }

        List<Integer> keys = new ArrayList<>();
        for (int key = 0; key < KEY_COUNT; key++) {
            if (counts[key] > 0) keys.add(key);
        }

        List<List<Integer>> boxes = new ArrayList<>();
        if (!keys.isEmpty()) boxes.add(keys);
        while (boxes.size() < MAX_COLORS) {
            int widest = widestBox(boxes, counts);
            if (widest < 0) break;
            List<Integer> box = boxes.remove(widest);
            List<Integer>[] halves = split(box, counts);
            boxes.add(halves[0]);
            boxes.add(halves[1]);
        }

        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        int[] indexOfKey = new int[KEY_COUNT];
        Arrays.fill(indexOfKey, TRANSPARENT_INDEX);
        for (int i = 0; i < boxes.size(); i++) {
            long n = 0, rs = 0, gs = 0, bs = 0;
            for (int key : boxes.get(i)) {
                n += counts[key];
                rs += sumR[key];
                gs += sumG[key];
                bs += sumB[key];
                indexOfKey[key] = i;
            }
            r[i] = (byte) Math.round((double) rs / n);
            g[i] = (byte) Math.round((double) gs / n);
            b[i] = (byte) Math.round((double) bs / n);
        }

        IndexColorModel model = new IndexColorModel(8, 256, r, g, b, TRANSPARENT_INDEX);
        BufferedImage indexed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, model);
        WritableRaster raster = indexed.getRaster();
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = argb[y * width + x];
                row[x] = pixel >>> 24 < 0x80 ? TRANSPARENT_INDEX : indexOfKey[key(pixel)];
            }
            raster.setPixels(0, y, width, 1, row);
        }
        return indexed;
    }

    /** Whether any pixel of the image is transparent once quantized. */
    static boolean hasTransparency(BufferedImage image) {
        int width = image.getWidth();
        int[] argb = image.getRGB(0, 0, width, image.getHeight(), null, 0, width);
        for (int pixel : argb) {
            if (pixel >>> 24 < 0x80) return true;
        }
        return false;
    }

    private static int key(int pixel) {
        return (pixel >> 19 & 0x1F) << 10 | (pixel >> 11 & 0x1F) << 5 | pixel >> 3 & 0x1F;
    }

    private static int channel(int key, int channel) {
        return key >> (10 - channel * 5) & 0x1F;
    }

    /** The box to split next: the one whose widest channel spans the most, weighted by its pixels. */
    private static int widestBox(List<List<Integer>> boxes, long[] counts) {
        int best = -1;
        double bestScore = 0;
        for (int i = 0; i < boxes.size(); i++) {
            List<Integer> box = boxes.get(i);
            if (box.size() < 2) continue;
            long pixels = 0;
            for (int key : box) pixels += counts[key];
            double score = (double) range(box, longestChannel(box)) * Math.sqrt(pixels);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer>[] split(List<Integer> box, long[] counts) {
        int channel = longestChannel(box);
        List<Integer> sorted = new ArrayList<>(box);
        sorted.sort(Comparator.comparingInt(key -> channel(key, channel)));
        long total = 0;
        for (int key : sorted) total += counts[key];
        long half = 0;
        int cut = 1;
        for (int i = 0; i < sorted.size() - 1; i++) {
            half += counts[sorted.get(i)];
            cut = i + 1;
            if (half * 2 >= total) break;
        }
        return new List[] { new ArrayList<>(sorted.subList(0, cut)),
            new ArrayList<>(sorted.subList(cut, sorted.size())) };
    }

    private static int longestChannel(List<Integer> box) {
        int best = 0;
        int bestRange = -1;
        for (int channel = 0; channel < 3; channel++) {
            int range = range(box, channel);
            if (range > bestRange) {
                bestRange = range;
                best = channel;
            }
        }
        return best;
    }

    private static int range(List<Integer> box, int channel) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int key : box) {
            int value = channel(key, channel);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return max - min;
    }
}
