package industries.angelic.wawelauth.client.skin.layers;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.wawelauth.api.skin.LayerSegment;
import industries.angelic.wawelauth.api.skin.SkinPart;

public class OverlayBoxGeometryTest {

    private static final float EPSILON = 1.0E-6F;
    private static final LayerSegment SLEEVE = LayerSegment.box(SkinPart.RIGHT_SLEEVE, -3, -2, -2, 4, 12, 4, 40, 32);

    @Test
    public void wholeBoxMatchesVanillaModelBox() throws Exception {
        ModelRenderer renderer = new ModelRenderer(new ModelBase() {}, 40, 32);
        renderer.setTextureSize(64, 64);
        renderer.addBox(-3, -2, -2, 4, 12, 4, 0.25F);
        ModelBox box = (ModelBox) renderer.cubeList.get(0);
        Field quadField = ModelBox.class.getDeclaredField("quadList");
        quadField.setAccessible(true);
        TexturedQuad[] vanilla = (TexturedQuad[]) quadField.get(box);

        List<OverlayBoxGeometry.Quad> ours = OverlayBoxGeometry.quads(SLEEVE, 0.25F);
        Assert.assertEquals(vanilla.length, ours.size());
        for (int q = 0; q < vanilla.length; q++) {
            for (int i = 0; i < 4; i++) {
                PositionTextureVertex vertex = vanilla[q].vertexPositions[i];
                OverlayBoxGeometry.Quad quad = ours.get(q);
                String at = "quad " + q + " vertex " + i;
                Assert.assertEquals(at, (float) vertex.vector3D.xCoord, quad.positions[i * 3], EPSILON);
                Assert.assertEquals(at, (float) vertex.vector3D.yCoord, quad.positions[i * 3 + 1], EPSILON);
                Assert.assertEquals(at, (float) vertex.vector3D.zCoord, quad.positions[i * 3 + 2], EPSILON);
                Assert.assertEquals(at, vertex.texturePositionX, quad.uvs[i * 2], EPSILON);
                Assert.assertEquals(at, vertex.texturePositionY, quad.uvs[i * 2 + 1], EPSILON);
            }
        }
    }

    @Test
    public void upperSliceHasNoBottomCapAndNoInflationAtTheCut() {
        List<OverlayBoxGeometry.Quad> quads = OverlayBoxGeometry.quads(SLEEVE.rows(0, 6), 0.25F);
        Assert.assertEquals(5, quads.size());
        Assert.assertEquals(-2.25F, minY(quads), EPSILON);
        Assert.assertEquals(4.0F, maxY(quads), EPSILON);
        assertSideRows(quads, 36, 42);
    }

    @Test
    public void lowerSliceHasNoTopCapAndReadsLowerRows() {
        List<OverlayBoxGeometry.Quad> quads = OverlayBoxGeometry.quads(SLEEVE.rows(6, 12), 0.25F);
        Assert.assertEquals(5, quads.size());
        Assert.assertEquals(4.0F, minY(quads), EPSILON);
        Assert.assertEquals(10.25F, maxY(quads), EPSILON);
        assertSideRows(quads, 42, 48);
    }

    @Test
    public void normalsAreUnitLength() {
        for (OverlayBoxGeometry.Quad quad : OverlayBoxGeometry.quads(SLEEVE, 0.25F)) {
            float length = (float) Math
                .sqrt(quad.normalX * quad.normalX + quad.normalY * quad.normalY + quad.normalZ * quad.normalZ);
            Assert.assertEquals(1.0F, length, EPSILON);
        }
    }

    private static float minY(List<OverlayBoxGeometry.Quad> quads) {
        float min = Float.MAX_VALUE;
        for (OverlayBoxGeometry.Quad quad : quads) {
            for (int i = 0; i < 4; i++) min = Math.min(min, quad.positions[i * 3 + 1]);
        }
        return min;
    }

    private static float maxY(List<OverlayBoxGeometry.Quad> quads) {
        float max = -Float.MAX_VALUE;
        for (OverlayBoxGeometry.Quad quad : quads) {
            for (int i = 0; i < 4; i++) max = Math.max(max, quad.positions[i * 3 + 1]);
        }
        return max;
    }

    /** Side faces are the quads whose texture rows start below the caps (v >= v + depth). */
    private static void assertSideRows(List<OverlayBoxGeometry.Quad> quads, int startRow, int endRow) {
        for (OverlayBoxGeometry.Quad quad : quads) {
            float minV = Math.min(quad.uvs[1], quad.uvs[5]) * 64.0F;
            float maxV = Math.max(quad.uvs[1], quad.uvs[5]) * 64.0F;
            if (minV < 36.0F) continue;
            Assert.assertEquals(startRow, minV, EPSILON);
            Assert.assertEquals(endRow, maxV, EPSILON);
        }
    }
}
