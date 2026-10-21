package industries.angelic.wawelauth.client.skin.translucent;

import java.nio.ByteOrder;
import java.util.Arrays;

import net.minecraft.client.renderer.Tessellator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorTessellator;

/**
 * A Tessellator draw's vertices, copied before they are drawn, to hand to the Tessellator again later vertex by
 * vertex. That takes the same path as any draw, Angelica's and its shaders' included.
 */
@SideOnly(Side.CLIENT)
final class TessellatorRecord implements TranslucentLayers.Replay {

    /** As the Tessellator lays out a vertex: position, texture, color, normal, light. */
    private static final int INTS = 8;
    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private final int[] data;
    private final int vertices;
    private final int mode;
    private final boolean texture;
    private final boolean brightness;
    private final boolean color;
    private final boolean normals;

    TessellatorRecord(AccessorTessellator tessellator) {
        vertices = tessellator.wawelauth$getVertexCount();
        data = Arrays.copyOf(tessellator.wawelauth$getRawBuffer(), vertices * INTS);
        mode = tessellator.wawelauth$getDrawMode();
        texture = tessellator.wawelauth$hasTexture();
        brightness = tessellator.wawelauth$hasBrightness();
        color = tessellator.wawelauth$hasColor();
        normals = tessellator.wawelauth$hasNormals();
    }

    @Override
    public void paint(float tint) {
        if (vertices == 0) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(mode);
        for (int i = 0; i < data.length; i += INTS) {
            if (texture) tessellator.setTextureUV(Float.intBitsToFloat(data[i + 3]), Float.intBitsToFloat(data[i + 4]));
            if (color) {
                int[] rgba = rgba(data[i + 5]);
                // Vertex colors take the place of the current color, which carries the tint otherwise.
                tessellator.setColorRGBA(rgba[0], (int) (rgba[1] * tint), (int) (rgba[2] * tint), rgba[3]);
            }
            if (normals) {
                int normal = data[i + 6];
                tessellator
                    .setNormal((byte) normal / 127.0F, (byte) (normal >> 8) / 127.0F, (byte) (normal >> 16) / 127.0F);
            }
            if (brightness) tessellator.setBrightness(data[i + 7]);
            tessellator.addVertex(
                Float.intBitsToFloat(data[i]),
                Float.intBitsToFloat(data[i + 1]),
                Float.intBitsToFloat(data[i + 2]));
        }
        tessellator.draw();
    }

    /** A vertex color as the Tessellator packs it, in the platform's byte order: red, green, blue, alpha. */
    static int[] rgba(int packed) {
        if (LITTLE_ENDIAN) {
            return new int[] { packed & 0xFF, packed >>> 8 & 0xFF, packed >>> 16 & 0xFF, packed >>> 24 };
        }
        return new int[] { packed >>> 24, packed >>> 16 & 0xFF, packed >>> 8 & 0xFF, packed & 0xFF };
    }
}
