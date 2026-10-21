package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A draw's vertices, for see-through skins to paint again later. */
@Mixin(Tessellator.class)
public interface AccessorTessellator {

    @Accessor("rawBuffer")
    int[] wawelauth$getRawBuffer();

    @Accessor("vertexCount")
    int wawelauth$getVertexCount();

    @Accessor("drawMode")
    int wawelauth$getDrawMode();

    @Accessor("hasTexture")
    boolean wawelauth$hasTexture();

    @Accessor("hasBrightness")
    boolean wawelauth$hasBrightness();

    @Accessor("hasColor")
    boolean wawelauth$hasColor();

    @Accessor("hasNormals")
    boolean wawelauth$hasNormals();
}
