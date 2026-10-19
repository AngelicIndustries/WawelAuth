package industries.angelic.wawelauth.mixins.early.minecraft;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import industries.angelic.wawelauth.api.WawelTextureResolver;
import industries.angelic.wawelauth.client.render.LocalTextureLoader;
import industries.angelic.wawelauth.client.render.NameSkinTexture;
import industries.angelic.wawelauth.client.render.animatedcape.AnimatedCapeTexture;
import industries.angelic.wawelauth.client.render.animatedcape.AnimatedCapeTracker;
import industries.angelic.wawelauth.wawelclient.BuiltinProviders;
import industries.angelic.wawelauth.wawelclient.SessionBridge;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;

@Mixin(value = AbstractClientPlayer.class, priority = 999)
public class MixinAbstractClientPlayer {

    @Shadow
    @Final
    @Mutable
    public static ResourceLocation locationStevePng;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void wawelauth$useModernSteve(CallbackInfo ci) {
        locationStevePng = new ResourceLocation("wawelauth", "textures/steve_64.png");
    }

    /**
     * Skins by player name, as Model Citizens loads them, came from skins.minecraft.net, which is gone. The name is
     * resolved through Wawel Auth's providers instead. Only the name's own location is taken over: BetterQuesting
     * passes a player's current skin location here, which may be the one Wawel Auth is about to fill.
     */
    @Inject(method = "getDownloadImageSkin", at = @At("HEAD"), cancellable = true)
    private static void wawelauth$skinByName(ResourceLocation location, String name,
        CallbackInfoReturnable<ThreadDownloadImageData> cir) {
        if (WawelClient.instance() == null || name == null) return;
        if (!AbstractClientPlayer.getLocationSkin(name)
            .equals(location)) return;
        TextureManager textures = Minecraft.getMinecraft()
            .getTextureManager();
        ITextureObject existing = textures.getTexture(location);
        if (existing != null) return;
        NameSkinTexture texture = new NameSkinTexture(StringUtils.stripControlCodes(name), locationStevePng);
        textures.loadTexture(location, texture);
        cir.setReturnValue(texture);
    }

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/SkinManager;func_152790_a(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/client/resources/SkinManager$SkinAvailableCallback;Z)V"))
    private void wawelauth$suppressVanillaSkinLoading(SkinManager skinManager, GameProfile profile,
        SkinManager.SkinAvailableCallback callback, boolean requireSecure) {
        // All skin loading goes through WawelTextureResolver. Suppress vanilla loading.
    }

    @Inject(method = "getLocationCape", at = @At("RETURN"), cancellable = true)
    private void wawelauth$overrideGetCape(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        UUID uuid = self.getUniqueID();
        if (uuid == null) return;

        AnimatedCapeTexture animated = AnimatedCapeTracker.get(uuid);
        if (animated != null) {
            cir.setReturnValue(animated.getResourceLocation());
            return;
        }

        WawelClient client = WawelClient.instance();
        if (client == null) return;

        ClientProvider provider = wawelauth$resolveProvider(client, uuid);
        if (provider == null) return;

        ResourceLocation resolved = client.getTextureResolver()
            .getCape(uuid, self.getCommandSenderName(), provider, false);
        if (resolved != null) {
            cir.setReturnValue(resolved);
        }
    }

    @Inject(method = "func_152122_n", at = @At("RETURN"), cancellable = true)
    private void wawelauth$reportCape(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        WawelClient client = WawelClient.instance();
        if (client == null) return;

        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        UUID uuid = self.getUniqueID();
        if (uuid == null) return;

        ClientProvider provider = wawelauth$resolveProvider(client, uuid);
        if (provider == null) return;

        ResourceLocation resolved = client.getTextureResolver()
            .getCape(uuid, self.getCommandSenderName(), provider, false);
        if (resolved != null) {
            cir.setReturnValue(true);
            return;
        }

        // Check for animated GIF cape from offline local skin
        if (BuiltinProviders.isOfflineProvider(provider.getName())) {
            SessionBridge.OfflineLocalSkin local = client.getSessionBridge()
                .resolveOfflineLocalSkin(uuid, provider);
            if (local != null && local.getCapePath() != null) {
                if (LocalTextureLoader.getOfflineGIFCape(uuid, local.getCapePath()) != null) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "getLocationSkin", at = @At("RETURN"), cancellable = true)
    private void wawelauth$overrideSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        WawelClient client = WawelClient.instance();
        if (client == null) return;

        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        UUID uuid = self.getUniqueID();
        if (uuid == null) return;

        ClientProvider provider = wawelauth$resolveProvider(client, uuid);
        if (provider == null) return;

        ResourceLocation resolved = client.getTextureResolver()
            .getSkin(uuid, self.getCommandSenderName(), provider, false);
        if (resolved != null && !resolved.equals(WawelTextureResolver.getDefaultSkin())
            && !resolved.equals(WawelTextureResolver.getLegacyDefaultSkin())) {
            cir.setReturnValue(resolved);
        }
    }

    /** Resolve provider for this player, or null (caller keeps Steve). */
    @Unique
    private static ClientProvider wawelauth$resolveProvider(WawelClient client, UUID uuid) {
        ClientProvider provider = client.resolvePlayerProvider(uuid);
        if (provider != null) {
            return provider;
        }
        // Singleplayer with no explicit account: Mojang for local player
        if (Minecraft.getMinecraft()
            .isSingleplayer() && wawelauth$isLocalPlayer(uuid)) {
            return client.getMojangProvider();
        }
        return null;
    }

    @Unique
    private static boolean wawelauth$isLocalPlayer(UUID uuid) {
        if (uuid == null) return false;
        try {
            GameProfile sessionProfile = Minecraft.getMinecraft()
                .getSession()
                .func_148256_e();
            return sessionProfile != null && uuid.equals(sessionProfile.getId());
        } catch (Exception e) {
            return false;
        }
    }
}
