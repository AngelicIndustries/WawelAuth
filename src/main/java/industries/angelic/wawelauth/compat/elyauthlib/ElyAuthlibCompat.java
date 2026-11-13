package industries.angelic.wawelauth.compat.elyauthlib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;

import industries.angelic.wawelauth.WawelAuth;

/**
 * Ely.by's authlib ({@code by.ely:authlib}), which some launchers put on the classpath in place of Mojang's for
 * Ely.by accounts. Its session service hands {@code getTextures} to Ely.by's own texture service, which checks no
 * signature or skin domain and prefers Ely.by's skin for the player's name, so Wawel Auth reads textures itself there.
 */
public final class ElyAuthlibCompat {

    private static final boolean PRESENT;
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
        .create();
    private static volatile PublicKey mojangKey;
    private static volatile boolean mojangKeyLoaded;

    static {
        boolean found;
        try {
            Class.forName("by.ely.authlib.ElyTextureService", false, ElyAuthlibCompat.class.getClassLoader());
            found = true;
        } catch (ClassNotFoundException | LinkageError e) {
            found = false;
        }
        PRESENT = found;
    }

    private ElyAuthlibCompat() {}

    public static boolean isActive() {
        return PRESENT;
    }

    /** A textures property's payload, decoded as the vanilla authlib does. */
    public static MinecraftTexturesPayload decodePayload(String value) {
        return GSON
            .fromJson(new String(Base64.decodeBase64(value), StandardCharsets.UTF_8), MinecraftTexturesPayload.class);
    }

    /**
     * Mojang's session key, which the vanilla authlib checks texture signatures against. Ely.by's authlib still ships
     * the key file but no longer loads it. Null when it can't be read.
     */
    public static PublicKey mojangKey() {
        if (!mojangKeyLoaded) {
            synchronized (ElyAuthlibCompat.class) {
                if (!mojangKeyLoaded) {
                    mojangKey = loadMojangKey();
                    mojangKeyLoaded = true;
                }
            }
        }
        return mojangKey;
    }

    private static PublicKey loadMojangKey() {
        try (InputStream in = YggdrasilMinecraftSessionService.class
            .getResourceAsStream("/yggdrasil_session_pubkey.der")) {
            if (in == null) {
                WawelAuth.LOG.warn("Ely.by's authlib has no Mojang session key, Mojang-signed skins can't be checked");
                return null;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int n; (n = in.read(buffer)) > 0;) {
                bytes.write(buffer, 0, n);
            }
            return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(bytes.toByteArray()));
        } catch (IOException | java.security.GeneralSecurityException e) {
            WawelAuth.LOG.warn("Could not read Mojang's session key from Ely.by's authlib: {}", e.toString());
            return null;
        }
    }
}
