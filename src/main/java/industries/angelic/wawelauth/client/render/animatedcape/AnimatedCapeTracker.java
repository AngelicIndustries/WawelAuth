package industries.angelic.wawelauth.client.render.animatedcape;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.WawelAuth;

/**
 * Client-side singleton that maps player UUIDs to their animated cape textures.
 * <p>
 * Used by the AbstractClientPlayer mixin to override getLocationCape() for
 * players with animated capes.
 */
@SideOnly(Side.CLIENT)
public final class AnimatedCapeTracker {

    private static final Map<UUID, AnimatedCapeTexture> CAPES = new ConcurrentHashMap<>();
    /**
     * Downloaded capes that turned out to be animated, at their own texture location, which every player wearing that
     * cape binds. Kept across worlds, like other downloaded skin textures.
     */
    private static final Map<ResourceLocation, AnimatedCapeTexture> SHARED = new ConcurrentHashMap<>();

    private AnimatedCapeTracker() {}

    public static void register(UUID uuid, AnimatedCapeTexture texture) {
        AnimatedCapeTexture previous = CAPES.put(uuid, texture);
        // Same location means createFromDecoded already replaced the GL texture.
        if (previous != null && !previous.getResourceLocation()
            .equals(texture.getResourceLocation())) {
            previous.delete();
        }
        WawelAuth.debug("Registered animated cape for " + uuid);
    }

    public static void remove(UUID uuid) {
        AnimatedCapeTexture removed = CAPES.remove(uuid);
        if (removed != null) {
            removed.delete();
            WawelAuth.debug("Removed animated cape for " + uuid);
        }
    }

    /** A downloaded cape animated in place of its still first frame. */
    public static void registerShared(ResourceLocation location, AnimatedCapeTexture texture) {
        SHARED.put(location, texture);
        WawelAuth.debug("Animating downloaded cape " + location);
    }

    public static boolean isShared(ResourceLocation location) {
        return SHARED.containsKey(location);
    }

    public static AnimatedCapeTexture get(UUID uuid) {
        return CAPES.get(uuid);
    }

    public static boolean has(UUID uuid) {
        return CAPES.containsKey(uuid);
    }

    /** Ticks all active animated cape textures. Call once per client tick. */
    public static void tickAll() {
        for (AnimatedCapeTexture tex : CAPES.values()) {
            tex.tick();
        }
        for (AnimatedCapeTexture tex : SHARED.values()) {
            tex.tick();
        }
    }

    /** Clears all tracked capes and frees their GL textures. Call on world unload. */
    public static void clearAll() {
        if (CAPES.isEmpty()) {
            return;
        }
        WawelAuth.debug("Clearing " + CAPES.size() + " animated cape(s)");
        for (Iterator<AnimatedCapeTexture> it = CAPES.values()
            .iterator(); it.hasNext();) {
            it.next()
                .delete();
            it.remove();
        }
    }
}
