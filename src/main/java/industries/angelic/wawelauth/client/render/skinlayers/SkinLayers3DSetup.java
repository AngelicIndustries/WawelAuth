package industries.angelic.wawelauth.client.render.skinlayers;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.client.render.LocalTextureLoader;
import industries.angelic.wawelauth.client.render.ProviderThreadDownloadImageData;
import industries.angelic.wawelauth.client.skin.voxel.ImageVoxelTexture;
import industries.angelic.wawelauth.client.skin.voxel.VoxelBuilder;
import industries.angelic.wawelauth.client.skin.voxel.VoxelCube;
import industries.angelic.wawelauth.client.skin.voxel.VoxelCubeCollector;
import industries.angelic.wawelauth.client.skin.voxel.VoxelMesh;
import industries.angelic.wawelauth.client.skin.voxel.VoxelSurfaceBuilder;

/**
 * Static utility that generates 3D meshes from a player's skin BufferedImage.
 * <p>
 * Handles caching and change detection via ResourceLocation comparison.
 */
public class SkinLayers3DSetup {

    /** Cache for skull meshes keyed by GameProfile. */
    private static final Map<UUID, SkullMeshCache> skullCache = new ConcurrentHashMap<>();

    public static void clearSkullCache() {
        skullCache.values()
            .forEach(cached -> { if (cached.mesh != null) cached.mesh.cleanup(); });
        skullCache.clear();
    }

    public static void updateSkullCache(UUID uuid, SkullMeshCache newCache) {
        if (uuid == null) return;
        SkullMeshCache oldCache = (newCache == null) ? skullCache.remove(uuid) : skullCache.put(uuid, newCache);
        if (oldCache != null && oldCache.mesh != null && oldCache != newCache) {
            oldCache.mesh.cleanup();
        }
    }

    public static SkullMeshCache getSkullCache(UUID uuid) {
        if (uuid == null) return null;
        return skullCache.get(uuid);
    }

    /**
     * Get or create a 3D hat mesh for a player skull.
     *
     * @param profile      the skull's game profile
     * @param skinLocation the skull's skin texture location
     * @return the hat mesh, or null if generation failed
     */
    public static VoxelMesh getOrCreateSkullMesh(GameProfile profile, ResourceLocation skinLocation) {
        if (profile == null || skinLocation == null) return null;

        SkullMeshCache cached = getSkullCache(profile.getId());
        if (cached != null) {
            if (skinLocation.equals(cached.skinLocation)) {
                return cached.mesh;
            }
            // Remove the entry along with the cleanup so a failed rebuild
            // cannot leave a destroyed mesh served under the old location.
            updateSkullCache(profile.getId(), null);
        }

        BufferedImage skinImage = getSkinImage(skinLocation);
        if (skinImage == null || skinImage.getWidth() != 64 || skinImage.getHeight() != 64) {
            return null;
        }

        try {
            ImageVoxelTexture skinData = new ImageVoxelTexture(skinImage);
            VoxelMesh mesh = buildMesh(skinData, 8, 8, 8, 32, 0, false, 0.6f);
            updateSkullCache(profile.getId(), new SkullMeshCache(skinLocation, mesh));
            return mesh;
        } catch (Exception e) {
            WawelAuth.LOG.error("Failed to build 3D skull hat mesh", e);
            return null;
        }
    }

    private static VoxelMesh buildMesh(ImageVoxelTexture skinData, int width, int height, int depth, int textureU,
        int textureV, boolean topPivot, float rotationOffset) {
        VoxelCubeCollector builder = new VoxelCubeCollector();
        VoxelBuilder result = VoxelSurfaceBuilder
            .wrapBox(builder, skinData, width, height, depth, textureU, textureV, topPivot, rotationOffset);
        if (result == null || builder.isEmpty()) {
            return null;
        }
        List<VoxelCube> cubes = builder.getCubes();
        VoxelMesh mesh = new VoxelMesh(cubes);
        mesh.compileDisplayList();
        return mesh;
    }

    /**
     * Extract the BufferedImage from a skin ResourceLocation via the texture
     * manager and the accessor mixin.
     */
    private static BufferedImage getSkinImage(ResourceLocation skinLocation) {
        ITextureObject texture = Minecraft.getMinecraft()
            .getTextureManager()
            .getTexture(skinLocation);
        if (texture instanceof ProviderThreadDownloadImageData providerImageData) {
            return providerImageData.bufferedImage;
        }
        // Offline/local skins are registered as DynamicTexture via LocalTextureLoader
        return LocalTextureLoader.getCachedImage(skinLocation);
    }

    public static class SkullMeshCache {

        final ResourceLocation skinLocation;
        final VoxelMesh mesh;

        SkullMeshCache(ResourceLocation skinLocation, VoxelMesh mesh) {
            this.skinLocation = skinLocation;
            this.mesh = mesh;
        }
    }
}
