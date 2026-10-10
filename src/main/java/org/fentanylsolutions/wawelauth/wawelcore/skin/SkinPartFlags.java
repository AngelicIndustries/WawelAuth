package org.fentanylsolutions.wawelauth.wawelcore.skin;

import java.util.function.Predicate;

import org.fentanylsolutions.wawelauth.api.skin.SkinPart;

/**
 * Protocol v2 of the skin part flags kept in player DataWatcher slot 16.
 * <p>
 * The slot stays a vanilla byte. Every bit means "hidden", so 0 shows everything and missing data is harmless.
 * Vanilla only reads bit 1 (hide cape). In v2 that bit is the composite of the wearer's cape choice (bit 0) and
 * anything else hiding the cape, such as an elytra. The non-cape parts use bits 2 to 7.
 */
public final class SkinPartFlags {

    public static final int PROTOCOL_VERSION = 2;

    /** Raw custom payload channel a v2 server uses to announce its protocol. Unknown to older clients. */
    public static final String CHANNEL = "WAUTH|SKIN";

    public static final int DATA_WATCHER_ID = 16;

    public static final int WEARER_CAPE_BIT = 0;
    public static final int CAPE_BIT = 1;

    public static final int WEARER_CAPE_MASK = 1 << WEARER_CAPE_BIT;
    public static final int CAPE_MASK = 1 << CAPE_BIT;

    /** Bits owned by the non-cape parts. */
    public static final int PART_MASK = 0xFC;

    private SkinPartFlags() {}

    /** Encodes the hidden non-cape parts into v2 bit positions. */
    public static int encodeParts(Predicate<SkinPart> hidden) {
        int flags = 0;
        for (SkinPart part : SkinPart.VALUES) {
            if (part != SkinPart.CAPE && hidden.test(part)) {
                flags |= 1 << SkinPartLayout.V2.bit(part);
            }
        }
        return flags;
    }

    /** Replaces the non-cape part bits, keeping both cape bits. */
    public static int mergeParts(int flags, int parts) {
        return ((flags & ~PART_MASK) | (parts & PART_MASK)) & 0xFF;
    }

    public static int withWearerCape(int flags, boolean hidden) {
        return (hidden ? flags | WEARER_CAPE_MASK : flags & ~WEARER_CAPE_MASK) & 0xFF;
    }

    public static int withPart(int flags, SkinPart part, boolean hidden) {
        if (part == SkinPart.CAPE) {
            return withWearerCape(flags, hidden);
        }
        int mask = 1 << SkinPartLayout.V2.bit(part);
        return (hidden ? flags | mask : flags & ~mask) & 0xFF;
    }

    /** Recomputes the vanilla cape bit from the wearer's choice and the equipment state. */
    public static int composeCape(int flags, boolean hiddenByEquipment) {
        boolean hidden = (flags & WEARER_CAPE_MASK) != 0 || hiddenByEquipment;
        return (hidden ? flags | CAPE_MASK : flags & ~CAPE_MASK) & 0xFF;
    }
}
