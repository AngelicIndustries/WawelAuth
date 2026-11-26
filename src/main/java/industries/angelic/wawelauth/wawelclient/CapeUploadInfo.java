package industries.angelic.wawelauth.wawelclient;

/**
 * Whether an account can get a cape from the cape creator, and what its server takes. Offline accounts keep their
 * cape as a local file, without limits. Wawel Auth servers tell their limits in their API metadata; older ones don't,
 * and get the limits Wawel Auth always had by default. Other servers, such as Drasl, tell nothing: the upload is tried
 * and their answer says if they refuse it.
 */
public final class CapeUploadInfo {

    private static final int DEFAULT_MAX_CAPE_WIDTH = 64;
    private static final int DEFAULT_MAX_CAPE_HEIGHT = 32;
    private static final int DEFAULT_MAX_FILE_SIZE_BYTES = 1_048_576;
    private static final int DEFAULT_MAX_FRAMES = 256;
    private static final int DEFAULT_MAX_ANIMATED_BYTES = 10_485_760;

    private final boolean offline;
    private final boolean allowed;
    private final String reason;
    private final boolean animatedCapes;
    private final int maxCapeWidth;
    private final int maxCapeHeight;
    private final int maxFileSizeBytes;
    private final int maxFrames;
    private final int maxAnimatedBytes;
    private final boolean apng;

    private CapeUploadInfo(boolean offline, boolean allowed, String reason, boolean animatedCapes, int maxCapeWidth,
        int maxCapeHeight, int maxFileSizeBytes, int maxFrames, int maxAnimatedBytes, boolean apng) {
        this.offline = offline;
        this.allowed = allowed;
        this.reason = reason;
        this.animatedCapes = animatedCapes;
        this.maxCapeWidth = maxCapeWidth;
        this.maxCapeHeight = maxCapeHeight;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxFrames = maxFrames;
        this.maxAnimatedBytes = maxAnimatedBytes;
        this.apng = apng;
    }

    public static CapeUploadInfo offline() {
        return new CapeUploadInfo(
            true,
            true,
            null,
            true,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            true);
    }

    /**
     * A server that isn't Wawel Auth's, with the authlib-injector cape upload. It gets a PNG, an animated PNG when the
     * cape is animated: servers that keep the file as sent, like Drasl, play it; others show its first frame.
     */
    public static CapeUploadInfo otherServer() {
        return new CapeUploadInfo(
            false,
            true,
            null,
            true,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            true);
    }

    /** No cape from the creator for this account; {@code reason} says why, as a translation key. */
    public static CapeUploadInfo unavailable(String reason) {
        return new CapeUploadInfo(false, false, reason, false, 0, 0, 0, 0, 0, false);
    }

    /** {@code apng}: the server keeps animated PNG capes animated; older ones re-encode them to their first frame. */
    public static CapeUploadInfo server(Boolean animatedCapes, Integer maxCapeWidth, Integer maxCapeHeight,
        Integer maxFileSizeBytes, Integer maxFrames, Integer maxAnimatedBytes, boolean apng) {
        return new CapeUploadInfo(
            false,
            true,
            null,
            animatedCapes != null ? animatedCapes : true,
            maxCapeWidth != null ? maxCapeWidth : DEFAULT_MAX_CAPE_WIDTH,
            maxCapeHeight != null ? maxCapeHeight : DEFAULT_MAX_CAPE_HEIGHT,
            maxFileSizeBytes != null ? maxFileSizeBytes : DEFAULT_MAX_FILE_SIZE_BYTES,
            maxFrames != null ? maxFrames : DEFAULT_MAX_FRAMES,
            maxAnimatedBytes != null ? maxAnimatedBytes : DEFAULT_MAX_ANIMATED_BYTES,
            apng);
    }

    public boolean isOffline() {
        return offline;
    }

    public boolean isAllowed() {
        return allowed;
    }

    /** A translation key, or null. */
    public String getReason() {
        return reason;
    }

    public boolean allowsAnimatedCapes() {
        return animatedCapes;
    }

    public int getMaxCapeWidth() {
        return maxCapeWidth;
    }

    public int getMaxCapeHeight() {
        return maxCapeHeight;
    }

    public int getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    /** Whether animated capes may be APNG here; offline accounts' always may. */
    public boolean takesApng() {
        return apng;
    }

    public int getMaxAnimatedBytes() {
        return maxAnimatedBytes;
    }
}
