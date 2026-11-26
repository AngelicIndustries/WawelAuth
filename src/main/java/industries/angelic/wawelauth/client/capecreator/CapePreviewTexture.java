package industries.angelic.wawelauth.client.capecreator;

import java.awt.image.BufferedImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The cape the creator's preview players wear. It holds one frame on the GPU and copies the next one in when its time
 * comes, so a long GIF needs no texture as wide as all its frames. Client thread only.
 */
@SideOnly(Side.CLIENT)
public final class CapePreviewTexture {

    private DynamicTexture texture;
    private ResourceLocation location;
    private int width;
    private int height;
    private int[][] frames;
    private int[] delaysMs;
    private int frame;
    private long nextFrameAt;

    /** Shows new frames, from the first one on. */
    public void show(CapeCreator.PreviewFrames preview) {
        if (texture == null || width != preview.width || height != preview.height) {
            delete();
            width = preview.width;
            height = preview.height;
            texture = new DynamicTexture(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
            location = Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation("wawelauth_cape_creator", texture);
        }
        frames = preview.pixels;
        delaysMs = preview.delaysMs;
        frame = 0;
        upload();
        nextFrameAt = System.currentTimeMillis() + delaysMs[0];
    }

    /** Moves to the frame due now; call before drawing. */
    public void tick() {
        if (frames == null || frames.length < 2) return;
        long now = System.currentTimeMillis();
        if (now < nextFrameAt) return;
        // After a long pause (the game in the background), skip ahead instead of playing every missed frame.
        int steps = 0;
        while (now >= nextFrameAt && steps++ < frames.length) {
            frame = (frame + 1) % frames.length;
            nextFrameAt += delaysMs[frame];
        }
        if (now >= nextFrameAt) nextFrameAt = now + delaysMs[frame];
        upload();
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public void delete() {
        if (texture != null) {
            Minecraft.getMinecraft()
                .getTextureManager()
                .deleteTexture(location);
            texture = null;
            location = null;
        }
    }

    private void upload() {
        System.arraycopy(frames[frame], 0, texture.getTextureData(), 0, width * height);
        texture.updateDynamicTexture();
    }
}
