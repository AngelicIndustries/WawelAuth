package industries.angelic.wawelauth.mixins.early.authlib;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.client.render.animatedcape.AnimatedCapeTexture;
import industries.angelic.wawelauth.client.render.animatedcape.AnimatedCapeTracker;
import industries.angelic.wawelauth.compat.authlibinjector.AuthlibInjectorCompat;
import industries.angelic.wawelauth.compat.elyauthlib.ElyAuthlibCompat;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;
import industries.angelic.wawelauth.wawelclient.http.ProviderRoutedHttp;
import industries.angelic.wawelauth.wawelcore.crypto.PropertySigner;

/**
 * Mixin into authlib's YggdrasilMinecraftSessionService.
 * remap = false because authlib is not obfuscated.
 *
 * Hooks: signature verification, domain whitelisting, profile fetching,
 * animated cape detection, and texture reading under Ely.by's authlib.
 */
@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public class MixinYggdrasilMinecraftSessionService {

    /** Redirect signature check to use provider context key, falling back to Mojang. */
    @Redirect(
        method = "getTextures",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/properties/Property;isSignatureValid(Ljava/security/PublicKey;)Z"))
    private boolean wawelauth$verifyScopedKey(Property property, PublicKey mojangKey, GameProfile profile,
        boolean requireSecure) {
        WawelClient client = WawelClient.instance();
        if (client == null) {
            return property.isSignatureValid(mojangKey);
        }

        // Check vanilla Mojang key first if allowed
        if (mojangKey != null && client.getSessionBridge()
            .isVanillaTextureTrustAllowed() && wawelauth$verifyProperty(property, mojangKey)) {
            return true;
        }

        // Check provider-scoped keys
        for (PublicKey key : client.getSessionBridge()
            .getTextureVerificationKeys()) {
            if (wawelauth$verifyProperty(property, key)) return true;
        }
        return false;
    }

    /**
     * Verify a property signature against a specific key.
     * When authlib-injector is active, Property.isSignatureValid ignores the key parameter,
     * so we verify directly using PropertySigner to bypass the hijacked method.
     */
    private static boolean wawelauth$verifyProperty(Property property, PublicKey key) {
        if (AuthlibInjectorCompat.isActive()) {
            String signature = property.getSignature();
            if (signature == null) return false;
            return PropertySigner.verifyWithKey(property.getValue(), signature, key);
        }
        return property.isSignatureValid(key);
    }

    /** Redirect domain whitelist to use provider context domains, falling back to vanilla. */
    @Redirect(
        method = "getTextures",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService;isWhitelistedDomain(Ljava/lang/String;)Z"))
    private boolean wawelauth$checkScopedDomains(String url, GameProfile profile, boolean requireSecure) {
        WawelClient client = WawelClient.instance();
        if (client == null) {
            try {
                String host = new java.net.URI(url).getHost();
                return host != null && (host.endsWith(".minecraft.net") || host.endsWith(".mojang.com"));
            } catch (Exception e) {
                return false;
            }
        }
        return client.getSessionBridge()
            .isWhitelistedDomain(url);
    }

    /** Route fillProfileProperties through the active provider's session server. */
    @Inject(method = "fillProfileProperties", at = @At("HEAD"), cancellable = true)
    private void wawelauth$fillFromProvider(GameProfile profile, boolean requireSecure,
        CallbackInfoReturnable<GameProfile> cir) {
        if (profile.getId() == null) return;

        WawelClient client = WawelClient.instance();
        if (client == null) return;

        // ThreadLocal provider context (set by WawelTextureResolver.doFetch)
        ClientProvider providerContext = client.getSessionBridge()
            .getActiveProviderContext();
        if (providerContext != null) {
            GameProfile filled = client.getSessionBridge()
                .fillProfileFromProvider(providerContext, profile, requireSecure);
            if (filled != null) {
                copyFetchedProperties(profile, filled);
                cir.setReturnValue(profile);
            }
            return;
        }

        // In-world: resolver handles profile fetching with correct provider
        if (net.minecraft.client.Minecraft.getMinecraft().theWorld != null) {
            return;
        }
        // Not in-world: fall through to vanilla Mojang
    }

    private static void copyFetchedProperties(GameProfile profile, GameProfile filled) {
        if (profile == null || filled == null || filled == profile) {
            return;
        }
        profile.getProperties()
            .clear();
        profile.getProperties()
            .putAll(filled.getProperties());
    }

    /**
     * Ely.by's authlib hands this method to its own texture service, which checks no signature or skin domain and
     * prefers Ely.by's skin for the player's name over the textures the profile carries. With Wawel Auth running,
     * read the textures as the vanilla authlib does, with the checks above.
     */
    @Inject(method = "getTextures", at = @At("HEAD"), cancellable = true)
    private void wawelauth$readTexturesUnderElyAuthlib(GameProfile profile, boolean requireSecure,
        CallbackInfoReturnable<Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> cir) {
        if (!ElyAuthlibCompat.isActive()) return;
        WawelClient client = WawelClient.instance();
        if (client == null) return;

        Property property = Iterables.getFirst(
            profile.getProperties()
                .get("textures"),
            null);
        // Without textures or a provider, vanilla shows the default skin; Ely.by's skin for the name only adds to it.
        if (property == null && client.getSessionBridge()
            .getActiveProviderContext() == null) return;

        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = wawelauth$vanillaTextures(
            property,
            profile,
            requireSecure);
        wawelauth$trackAnimatedCape(profile, textures);
        cir.setReturnValue(textures);
    }

    /** The vanilla authlib's getTextures, with the redirects above in place of its checks. */
    private Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> wawelauth$vanillaTextures(Property property,
        GameProfile profile, boolean requireSecure) {
        if (property == null) return new HashMap<>();
        if (requireSecure) {
            if (!property.hasSignature()) {
                WawelAuth.LOG.error("Signature is missing from textures payload");
                throw new InsecureTextureException("Signature is missing from textures payload");
            }
            if (!wawelauth$verifyScopedKey(property, ElyAuthlibCompat.mojangKey(), profile, requireSecure)) {
                WawelAuth.LOG.error("Textures payload has been tampered with (signature invalid)");
                throw new InsecureTextureException("Textures payload has been tampered with (signature invalid)");
            }
        }

        MinecraftTexturesPayload payload;
        try {
            payload = ElyAuthlibCompat.decodePayload(property.getValue());
        } catch (JsonParseException e) {
            WawelAuth.LOG.error("Could not decode textures payload", e);
            return new HashMap<>();
        }
        if (payload == null || payload.getTextures() == null) return new HashMap<>();

        for (MinecraftProfileTexture texture : payload.getTextures()
            .values()) {
            if (!wawelauth$checkScopedDomains(texture.getUrl(), profile, requireSecure)) {
                WawelAuth.LOG.error("Textures payload has been tampered with (non-whitelisted domain)");
                return new HashMap<>();
            }
        }
        return payload.getTextures();
    }

    /** Detect animated cape metadata from getTextures() result. */
    @Inject(method = "getTextures", at = @At("RETURN"))
    private void wawelauth$detectAnimatedCape(GameProfile profile, boolean requireSecure,
        CallbackInfoReturnable<Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> cir) {
        wawelauth$trackAnimatedCape(profile, cir.getReturnValue());
    }

    private void wawelauth$trackAnimatedCape(GameProfile profile,
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> result) {
        if (profile == null || profile.getId() == null) return;

        if (result == null || !result.containsKey(MinecraftProfileTexture.Type.CAPE)) return;

        UUID uuid = profile.getId();

        if (AnimatedCapeTracker.has(uuid)) return;

        WawelClient client = WawelClient.instance();
        if (client == null) return;

        try {
            for (Property prop : profile.getProperties()
                .get("textures")) {
                String decoded = new String(
                    Base64.getDecoder()
                        .decode(prop.getValue()),
                    StandardCharsets.UTF_8);
                JsonObject root = new JsonParser().parse(decoded)
                    .getAsJsonObject();
                if (!root.has("textures")) continue;

                JsonObject textures = root.getAsJsonObject("textures");
                if (!textures.has("CAPE")) continue;

                JsonObject capeObj = textures.getAsJsonObject("CAPE");
                if (!capeObj.has("metadata")) continue;

                JsonElement metaElem = capeObj.get("metadata");
                if (!metaElem.isJsonObject()) continue;

                JsonObject meta = metaElem.getAsJsonObject();
                if (!meta.has("animated")) continue;

                String animatedVal = meta.get("animated")
                    .getAsString();
                if (!"true".equals(animatedVal)) continue;

                String capeUrl = capeObj.get("url")
                    .getAsString();
                WawelAuth.debug("Detected animated cape for " + uuid + " at " + capeUrl);

                // Use ThreadLocal provider context, or resolve from connection
                ClientProvider provider = client.getSessionBridge()
                    .getActiveProviderContext();
                if (provider == null) {
                    provider = client.resolvePlayerProvider(uuid);
                }
                final ClientProvider downloadProvider = provider;

                final String locationPath = "capes/ingame/" + uuid.toString()
                    .replace("-", "");
                CompletableFuture.supplyAsync(() -> {
                    try {
                        byte[] gifBytes = wawelauth$downloadBytes(capeUrl, downloadProvider);
                        return AnimatedCapeTexture.decodeGif(gifBytes);
                    } catch (Exception e) {
                        WawelAuth.debug("Failed to download animated cape for " + uuid + ": " + e.getMessage());
                        return null;
                    }
                })
                    .whenComplete((decoded_, err) -> {
                        if (decoded_ == null) return;
                        Minecraft.getMinecraft()
                            .func_152344_a(() -> {
                                AnimatedCapeTexture tex = AnimatedCapeTexture.createFromDecoded(decoded_, locationPath);
                                if (tex != null) {
                                    AnimatedCapeTracker.register(uuid, tex);
                                }
                            });
                    });

                break;
            }
        } catch (Exception e) {
            WawelAuth.debug("Failed to parse animated cape metadata: " + e.getMessage());
        }
    }

    private static byte[] wawelauth$downloadBytes(String urlStr, ClientProvider provider) throws Exception {
        return ProviderRoutedHttp
            .downloadBytes(urlStr, provider, 10_000, 15_000, "WawelAuth", "Animated cape download");
    }
}
