package industries.angelic.wawelauth.client.render;

import java.awt.image.BufferedImage;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.compat.ears.EarsCompat;

/**
 * Whether a mod that draws 64x64 player skins is loaded: Salamander, SimpleSkinBackport or Ears. Wawel Auth only
 * supplies skins; without such a mod, players are drawn by vanilla's 64x32 model, so skin textures are uploaded in
 * the layout it reads.
 */
@SideOnly(Side.CLIENT)
public final class ModernRenderers {

    private static volatile Boolean present;

    private ModernRenderers() {}

    public static boolean present() {
        Boolean known = present;
        if (known == null) {
            known = Loader.isModLoaded("salamander") || Loader.isModLoaded("simpleskinbackport")
                || EarsCompat.isRendererActive();
            present = known;
        }
        return known;
    }

    /**
     * A parsed 64x64 (or square HD) skin as it is uploaded: unchanged when a modern renderer is loaded, otherwise its
     * top half, which holds everything vanilla's model draws, with vanilla's alpha rules.
     */
    public static BufferedImage uploaded(BufferedImage image) {
        if (image == null || present() || image.getHeight() != image.getWidth()) return image;
        return legacy(image);
    }

    /** Vanilla's view of a skin: the top half, base opaque, the hat cleared if it has no see-through pixel at all. */
    static BufferedImage legacy(BufferedImage image) {
        int width = image.getWidth();
        int height = Math.min(image.getHeight(), width / 2);
        int unit = width / 64;
        int[] data = image.getRGB(0, 0, width, height, null, 0, width);
        opaque(data, width, 0, 0, 32 * unit, 16 * unit);
        transparent(data, width, 32 * unit, 0, 64 * unit, 32 * unit);
        opaque(data, width, 0, 16 * unit, 64 * unit, 32 * unit);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width, height, data, 0, width);
        return out;
    }

    private static void opaque(int[] data, int width, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) data[y * width + x] |= 0xFF000000;
        }
    }

    private static void transparent(int[] data, int width, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if ((data[y * width + x] >>> 24) < 128) return;
            }
        }
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) data[y * width + x] &= 0x00FFFFFF;
        }
    }
}
