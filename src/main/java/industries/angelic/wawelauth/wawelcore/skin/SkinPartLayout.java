package industries.angelic.wawelauth.wawelcore.skin;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import industries.angelic.wawelauth.api.skin.SkinPart;

/**
 * Bit layouts of the skin part flags in player DataWatcher slot 16, one per server generation.
 * Bit positions are indexed by {@link SkinPart} ordinal, -1 means the layout does not carry the part.
 */
public enum SkinPartLayout {

    /** Server without WA: only vanilla's cape bit carries data. */
    VANILLA(1, -1, -1, -1, -1, -1, -1),
    /** WA 1.0.1 to 1.0.4. */
    V0(1, 2, 3, 4, 5, 6, 7),
    /** WA 1.0.5 and 1.0.6. */
    V1(0, 1, 2, 3, 4, 5, 6),
    /** Current protocol, see {@link SkinPartFlags}. The cape entry is the wearer's bit. */
    V2(0, 7, 2, 3, 4, 5, 6);

    private static final Pattern RELEASE_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int[] bits;

    SkinPartLayout(int... bits) {
        this.bits = bits;
    }

    public int bit(SkinPart part) {
        return this.bits[part.ordinal()];
    }

    public boolean isHidden(int flags, SkinPart part) {
        if (this == V2 && part == SkinPart.CAPE) {
            return (flags & (SkinPartFlags.WEARER_CAPE_MASK | SkinPartFlags.CAPE_MASK)) != 0;
        }
        int bit = bit(part);
        return bit >= 0 && (flags & (1 << bit)) != 0;
    }

    /** Encodes all hidden parts the way a server of this generation stores them. */
    public int encode(Predicate<SkinPart> hidden) {
        int flags = 0;
        for (SkinPart part : SkinPart.VALUES) {
            int bit = bit(part);
            if (bit >= 0 && hidden.test(part)) {
                flags |= 1 << bit;
            }
        }
        return flags & 0xFF;
    }

    /**
     * Layout of an older WA server that did not announce a protocol, from its mod version.
     * Unparseable versions (snapshots) are assumed to be recent.
     */
    public static SkinPartLayout forLegacyVersion(String version) {
        int[] release = parseRelease(version);
        if (release != null && compare(release, 1, 0, 5) < 0) {
            return V0;
        }
        return V1;
    }

    /**
     * Whether an older WA server of this version is known to accept the legacy layer packet.
     * Snapshots are not, since early ones predate it and an unknown packet gets the client kicked.
     */
    public static boolean acceptsLegacyPacket(String version) {
        int[] release = parseRelease(version);
        return release != null && compare(release, 1, 0, 1) >= 0;
    }

    private static int[] parseRelease(String version) {
        if (version == null) {
            return null;
        }
        Matcher matcher = RELEASE_VERSION.matcher(version.trim());
        if (!matcher.find()) {
            return null;
        }
        return new int[] { Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)) };
    }

    private static int compare(int[] release, int major, int minor, int patch) {
        if (release[0] != major) return Integer.compare(release[0], major);
        if (release[1] != minor) return Integer.compare(release[1], minor);
        return Integer.compare(release[2], patch);
    }
}
