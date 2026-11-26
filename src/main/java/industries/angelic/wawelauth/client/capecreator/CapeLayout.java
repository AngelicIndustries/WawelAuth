package industries.angelic.wawelauth.client.capecreator;

/**
 * Where the cape and the elytra are on a cape texture, in pixels of the 64x32 layout. Larger textures scale every
 * region by the same factor.
 */
final class CapeLayout {

    static final int BASE_WIDTH = 64;
    static final int BASE_HEIGHT = 32;
    /** Scale 1 is 64x32, scale 6 is 2048x1024. */
    static final int MIN_SCALE = 1;
    static final int MAX_SCALE = 6;

    /** All six faces of the cape box. */
    static final Region CAPE = new Region(0, 0, 22, 17);
    /** The face seen from behind the player. */
    static final Region CAPE_FRONT = new Region(1, 1, 10, 16);
    /** All faces of both elytra wings, which share this part of the texture. */
    static final Region ELYTRA = new Region(22, 0, 24, 22);
    /** The wings' outer face, seen from behind the player. */
    static final Region ELYTRA_FRONT = new Region(36, 2, 10, 20);

    /** Old capes that only hold the cape box, without room for an elytra. */
    static final int LEGACY_WIDTH = 22;
    static final int LEGACY_HEIGHT = 17;

    private CapeLayout() {}

    /** How many times larger than 64x32 a scale is: 1, 2, 4, ... 32. */
    static int factor(int scale) {
        return 1 << (clampScale(scale) - 1);
    }

    static int clampScale(int scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    static int width(int scale) {
        return BASE_WIDTH * factor(scale);
    }

    static int height(int scale) {
        return BASE_HEIGHT * factor(scale);
    }

    static final class Region {

        final int x;
        final int y;
        final int width;
        final int height;

        Region(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
