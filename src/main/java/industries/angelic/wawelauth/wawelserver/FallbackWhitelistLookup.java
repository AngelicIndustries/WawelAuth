package industries.angelic.wawelauth.wawelserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.mojang.authlib.GameProfile;

import industries.angelic.angellib.util.StringUtil;
import industries.angelic.wawelauth.Config;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelcore.config.FallbackServer;
import industries.angelic.wawelauth.wawelcore.config.ServerConfig;
import industries.angelic.wawelauth.wawelcore.data.WawelProfile;
import industries.angelic.wawelauth.wawelcore.util.ProfileNameLookup;

/**
 * Resolves whitelist lookups using explicit fallback provider selection:
 * <username>@<providerName>.
 * <p>
 * providerName must match either:
 * <p>
 * - a fallback entry "name" in server config, or
 * <p>
 * - a local alias ("local", "localauth", "wawelauth", "self") for local profiles.
 */
public final class FallbackWhitelistLookup {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private static final String API_LOCATION_HEADER = "X-Authlib-Injector-API-Location";
    private static final String API_LOCATION_HEADER_ALT = "X-Authlib-Injector-Api-Location";

    private FallbackWhitelistLookup() {}

    public static boolean isQualifiedProviderUsername(String rawInput) {
        QualifiedName qualified = parseQualifiedName(rawInput);
        return qualified != null;
    }

    public static GameProfile resolveQualifiedProfile(String rawInput) {
        QualifiedName qualified = parseQualifiedName(rawInput);
        if (qualified == null) return null;

        if (isLocalProviderAlias(qualified.providerName)) {
            GameProfile localProfile = resolveLocalProfile(qualified.username);
            if (localProfile != null) {
                return localProfile;
            }
            WawelAuth.debug(
                "Local whitelist lookup failed for username '" + qualified.username
                    + "' using provider alias '"
                    + qualified.providerName
                    + "'");
            return null;
        }

        FallbackServer fallback = findFallbackByName(qualified.providerName);
        if (fallback == null) {
            WawelAuth.debug(
                "Whitelist provider '" + qualified.providerName
                    + "' not found for username '"
                    + qualified.username
                    + "'");
            return null;
        }

        try {
            return lookupProfileByName(qualified.username, fallback);
        } catch (IOException e) {
            WawelAuth.LOG.warn(
                "Whitelist provider lookup failed for '{}@{}': {}",
                qualified.username,
                qualified.providerName,
                e.getMessage());
            return null;
        }
    }

    public static String resolveQualifiedProviderKey(String rawInput) {
        QualifiedName qualified = parseQualifiedName(rawInput);
        if (qualified == null) return null;
        if (isLocalProviderAlias(qualified.providerName)) {
            return "local";
        }
        FallbackServer fallback = findFallbackByName(qualified.providerName);
        return fallback == null ? null : StringUtil.trimToNull(fallback.getName());
    }

    /** The session server of the fallback provider a qualified name names; null for local profiles or none. */
    public static String resolveQualifiedSessionUrl(String rawInput) {
        QualifiedName qualified = parseQualifiedName(rawInput);
        if (qualified == null || isLocalProviderAlias(qualified.providerName)) return null;
        FallbackServer fallback = findFallbackByName(qualified.providerName);
        return fallback == null ? null : StringUtil.trimToNull(fallback.getSessionServerUrl());
    }

    public static String getQualifiedUsername(String rawInput) {
        QualifiedName qualified = parseQualifiedName(rawInput);
        return qualified == null ? null : qualified.username;
    }

    public static String getQualifiedProviderName(String rawInput) {
        QualifiedName qualified = parseQualifiedName(rawInput);
        return qualified == null ? null : qualified.providerName;
    }

    private static GameProfile resolveLocalProfile(String username) {
        WawelServer server = WawelServer.instance();
        if (server == null) {
            return null;
        }

        WawelProfile profile = server.getProfileDAO()
            .findByName(username);
        if (profile == null) {
            return null;
        }
        return new GameProfile(profile.getUuid(), profile.getName());
    }

    private static GameProfile lookupProfileByName(String username, FallbackServer fallback) throws IOException {
        List<String> candidateUrls = ProfileNameLookup
            .candidateUrls(null, fallback.getServicesUrl(), fallback.getAccountUrl(), fallback.getSessionServerUrl());
        return ProfileNameLookup.lookup(username, candidateUrls, FallbackWhitelistLookup::post);
    }

    private static ProfileNameLookup.Response post(String url, byte[] payload) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            conn.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload);
            }

            int status = conn.getResponseCode();
            String apiLocation = header(conn, API_LOCATION_HEADER, API_LOCATION_HEADER_ALT);
            String body = status == 200
                ? ProfileNameLookup.readUtf8(conn.getInputStream(), ProfileNameLookup.MAX_JSON_BYTES)
                : null;
            return new ProfileNameLookup.Response(status, body, apiLocation);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static FallbackServer findFallbackByName(String providerName) {
        ServerConfig config = Config.server();
        if (config == null) return null;

        String wanted = StringUtil.trimToNull(providerName);
        if (wanted == null) return null;

        for (FallbackServer fallback : config.getFallbackServers()) {
            if (fallback == null) continue;
            String currentName = StringUtil.trimToNull(fallback.getName());
            if (currentName != null && currentName.equalsIgnoreCase(wanted)) {
                return fallback;
            }
        }
        return null;
    }

    private static QualifiedName parseQualifiedName(String rawInput) {
        String value = StringUtil.trimToNull(rawInput);
        if (value == null) return null;

        int at = value.lastIndexOf('@');
        if (at <= 0 || at >= value.length() - 1) {
            return null;
        }

        String username = StringUtil.trimToNull(value.substring(0, at));
        String provider = StringUtil.trimToNull(value.substring(at + 1));
        if (username == null || provider == null) {
            return null;
        }

        return new QualifiedName(username, provider);
    }

    private static String header(HttpURLConnection conn, String first, String second) {
        String value = conn.getHeaderField(first);
        if (value != null) return value;
        return conn.getHeaderField(second);
    }

    private static boolean isLocalProviderAlias(String providerName) {
        String normalized = StringUtil.trimToNull(providerName);
        if (normalized == null) return false;
        return "local".equalsIgnoreCase(normalized) || "localauth".equalsIgnoreCase(normalized)
            || "wawelauth".equalsIgnoreCase(normalized)
            || "self".equalsIgnoreCase(normalized);
    }

    private static final class QualifiedName {

        final String username;
        final String providerName;

        QualifiedName(String username, String providerName) {
            this.username = username;
            this.providerName = providerName;
        }
    }
}
