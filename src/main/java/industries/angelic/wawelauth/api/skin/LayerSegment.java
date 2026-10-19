package industries.angelic.wawelauth.api.skin;

/**
 * One overlay box, or a slice of one, drawn on a host model part.
 * <p>
 * Coordinates follow {@link net.minecraft.client.model.ModelBox}: the origin is the box's minimum corner in pixels,
 * relative to the part's rotation point as set up by {@code ModelRenderer.postRender}. The size is in skin texels
 * and {@code u}, {@code v} is the overlay's UV origin in the 64x64 layout (for example 40, 32 for the right sleeve).
 * <p>
 * A slice covers rows {@code rowStart} to {@code rowEnd} of the box's height, so a jointed limb can draw its upper and
 * lower half separately. Caps only exist on the box's real ends: a slice that starts or ends inside the box has no cap
 * there. Immutable; hosts build their segments once and reuse them.
 */
public final class LayerSegment {

    private final SkinPart part;
    private final float originX;
    private final float originY;
    private final float originZ;
    private final int width;
    private final int height;
    private final int depth;
    private final int u;
    private final int v;
    private final int rowStart;
    private final int rowEnd;

    private LayerSegment(SkinPart part, float originX, float originY, float originZ, int width, int height, int depth,
        int u, int v, int rowStart, int rowEnd) {
        if (part == null || part == SkinPart.CAPE) {
            throw new IllegalArgumentException("Layer segments are for the overlay parts, not " + part);
        }
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Box size must be positive: " + width + "x" + height + "x" + depth);
        }
        if (rowStart < 0 || rowEnd > height || rowStart >= rowEnd) {
            throw new IllegalArgumentException("Rows " + rowStart + ".." + rowEnd + " outside 0.." + height);
        }
        this.part = part;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.u = u;
        this.v = v;
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;
    }

    /** A whole overlay box. */
    public static LayerSegment box(SkinPart part, float originX, float originY, float originZ, int width, int height,
        int depth, int u, int v) {
        return new LayerSegment(part, originX, originY, originZ, width, height, depth, u, v, 0, height);
    }

    /**
     * Rows {@code start} to {@code end} of this segment's box, keeping its origin. The slice starts {@code start}
     * pixels below the box origin.
     */
    public LayerSegment rows(int start, int end) {
        return new LayerSegment(part, originX, originY, originZ, width, height, depth, u, v, start, end);
    }

    public SkinPart part() {
        return part;
    }

    public float originX() {
        return originX;
    }

    public float originY() {
        return originY;
    }

    public float originZ() {
        return originZ;
    }

    public int width() {
        return width;
    }

    /** Height of the whole box, not of the slice. */
    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public int u() {
        return u;
    }

    public int v() {
        return v;
    }

    public int rowStart() {
        return rowStart;
    }

    public int rowEnd() {
        return rowEnd;
    }

    public int sliceHeight() {
        return rowEnd - rowStart;
    }

    public boolean hasTopCap() {
        return rowStart == 0;
    }

    public boolean hasBottomCap() {
        return rowEnd == height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LayerSegment other)) return false;
        return part == other.part && Float.compare(originX, other.originX) == 0
            && Float.compare(originY, other.originY) == 0
            && Float.compare(originZ, other.originZ) == 0
            && width == other.width
            && height == other.height
            && depth == other.depth
            && u == other.u
            && v == other.v
            && rowStart == other.rowStart
            && rowEnd == other.rowEnd;
    }

    @Override
    public int hashCode() {
        int h = part.hashCode();
        h = 31 * h + Float.floatToIntBits(originX);
        h = 31 * h + Float.floatToIntBits(originY);
        h = 31 * h + Float.floatToIntBits(originZ);
        h = 31 * h + width;
        h = 31 * h + height;
        h = 31 * h + depth;
        h = 31 * h + u;
        h = 31 * h + v;
        h = 31 * h + rowStart;
        h = 31 * h + rowEnd;
        return h;
    }

    @Override
    public String toString() {
        return part + "["
            + originX
            + ","
            + originY
            + ","
            + originZ
            + " "
            + width
            + "x"
            + height
            + "x"
            + depth
            + " uv "
            + u
            + ","
            + v
            + " rows "
            + rowStart
            + ".."
            + rowEnd
            + "]";
    }
}
