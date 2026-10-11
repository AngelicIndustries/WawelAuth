package org.fentanylsolutions.wawelauth.client.skin.layers;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.fentanylsolutions.wawelauth.api.skin.LayerSegment;
import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Flat overlay boxes. Their geometry does not depend on the skin, so each segment is compiled once into a display
 * list and shared by every player.
 */
@SideOnly(Side.CLIENT)
final class OverlayBoxes {

    /** Vanilla model scale; lists are compiled at it. */
    static final float MODEL_SCALE = 0.0625F;

    private static final Map<LayerSegment, Integer> LISTS = new HashMap<>();

    private OverlayBoxes() {}

    /** Vanilla's 64x64 player model inflates the headwear by 0.5 and the other overlays by 0.25. */
    static float inflate(SkinPart part) {
        return part == SkinPart.HAT ? 0.5F : 0.25F;
    }

    static void draw(LayerSegment segment, float scale) {
        Integer list = LISTS.get(segment);
        if (list == null) {
            list = compile(segment);
            LISTS.put(segment, list);
        }
        if (scale == MODEL_SCALE) {
            GL11.glCallList(list);
            return;
        }
        float factor = scale / MODEL_SCALE;
        GL11.glPushMatrix();
        GL11.glScalef(factor, factor, factor);
        GL11.glCallList(list);
        GL11.glPopMatrix();
    }

    private static int compile(LayerSegment segment) {
        int list = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        Tessellator tessellator = Tessellator.instance;
        for (OverlayBoxGeometry.Quad quad : OverlayBoxGeometry.quads(segment, inflate(segment.part()))) {
            tessellator.startDrawingQuads();
            tessellator.setNormal(quad.normalX, quad.normalY, quad.normalZ);
            for (int i = 0; i < 4; i++) {
                tessellator.addVertexWithUV(
                    quad.positions[i * 3] * MODEL_SCALE,
                    quad.positions[i * 3 + 1] * MODEL_SCALE,
                    quad.positions[i * 3 + 2] * MODEL_SCALE,
                    quad.uvs[i * 2],
                    quad.uvs[i * 2 + 1]);
            }
            tessellator.draw();
        }
        GL11.glEndList();
        return list;
    }
}
