package industries.angelic.wawelauth.api.skin;

import java.awt.image.BufferedImage;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/**
 * An immutable snapshot of a player's skin, from {@link PlayerSkins#get}.
 * <p>
 * The revision changes whenever the skin's pixels or model may have changed, such as when a download finishes, the
 * server refreshes the skin, the player uploads a new one or a local skin is replaced. Equal revisions mean equal
 * pixels, so consumers can key caches on {@code (profileId, revision)}. Revisions are unique across all players.
 */
public interface PlayerSkin {

    UUID profileId();

    long revision();

    /** False while the skin is still downloading or could not be loaded; the texture is then a placeholder. */
    boolean available();

    /** The texture bound when rendering this player. */
    ResourceLocation texture();

    SkinModel model();

    /**
     * The image as it was decoded, before WawelAuth fixed up its alpha. Legacy 64x32 local skins are already
     * converted to 64x64. A new copy owned by the caller; null when not available.
     */
    BufferedImage originalImage();

    /**
     * The image as uploaded to the GPU: 64x64, or square HD. A new copy owned by the caller; null when not available.
     */
    BufferedImage normalizedImage();
}
