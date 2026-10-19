package industries.angelic.wawelauth.client.skin;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.skin.PlayerSkin;
import industries.angelic.wawelauth.client.render.LocalTextureLoader;
import industries.angelic.wawelauth.client.render.ProviderThreadDownloadImageData;
import industries.angelic.wawelauth.client.render.SkinModelHelper;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorThreadDownloadImageData;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/**
 * Builds {@link PlayerSkin} snapshots. A player's snapshot is rebuilt under a new revision whenever its texture
 * location, texture object, loaded image or model changes, which covers downloads finishing, server refreshes,
 * uploads and replaced local skins without hooking each of them.
 */
@SideOnly(Side.CLIENT)
public final class SkinSnapshots {

    private static final RevisionTracker<UUID, Source, PlayerSkin> TRACKER = new RevisionTracker<>(512);
    private static final Map<ResourceLocation, BufferedImage> RESOURCE_IMAGES = new HashMap<>();

    private static boolean registered;

    private SkinSnapshots() {}

    public static synchronized void register() {
        if (registered) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new SkinSnapshots());
        registered = true;
    }

    public static PlayerSkin get(AbstractClientPlayer player) {
        UUID profileId = player.getUniqueID();
        ResourceLocation texture = player.getLocationSkin();
        ITextureObject textureObject = Minecraft.getMinecraft()
            .getTextureManager()
            .getTexture(texture);
        BufferedImage normalized = normalizedImage(texture, textureObject);
        BufferedImage original = originalImage(textureObject, normalized);
        SkinModel model = SkinModelHelper.getSkinModel(player);

        Source source = new Source(texture, textureObject, normalized, model);
        return TRACKER.get(
            profileId,
            source,
            revision -> new SkinSnapshot(profileId, revision, texture, model, original, normalized));
    }

    private static BufferedImage normalizedImage(ResourceLocation texture, ITextureObject textureObject) {
        if (textureObject instanceof ProviderThreadDownloadImageData provider) {
            return provider.bufferedImage;
        }
        BufferedImage local = LocalTextureLoader.getCachedImage(texture);
        if (local != null) {
            return local;
        }
        if ((Object) textureObject instanceof AccessorThreadDownloadImageData download) {
            return download.wawelauth$getBufferedImage();
        }
        return textureObject != null ? resourceImage(texture) : null;
    }

    /** Only provider downloads keep the pre-processing image; local and resource skins are not processed. */
    private static BufferedImage originalImage(ITextureObject textureObject, BufferedImage normalized) {
        if (textureObject instanceof ProviderThreadDownloadImageData provider) {
            BufferedImage original = provider.getOriginalImage();
            if (original != null) return original;
        }
        return normalized;
    }

    /** Default skins come from resource packs. Remembered per location, including failures. */
    private static BufferedImage resourceImage(ResourceLocation texture) {
        if (RESOURCE_IMAGES.containsKey(texture)) {
            return RESOURCE_IMAGES.get(texture);
        }
        BufferedImage image = null;
        try (InputStream input = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(texture)
            .getInputStream()) {
            image = ImageIO.read(input);
        } catch (Exception ignored) {
            // Not a resource texture, or a resource pack without it: no pixels for this skin.
        }
        RESOURCE_IMAGES.put(texture, image);
        return image;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                TRACKER.clear();
                RESOURCE_IMAGES.clear();
            });
    }

    /** What a snapshot was built from. Texture objects and images compare by identity. */
    private static final class Source {

        private final ResourceLocation texture;
        private final ITextureObject textureObject;
        private final BufferedImage image;
        private final SkinModel model;

        Source(ResourceLocation texture, ITextureObject textureObject, BufferedImage image, SkinModel model) {
            this.texture = texture;
            this.textureObject = textureObject;
            this.image = image;
            this.model = model;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Source other)) return false;
            return textureObject == other.textureObject && image == other.image
                && model == other.model
                && (texture == null ? other.texture == null : texture.equals(other.texture));
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(textureObject) * 31 + System.identityHashCode(image);
        }
    }

    private static final class SkinSnapshot implements PlayerSkin {

        private final UUID profileId;
        private final long revision;
        private final ResourceLocation texture;
        private final SkinModel model;
        private final BufferedImage original;
        private final BufferedImage normalized;

        SkinSnapshot(UUID profileId, long revision, ResourceLocation texture, SkinModel model, BufferedImage original,
            BufferedImage normalized) {
            this.profileId = profileId;
            this.revision = revision;
            this.texture = texture;
            this.model = model;
            this.original = original;
            this.normalized = normalized;
        }

        @Override
        public UUID profileId() {
            return profileId;
        }

        @Override
        public long revision() {
            return revision;
        }

        @Override
        public boolean available() {
            return normalized != null;
        }

        @Override
        public ResourceLocation texture() {
            return texture;
        }

        @Override
        public SkinModel model() {
            return model;
        }

        @Override
        public BufferedImage originalImage() {
            return SkinImages.copy(original);
        }

        @Override
        public BufferedImage normalizedImage() {
            return SkinImages.copy(normalized);
        }
    }
}
