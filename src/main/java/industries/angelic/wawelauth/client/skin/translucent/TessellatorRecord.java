package industries.angelic.wawelauth.client.skin.translucent;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorTessellator;

/** A Tessellator draw's vertices, copied before they are drawn, to draw them again later the way it does. */
@SideOnly(Side.CLIENT)
final class TessellatorRecord implements TranslucentLayers.Replay {

    /** As the Tessellator lays out a vertex: position, texture, color, normal, light; 32 bytes. */
    private static final int STRIDE = 32;
    private static ByteBuffer bytes;
    private static IntBuffer ints;
    private static FloatBuffer floats;
    private static ShortBuffer shorts;

    private final int[] data;
    private final int vertices;
    private final int mode;
    private final boolean texture;
    private final boolean brightness;
    private final boolean color;
    private final boolean normals;

    TessellatorRecord(AccessorTessellator tessellator) {
        vertices = tessellator.wawelauth$getVertexCount();
        data = Arrays.copyOf(tessellator.wawelauth$getRawBuffer(), vertices * STRIDE / 4);
        mode = tessellator.wawelauth$getDrawMode();
        texture = tessellator.wawelauth$hasTexture();
        brightness = tessellator.wawelauth$hasBrightness();
        color = tessellator.wawelauth$hasColor();
        normals = tessellator.wawelauth$hasNormals();
    }

    @Override
    public void paint(float tint) {
        if (vertices == 0) return;
        reserve(vertices * STRIDE);
        ints.clear();
        ints.put(data);
        // Vertex colors take the place of the current color, which carries the tint otherwise.
        if (color && tint != 1.0F) for (int i = 5; i < data.length; i += STRIDE / 4) ints.put(i, tinted(data[i], tint));

        if (texture) {
            floats.position(3);
            GL11.glTexCoordPointer(2, STRIDE, floats);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        }
        if (brightness) {
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            shorts.position(14);
            GL11.glTexCoordPointer(2, STRIDE, shorts);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        if (color) {
            bytes.position(20);
            GL11.glColorPointer(4, true, STRIDE, bytes);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        }
        if (normals) {
            bytes.position(24);
            GL11.glNormalPointer(STRIDE, bytes);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        }
        floats.position(0);
        GL11.glVertexPointer(3, STRIDE, floats);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDrawArrays(mode, 0, vertices);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        if (texture) GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        if (brightness) {
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        if (color) GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        if (normals) GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
    }

    private static void reserve(int size) {
        if (bytes != null && bytes.capacity() >= size) {
            bytes.clear();
            return;
        }
        bytes = GLAllocation.createDirectByteBuffer(Math.max(64 * 1024, Integer.highestOneBit(size) << 1));
        ints = bytes.asIntBuffer();
        floats = bytes.asFloatBuffer();
        shorts = bytes.asShortBuffer();
    }

    /**
     * A vertex color with its green and blue scaled. In either byte order the Tessellator packs them into the middle
     * two bytes.
     */
    static int tinted(int color, float tint) {
        int low = (int) ((color >>> 8 & 0xFF) * tint);
        int high = (int) ((color >>> 16 & 0xFF) * tint);
        return color & 0xFF0000FF | high << 16 | low << 8;
    }
}
