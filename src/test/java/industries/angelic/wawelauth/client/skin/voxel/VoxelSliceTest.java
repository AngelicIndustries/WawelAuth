package industries.angelic.wawelauth.client.skin.voxel;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.wawelauth.api.skin.LayerSegment;
import industries.angelic.wawelauth.api.skin.SkinPart;

public class VoxelSliceTest {

    // Right sleeve, classic: 4x12x4 at uv 40,32, minimum corner -3,-2,-2.
    private static final int W = 4, H = 12, D = 4, U = 40, V = 32;
    private static final LayerSegment SLEEVE = LayerSegment.box(SkinPart.RIGHT_SLEEVE, -3, -2, -2, W, H, D, U, V);

    @Test
    public void wholeBoxHasAVoxelPerTexelAndBothCaps() {
        List<Voxel> voxels = record(SLEEVE);
        Assert.assertEquals(2 * W * D + 2 * W * H + 2 * D * H, voxels.size());
        assertRows(voxels, -2, -2 + H);
        Assert.assertTrue(any(voxels, Region.TOP_CAP));
        Assert.assertTrue(any(voxels, Region.BOTTOM_CAP));
    }

    @Test
    public void upperHalfDropsTheBottomCapAndReadsUpperRows() {
        List<Voxel> voxels = record(SLEEVE.rows(0, 6));
        Assert.assertEquals(W * D + 2 * W * 6 + 2 * D * 6, voxels.size());
        assertRows(voxels, -2, 4);
        Assert.assertTrue(any(voxels, Region.TOP_CAP));
        Assert.assertFalse(any(voxels, Region.BOTTOM_CAP));
        assertSideRows(voxels, V + D, V + D + 6);
    }

    @Test
    public void lowerHalfDropsTheTopCapAndReadsLowerRows() {
        List<Voxel> voxels = record(SLEEVE.rows(6, 12));
        Assert.assertEquals(W * D + 2 * W * 6 + 2 * D * 6, voxels.size());
        assertRows(voxels, 4, 10);
        Assert.assertFalse(any(voxels, Region.TOP_CAP));
        Assert.assertTrue(any(voxels, Region.BOTTOM_CAP));
        assertSideRows(voxels, V + D + 6, V + D + H);
    }

    @Test
    public void legacyWrapBoxMatchesAWholeSlice() {
        Recorder legacy = new Recorder();
        VoxelSurfaceBuilder.wrapBox(legacy, new Solid(), W, H, D, U, V, true, -2f);
        Recorder slice = new Recorder();
        VoxelSurfaceBuilder.wrapSlice(slice, new Solid(), W, H, D, U, V, 0, H, -W / 2f, -2f, -D / 2f);
        Assert.assertEquals(legacy.voxels, slice.voxels);
    }

    @Test
    public void transparentTexelsMakeNoVoxels() {
        Assert.assertTrue(
            VoxelMeshes.cubes(new Empty(), SLEEVE)
                .isEmpty());
    }

    @Test
    public void segmentValidation() {
        assertRejected(() -> LayerSegment.box(SkinPart.CAPE, 0, 0, 0, 10, 16, 1, 0, 0));
        assertRejected(() -> SLEEVE.rows(6, 6));
        assertRejected(() -> SLEEVE.rows(-1, 6));
        assertRejected(() -> SLEEVE.rows(6, 13));
        Assert.assertEquals(SLEEVE.rows(0, 6), SLEEVE.rows(0, 6));
        Assert.assertEquals(
            SLEEVE.rows(0, 6)
                .hashCode(),
            SLEEVE.rows(0, 6)
                .hashCode());
        Assert.assertNotEquals(SLEEVE.rows(0, 6), SLEEVE.rows(6, 12));
        Assert.assertTrue(
            SLEEVE.rows(0, 6)
                .hasTopCap());
        Assert.assertFalse(
            SLEEVE.rows(0, 6)
                .hasBottomCap());
    }

    private enum Region {
        TOP_CAP,
        BOTTOM_CAP
    }

    private static boolean any(List<Voxel> voxels, Region region) {
        for (Voxel voxel : voxels) {
            if (voxel.v >= V + D) continue;
            boolean top = voxel.u >= U + D && voxel.u < U + D + W;
            boolean bottom = voxel.u >= U + D + W && voxel.u < U + D + 2 * W;
            if (region == Region.TOP_CAP ? top : bottom) return true;
        }
        return false;
    }

    private static void assertRows(List<Voxel> voxels, float minY, float endY) {
        for (Voxel voxel : voxels) {
            Assert.assertTrue("y " + voxel.y, voxel.y >= minY && voxel.y < endY);
        }
    }

    private static void assertSideRows(List<Voxel> voxels, int startV, int endV) {
        for (Voxel voxel : voxels) {
            if (voxel.v < V + D) continue;
            Assert.assertTrue("v " + voxel.v, voxel.v >= startV && voxel.v < endV);
        }
    }

    private static void assertRejected(Runnable action) {
        try {
            action.run();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
    }

    private static List<Voxel> record(LayerSegment segment) {
        Recorder recorder = new Recorder();
        VoxelMeshes.wrap(recorder, new Solid(), segment);
        return recorder.voxels;
    }

    /** One recorded voxel: its position and the texel it samples. */
    private static final class Voxel {

        final float x, y, z;
        final int u, v;

        Voxel(float x, float y, float z, int u, int v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Voxel other)) return false;
            return x == other.x && y == other.y && z == other.z && u == other.u && v == other.v;
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits(x) * 31 + Float.floatToIntBits(y) * 17 + u * 7 + v;
        }
    }

    private static final class Recorder implements VoxelBuilder {

        final List<Voxel> voxels = new ArrayList<>();
        private int u, v;

        @Override
        public VoxelBuilder textureSize(int width, int height) {
            return this;
        }

        @Override
        public VoxelBuilder uv(int u, int v) {
            // The surface builder offsets the cube UV by (-2, -1) so the cube's front samples the texel.
            this.u = u + 2;
            this.v = v + 1;
            return this;
        }

        @Override
        public VoxelBuilder mirror(boolean bl) {
            return this;
        }

        @Override
        public VoxelBuilder addBox(float x, float y, float z, float pixelSize, Direction[] hide,
            Direction[][] corners) {
            voxels.add(new Voxel(x, y, z, u, v));
            return this;
        }

        @Override
        public VoxelBuilder addVanillaBox(float x, float y, float z, float width, float height, float depth) {
            return this;
        }

        @Override
        public boolean isEmpty() {
            return voxels.isEmpty();
        }
    }

    private static final class Solid implements VoxelTexture {

        @Override
        public boolean isPresent(VoxelSurfaceBuilder.UV uv) {
            return true;
        }

        @Override
        public boolean isSolid(VoxelSurfaceBuilder.UV uv) {
            return true;
        }

        @Override
        public int getWidth() {
            return 64;
        }

        @Override
        public int getHeight() {
            return 64;
        }
    }

    private static final class Empty implements VoxelTexture {

        @Override
        public boolean isPresent(VoxelSurfaceBuilder.UV uv) {
            return false;
        }

        @Override
        public boolean isSolid(VoxelSurfaceBuilder.UV uv) {
            return false;
        }

        @Override
        public int getWidth() {
            return 64;
        }

        @Override
        public int getHeight() {
            return 64;
        }
    }
}
