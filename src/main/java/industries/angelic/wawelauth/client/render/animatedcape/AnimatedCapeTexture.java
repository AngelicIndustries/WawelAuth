package industries.angelic.wawelauth.client.render.animatedcape;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import org.w3c.dom.Node;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.ApngUtil;
import industries.angelic.angellib.util.GifUtil;
import industries.angelic.wawelauth.WawelAuth;

/**
 * Wraps a {@link DynamicTexture} and cycles through GIF animation frames.
 * <p>
 * The GIF is decoded via AngelLib's {@link GifUtil} which stitches all frames
 * into a single horizontal sprite sheet. Each tick, the appropriate frame's
 * pixels are copied into the dynamic texture.
 */
@SideOnly(Side.CLIENT)
public class AnimatedCapeTexture {

    private final DynamicTexture dynamicTexture;
    private final ResourceLocation resourceLocation;
    private final int[] spriteSheetPixels;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final int frameDelayMs;
    private int lastFrame = -1;

    private AnimatedCapeTexture(DynamicTexture dynamicTexture, ResourceLocation resourceLocation,
        int[] spriteSheetPixels, int frameWidth, int frameHeight, int frameCount, int frameDelayMs) {
        this.dynamicTexture = dynamicTexture;
        this.resourceLocation = resourceLocation;
        this.spriteSheetPixels = spriteSheetPixels;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;
        this.frameDelayMs = frameDelayMs > 0 ? frameDelayMs : 100;
    }

    /**
     * Advances the animation. Call once per tick (or per frame).
     * Only updates the texture when the current frame index changes.
     */
    public void tick() {
        int frame = (int) ((System.currentTimeMillis() / frameDelayMs) % frameCount);
        if (frame == lastFrame) return;
        lastFrame = frame;

        int[] texPixels = dynamicTexture.getTextureData();
        for (int y = 0; y < frameHeight; y++) {
            System.arraycopy(
                spriteSheetPixels,
                y * (frameWidth * frameCount) + frame * frameWidth,
                texPixels,
                y * frameWidth,
                frameWidth);
        }
        dynamicTexture.updateDynamicTexture();
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }

    /** Unregisters and frees the GL texture. Main thread only. */
    public void delete() {
        Minecraft.getMinecraft().renderEngine.deleteTexture(resourceLocation);
    }

    /**
     * Intermediate result from decoding a GIF on a background thread.
     * Contains only CPU-side data: no OpenGL resources.
     * Must be finalized on the main thread via {@link #createFromDecoded}.
     */
    public static class DecodedGif {

        final int[] spriteSheetPixels;
        final int frameWidth;
        final int frameHeight;
        final int frameCount;
        final int frameDelayMs;

        DecodedGif(int[] spriteSheetPixels, int frameWidth, int frameHeight, int frameCount, int frameDelayMs) {
            this.spriteSheetPixels = spriteSheetPixels;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.frameCount = frameCount;
            this.frameDelayMs = frameDelayMs;
        }
    }

    /** Frames are kept no larger than this: a cape is drawn small, and its frames all stay in memory. */
    private static final int MAX_FRAME_PIXELS = 512 * 256;
    /** The whole sprite sheet stays under 64 MB, however many frames the cape has. */
    private static final int MAX_SHEET_PIXELS = 16 * 1024 * 1024;

    /** Decodes an animated GIF or APNG cape; null when it's neither, or can't be read. */
    public static DecodedGif decodeAnimated(byte[] data) {
        if (data == null) return null;
        if (isGif(data)) return decodeGif(data);
        if (ApngUtil.isAnimated(data)) return decodeApng(data);
        return null;
    }

    public static boolean isGif(byte[] data) {
        return data != null && data.length >= 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F';
    }

    /** Decodes an APNG into a sprite sheet pixel array, reading large ones scaled down. Safe on any thread. */
    public static DecodedGif decodeApng(byte[] apngBytes) {
        try {
            GifUtil.GifData apng = ApngUtil.read(apngBytes, 1024, 512);
            BufferedImage first = apng.getFrame(0);
            return fromFrames(apng, first.getWidth(), first.getHeight());
        } catch (Exception e) {
            WawelAuth.LOG.warn("Failed to decode animated cape APNG: {}", e.getMessage());
            return null;
        }
    }

    /** The frames side by side at the largest size the limits allow, keeping their proportions. */
    private static DecodedGif fromFrames(GifUtil.GifData frames, int width, int height) {
        int count = frames.getFrameCount();
        double maxPixels = Math.min(MAX_FRAME_PIXELS, MAX_SHEET_PIXELS / (double) Math.max(1, count));
        double scale = Math.min(1.0, Math.sqrt(maxPixels / ((double) width * height)));
        int frameWidth = Math.max(1, (int) Math.floor(width * scale));
        int frameHeight = Math.max(1, (int) Math.floor(height * scale));
        BufferedImage sheet = GifUtil.stitchGif(frames, frameWidth, frameHeight);
        int[] pixels = sheet.getRGB(0, 0, sheet.getWidth(), sheet.getHeight(), null, 0, sheet.getWidth());
        return new DecodedGif(pixels, frameWidth, frameHeight, count, GifUtil.getFrameDelay(frames));
    }

    /**
     * Decodes a GIF into a sprite sheet pixel array. Safe to call from any thread.
     * Does NOT create any OpenGL resources.
     *
     * @param gifBytes raw GIF file bytes
     * @return decoded data, or null if decoding failed
     */
    public static DecodedGif decodeGif(byte[] gifBytes) {
        try {
            // Read native GIF dimensions to preserve HD resolution
            int nativeW = 64, nativeH = 32;
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try (ImageInputStream iis = new MemoryCacheImageInputStream(new ByteArrayInputStream(gifBytes))) {
                    reader.setInput(iis);
                    int[] logicalDimensions = readGifLogicalDimensions(reader);
                    if (logicalDimensions != null) {
                        nativeW = logicalDimensions[0];
                        nativeH = logicalDimensions[1];
                    } else {
                        nativeW = reader.getWidth(0);
                        nativeH = reader.getHeight(0);
                    }
                } finally {
                    reader.dispose();
                }
            }

            return fromFrames(GifUtil.readGif(gifBytes), nativeW, nativeH);
        } catch (Exception e) {
            WawelAuth.LOG.warn("Failed to decode animated cape GIF: {}", e.getMessage());
            return null;
        }
    }

    private static int[] readGifLogicalDimensions(ImageReader reader) throws java.io.IOException {
        IIOMetadata metadata = reader.getStreamMetadata();
        if (metadata == null || metadata.getNativeMetadataFormatName() == null) {
            return null;
        }

        Node descriptor = findMetadataNode(
            metadata.getAsTree(metadata.getNativeMetadataFormatName()),
            "LogicalScreenDescriptor");
        if (descriptor == null) {
            return null;
        }

        Integer width = readIntAttribute(descriptor, "logicalScreenWidth");
        Integer height = readIntAttribute(descriptor, "logicalScreenHeight");
        if (width == null || height == null) {
            return null;
        }
        return new int[] { width.intValue(), height.intValue() };
    }

    private static Node findMetadataNode(Node node, String name) {
        if (node == null) {
            return null;
        }
        if (name.equals(node.getNodeName())) {
            return node;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node match = findMetadataNode(child, name);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static Integer readIntAttribute(Node node, String name) {
        Node attribute = node.getAttributes() == null ? null
            : node.getAttributes()
                .getNamedItem(name);
        if (attribute == null) {
            return null;
        }
        try {
            return Integer.valueOf(attribute.getNodeValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Creates the OpenGL texture and registers it. MUST be called on the main (render) thread.
     *
     * @param decoded      the decoded GIF data from {@link #decodeGif}
     * @param locationPath resource location path (e.g. "capes/animated/uuid")
     * @return the AnimatedCapeTexture, or null on failure
     */
    public static AnimatedCapeTexture createFromDecoded(DecodedGif decoded, String locationPath) {
        return createFromDecoded(decoded, new ResourceLocation("wawelauth", locationPath));
    }

    /** As {@link #createFromDecoded(DecodedGif, String)}, at any location, replacing the texture there. */
    public static AnimatedCapeTexture createFromDecoded(DecodedGif decoded, ResourceLocation location) {
        if (decoded == null) return null;

        try {
            DynamicTexture frameTex = new DynamicTexture(decoded.frameWidth, decoded.frameHeight);

            Minecraft mc = Minecraft.getMinecraft();
            ITextureObject existing = mc.renderEngine.getTexture(location);
            if (existing != null) {
                mc.renderEngine.deleteTexture(location);
            }
            mc.renderEngine.loadTexture(location, frameTex);

            AnimatedCapeTexture result = new AnimatedCapeTexture(
                frameTex,
                location,
                decoded.spriteSheetPixels,
                decoded.frameWidth,
                decoded.frameHeight,
                decoded.frameCount,
                decoded.frameDelayMs);

            // Force first frame render
            result.tick();

            WawelAuth.debug(
                "Loaded animated cape: " + decoded.frameCount
                    + " frames, "
                    + decoded.frameWidth
                    + "x"
                    + decoded.frameHeight
                    + ", delay="
                    + decoded.frameDelayMs
                    + "ms");

            return result;
        } catch (Exception e) {
            WawelAuth.LOG.warn("Failed to create animated cape texture: {}", e.getMessage());
            return null;
        }
    }
}
