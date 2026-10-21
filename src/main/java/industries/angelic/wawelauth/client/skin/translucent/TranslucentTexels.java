package industries.angelic.wawelauth.client.skin.translucent;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.skin.LayerSegment;
import industries.angelic.wawelauth.api.skin.PlayerSkin;

/** Which of a skin's layer boxes hold semi-transparent texels: alpha above none and below full. */
@SideOnly(Side.CLIENT)
final class TranslucentTexels {

    private static final int MAX_PROFILES = 512;
    private static final Map<UUID, Entry> PROFILES = new HashMap<>();

    private TranslucentTexels() {}

    /** Whether the segment's box, on the skin as it is on the GPU, has any semi-transparent texel. */
    static boolean present(PlayerSkin skin, LayerSegment segment) {
        Entry entry = PROFILES.get(skin.profileId());
        if (entry == null || entry.revision != skin.revision()) {
            if (PROFILES.size() >= MAX_PROFILES) PROFILES.clear();
            entry = new Entry(skin.revision(), skin.normalizedImage());
            PROFILES.put(skin.profileId(), entry);
        }
        if (entry.image == null) return false;
        long key = ((long) segment.u() << 40) | ((long) segment.v() << 30)
            | ((long) segment.width() << 20)
            | ((long) segment.height() << 10)
            | segment.depth();
        for (int i = 0; i < entry.count; i++) if (entry.keys[i] == key) return entry.present[i];
        boolean found = present(
            entry.image,
            segment.u(),
            segment.v(),
            segment.width(),
            segment.height(),
            segment.depth());
        if (entry.count < entry.keys.length) {
            entry.keys[entry.count] = key;
            entry.present[entry.count++] = found;
        }
        return found;
    }

    static void clear() {
        PROFILES.clear();
    }

    /**
     * Whether a box's faces in a vanilla UV layout, on a 64-wide skin or a square HD one, hold a semi-transparent
     * texel. Only the faces count: the unused corners of the layout can carry other mods' data.
     */
    static boolean present(BufferedImage image, int u, int v, int width, int height, int depth) {
        int texel = image.getWidth() / 64;
        if (texel < 1 || image.getHeight() < 64 * texel) return false;
        // Top and bottom, then the four sides.
        return region(image, texel, u + depth, v, 2 * width, depth)
            || region(image, texel, u, v + depth, 2 * (width + depth), height);
    }

    private static boolean region(BufferedImage image, int texel, int u, int v, int width, int height) {
        for (int y = v * texel; y < (v + height) * texel; y++) for (int x = u * texel; x < (u + width) * texel; x++) {
            if (x >= image.getWidth() || y >= image.getHeight()) continue;
            int alpha = image.getRGB(x, y) >>> 24;
            if (alpha > 0 && alpha < 255) return true;
        }
        return false;
    }

    private static final class Entry {

        final long revision;
        final BufferedImage image;
        /** The boxes asked about so far: a skin has a dozen at most, slices sharing their box. */
        final long[] keys = new long[32];
        final boolean[] present = new boolean[32];
        int count;

        Entry(long revision, BufferedImage image) {
            this.revision = revision;
            this.image = image;
        }
    }
}
