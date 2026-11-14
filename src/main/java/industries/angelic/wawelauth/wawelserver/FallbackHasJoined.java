package industries.angelic.wawelauth.wawelserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;

import industries.angelic.angellib.util.NetUtil;
import industries.angelic.angellib.util.StringUtil;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelcore.config.FallbackServer;
import industries.angelic.wawelauth.wawelcore.util.JsonUtil;

/**
 * Asks session servers in turn whether a player joined, as a server does after the login handshake, and fetches
 * profiles from them. Used by the dedicated server's fallbacks and by LAN worlds.
 */
public final class FallbackHasJoined {

    private FallbackHasJoined() {}

    /** The session server that confirmed a player. */
    public static final class Hit {

        public final GameProfile profile;
        public final FallbackServer fallback;
        /** The fallback's session server URL as configured, normalized. */
        public final String sessionUrl;

        Hit(GameProfile profile, FallbackServer fallback, String sessionUrl) {
            this.profile = profile;
            this.fallback = fallback;
            this.sessionUrl = sessionUrl;
        }
    }

    /** The first fallback that confirms the player, or null when none does. */
    public static Hit query(String username, String serverId, List<FallbackServer> fallbacks) {
        if (fallbacks == null || fallbacks.isEmpty()) {
            return null;
        }

        for (FallbackServer fallback : fallbacks) {
            if (fallback == null) continue;

            String rawSessionUrl = NetUtil.normalizeHttpUrl(fallback.getSessionServerUrl());
            if (rawSessionUrl == null) continue;

            String sessionMinecraftBase = ApiUrlUtil.resolveSessionMinecraftBase(rawSessionUrl);
            URL checkUrl = HttpAuthenticationService.constantURL(sessionMinecraftBase + "/hasJoined");
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("username", username);
            args.put("serverId", serverId);
            URL url = HttpAuthenticationService.concatenateURL(checkUrl, HttpAuthenticationService.buildQuery(args));

            String displayName = StringUtil.trimToNull(fallback.getName());
            if (displayName == null) {
                displayName = rawSessionUrl;
            }

            try {
                WawelAuth.debug("Trying fallback hasJoined: " + displayName + " -> " + url);
                GameProfile profile = fetchProfile(url, username);
                if (profile != null) {
                    WawelAuth.debug("Fallback hasJoined hit: " + displayName);
                    return new Hit(profile, fallback, rawSessionUrl);
                }
            } catch (AuthenticationUnavailableException e) {
                WawelAuth.debug("Fallback hasJoined unavailable for " + displayName + ": " + e.getMessage());
            }
        }

        return null;
    }

    /** A player's profile with signed textures from a session server, or null. */
    public static GameProfile profile(String sessionServerUrl, UUID id, String name) {
        String rawSessionUrl = NetUtil.normalizeHttpUrl(sessionServerUrl);
        if (rawSessionUrl == null || id == null) return null;
        URL url = HttpAuthenticationService.constantURL(
            ApiUrlUtil.resolveSessionMinecraftBase(rawSessionUrl) + "/profile/"
                + UUIDTypeAdapter.fromUUID(id)
                + "?unsigned=false");
        try {
            return fetchProfile(url, name);
        } catch (AuthenticationUnavailableException e) {
            WawelAuth.debug("Profile unavailable from " + rawSessionUrl + ": " + e.getMessage());
            return null;
        }
    }

    private static GameProfile fetchProfile(URL url, String fallbackName) throws AuthenticationUnavailableException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            if (status == 204 || status == 403 || status == 404) {
                return null;
            }
            if (status != 200) {
                WawelAuth.debug("Local hasJoined returned non-success HTTP " + status);
                return null;
            }

            String body = readUtf8(conn.getInputStream());
            JsonObject root;
            try {
                root = new JsonParser().parse(body)
                    .getAsJsonObject();
            } catch (Exception e) {
                WawelAuth.debug("Local hasJoined returned invalid JSON: " + e.getMessage());
                return null;
            }

            String id = JsonUtil.getString(root, "id");
            if (id == null || id.isEmpty()) {
                return null;
            }

            UUID uuid;
            try {
                uuid = UUIDTypeAdapter.fromString(id);
            } catch (Exception e) {
                WawelAuth.debug("Local hasJoined returned invalid UUID: " + id);
                return null;
            }

            String name = JsonUtil.getString(root, "name");
            String safeName = StringUtil.trimToNull(name);
            if (safeName == null) {
                safeName = StringUtil.trimToNull(fallbackName);
            }
            if (safeName == null) {
                safeName = "UnknownPlayer";
            }
            GameProfile result = new GameProfile(uuid, safeName);

            JsonArray properties = root.has("properties") && root.get("properties")
                .isJsonArray() ? root.getAsJsonArray("properties") : null;
            if (properties != null) {
                for (JsonElement element : properties) {
                    if (element == null || !element.isJsonObject()) continue;
                    JsonObject property = element.getAsJsonObject();
                    String propName = JsonUtil.getString(property, "name");
                    String propValue = JsonUtil.getString(property, "value");
                    if (propName == null || propValue == null) continue;

                    // 1.7.10 S0CPacketSpawnPlayer needs signature as a mandatory string.
                    // Null signature would crash packet encoding, so skip unsigned.
                    String signature = StringUtil.trimToNull(JsonUtil.getString(property, "signature"));
                    if (signature == null) {
                        continue;
                    }
                    result.getProperties()
                        .put(propName, new Property(propName, propValue, signature));
                }
            }

            return result;
        } catch (IOException e) {
            throw new AuthenticationUnavailableException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readUtf8(InputStream in) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
