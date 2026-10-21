package industries.angelic.wawelauth.client.skin.translucent;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.renderer.texture.TextureUtil;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.skin.PlayerSkin;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/**
 * The base skin with its semi-transparent texels, for the draws that split them off. Like vanilla, the skin's own
 * texture has its base forced opaque, which every other renderer keeps drawing; a skin whose base has semi-transparent
 * texels gets a copy with their alpha back. Fully transparent base texels stay opaque, so a skin can't hide its body,
 * and skins carrying Ears' data in their base alpha stay opaque altogether.
 */
@SideOnly(Side.CLIENT)
final class SeeThroughBase {

    /** No semi-transparent base texel: the base draws as before. */
    static final int NONE = -1;
    /** The skin's own texture already holds them, as unprocessed local and HD skins do. */
    static final int SKIN = 0;

    private static final int MAX_PROFILES = 512;
    private static final Map<UUID, Entry> PROFILES = new HashMap<>();
    private static final List<Integer> RETIRED = new ArrayList<>();

    /** Ears' Alfalfa: bytes in 7-bit pieces in the alpha of these base texels, x then y, starting with its magic. */
    private static final int[][] ALFALFA_REGIONS = { { 8, 0, 24, 8 }, { 0, 8, 8, 16 }, { 16, 8, 32, 16 },
        { 4, 16, 12, 20 }, { 20, 16, 36, 20 }, { 44, 16, 52, 20 }, { 0, 20, 56, 32 }, { 20, 48, 28, 52 },
        { 36, 48, 44, 52 }, { 16, 52, 48, 64 } };
    private static final int ALFALFA_MAGIC = 0xEA1FA1FA;

    private SeeThroughBase() {}

    /** The texture to split the player's base from: its own GL name, {@link #SKIN} or {@link #NONE}. */
    static int texture(PlayerSkin skin) {
        Entry entry = PROFILES.get(skin.profileId());
        if (entry == null || entry.revision != skin.revision()) {
            if (entry != null) retire(entry);
            if (PROFILES.size() >= MAX_PROFILES) clear();
            entry = new Entry(skin.revision(), build(skin));
            PROFILES.put(skin.profileId(), entry);
        }
        return entry.texture;
    }

    private static int build(PlayerSkin skin) {
        if (!skin.available()) return NONE;
        BufferedImage normalized = skin.normalizedImage();
        if (normalized == null) return NONE;
        if (fractional(normalized, skin.model())) return alfalfa(normalized) ? NONE : SKIN;
        BufferedImage restored = restored(skin.originalImage(), normalized, skin.model());
        if (restored == null) return NONE;
        int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int texture = TextureUtil.glGenTextures();
        TextureUtil.uploadTextureImage(texture, restored);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, bound);
        return texture;
    }

    /** Textures of old revisions are deleted at the start of the next frame: this one's draws may still use them. */
    static void deleteRetired() {
        for (int texture : RETIRED) TextureUtil.deleteTexture(texture);
        RETIRED.clear();
    }

    static void clear() {
        for (Entry entry : PROFILES.values()) retire(entry);
        PROFILES.clear();
    }

    private static void retire(Entry entry) {
        if (entry.texture > 0) RETIRED.add(entry.texture);
    }

    /**
     * The skin as uploaded, with its base faces' semi-transparent alpha back from the image as decoded, or null when
     * there is none. Only for 64x64 skins: legacy ones never had a see-through base.
     */
    static BufferedImage restored(BufferedImage original, BufferedImage normalized, SkinModel model) {
        if (original == null || original.getWidth() != 64 || original.getHeight() != 64) return null;
        if (normalized.getWidth() != 64 || normalized.getHeight() != 64) return null;
        BufferedImage restored = null;
        for (int[] box : boxes(model)) for (int[] face : faces(box)) {
            for (int y = face[1]; y < face[1] + face[3]; y++) for (int x = face[0]; x < face[0] + face[2]; x++) {
                int alpha = original.getRGB(x, y) >>> 24;
                if (alpha == 0 || alpha == 255) continue;
                if (restored == null) {
                    if (alfalfa(original)) return null;
                    restored = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                    restored.setRGB(0, 0, 64, 64, normalized.getRGB(0, 0, 64, 64, null, 0, 64), 0, 64);
                }
                restored.setRGB(x, y, alpha << 24 | normalized.getRGB(x, y) & 0xFFFFFF);
            }
        }
        return restored;
    }

    /** Whether the base faces of a 64-wide or square HD skin hold a semi-transparent texel. */
    static boolean fractional(BufferedImage image, SkinModel model) {
        for (int[] box : boxes(model))
            if (TranslucentTexels.present(image, box[0], box[1], box[2], box[3], box[4])) return true;
        return false;
    }

    /** Whether a 64x64 skin carries Ears' Alfalfa data, read as Ears reads it. */
    static boolean alfalfa(BufferedImage image) {
        if (image.getWidth() != 64 || image.getHeight() != 64) return false;
        BigInteger data = BigInteger.ZERO;
        int piece = 0;
        for (int[] region : ALFALFA_REGIONS)
            for (int x = region[0]; x < region[2]; x++) for (int y = region[1]; y < region[3]; y++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha == 0) continue;
                data = data.or(
                    BigInteger.valueOf(127 - (alpha & 127))
                        .shiftLeft(7 * piece++));
            }
        byte[] bytes = data.toByteArray();
        if (bytes.length < 5) return false;
        // A sign byte, then the magic.
        int magic = (bytes[1] & 0xFF) << 24 | (bytes[2] & 0xFF) << 16 | (bytes[3] & 0xFF) << 8 | bytes[4] & 0xFF;
        return magic == ALFALFA_MAGIC;
    }

    /** The base boxes' UV layouts: u, v, width, height, depth. */
    private static int[][] boxes(SkinModel model) {
        int arm = model == SkinModel.SLIM ? 3 : 4;
        return new int[][] { { 0, 0, 8, 8, 8 }, { 16, 16, 8, 12, 4 }, { 40, 16, arm, 12, 4 }, { 32, 48, arm, 12, 4 },
            { 0, 16, 4, 12, 4 }, { 16, 48, 4, 12, 4 } };
    }

    /** A box's top and bottom, then its four sides: x, y, width, height. */
    private static int[][] faces(int[] box) {
        int u = box[0], v = box[1], width = box[2], height = box[3], depth = box[4];
        return new int[][] { { u + depth, v, 2 * width, depth }, { u, v + depth, 2 * (width + depth), height } };
    }

    private static final class Entry {

        final long revision;
        final int texture;

        Entry(long revision, int texture) {
            this.revision = revision;
            this.texture = texture;
        }
    }
}
