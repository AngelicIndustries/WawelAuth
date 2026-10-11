package org.fentanylsolutions.wawelauth.client.skin.layers;

import java.util.ArrayList;
import java.util.List;

import org.fentanylsolutions.wawelauth.api.skin.LayerSegment;

/**
 * Quads of a flat overlay box, laid out exactly like vanilla's {@code ModelBox} (corners, face order, UV rectangles and
 * normals), in pixels and with UVs over a 64x64 layout.
 * <p>
 * A slice only gets the caps it reaches, its side faces read its own rows, and it is inflated vertically only at the
 * box's real ends, so the two halves of a limb meet at the joint.
 */
final class OverlayBoxGeometry {

    static final float TEXTURE_SIZE = 64.0F;

    private OverlayBoxGeometry() {}

    static final class Quad {

        /** Four vertices, x y z each, in pixels. */
        final float[] positions;
        /** Four vertices, u v each, 0 to 1. */
        final float[] uvs;
        final float normalX;
        final float normalY;
        final float normalZ;

        Quad(float[] positions, float[] uvs, float normalX, float normalY, float normalZ) {
            this.positions = positions;
            this.uvs = uvs;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }
    }

    static List<Quad> quads(LayerSegment segment, float inflate) {
        float x1 = segment.originX() - inflate;
        float x2 = segment.originX() + segment.width() + inflate;
        float y1 = segment.originY() + segment.rowStart() - (segment.hasTopCap() ? inflate : 0.0F);
        float y2 = segment.originY() + segment.rowEnd() + (segment.hasBottomCap() ? inflate : 0.0F);
        float z1 = segment.originZ() - inflate;
        float z2 = segment.originZ() + segment.depth() + inflate;

        // Corner names follow ModelBox's vertex construction.
        float[] a = { x1, y1, z1 };
        float[] b = { x2, y1, z1 };
        float[] c = { x2, y2, z1 };
        float[] d = { x1, y2, z1 };
        float[] e = { x1, y1, z2 };
        float[] f = { x2, y1, z2 };
        float[] g = { x2, y2, z2 };
        float[] h = { x1, y2, z2 };

        int u = segment.u();
        int v = segment.v();
        int w = segment.width();
        int dp = segment.depth();
        int sideTop = v + dp + segment.rowStart();
        int sideBottom = v + dp + segment.rowEnd();

        List<Quad> quads = new ArrayList<>(6);
        quads.add(quad(f, b, c, g, u + dp + w, sideTop, u + dp + w + dp, sideBottom));
        quads.add(quad(a, e, h, d, u, sideTop, u + dp, sideBottom));
        if (segment.hasTopCap()) {
            quads.add(quad(f, e, a, b, u + dp, v, u + dp + w, v + dp));
        }
        if (segment.hasBottomCap()) {
            quads.add(quad(c, d, h, g, u + dp + w, v + dp, u + dp + w + w, v));
        }
        quads.add(quad(b, a, d, c, u + dp, sideTop, u + dp + w, sideBottom));
        quads.add(quad(e, f, g, h, u + dp + w + dp, sideTop, u + dp + w + dp + w, sideBottom));
        return quads;
    }

    /** UVs as in TexturedQuad: vertex 0 gets (u2, v1), 1 (u1, v1), 2 (u1, v2), 3 (u2, v2). */
    private static Quad quad(float[] p0, float[] p1, float[] p2, float[] p3, int u1, int v1, int u2, int v2) {
        float[] positions = { p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], p3[0], p3[1], p3[2] };
        float[] uvs = { u2 / TEXTURE_SIZE, v1 / TEXTURE_SIZE, u1 / TEXTURE_SIZE, v1 / TEXTURE_SIZE, u1 / TEXTURE_SIZE,
            v2 / TEXTURE_SIZE, u2 / TEXTURE_SIZE, v2 / TEXTURE_SIZE };

        // TexturedQuad.draw: (v2 - v1) x (v0 - v1), normalized.
        float ax = p2[0] - p1[0], ay = p2[1] - p1[1], az = p2[2] - p1[2];
        float bx = p0[0] - p1[0], by = p0[1] - p1[1], bz = p0[2] - p1[2];
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0F) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        return new Quad(positions, uvs, nx, ny, nz);
    }
}
