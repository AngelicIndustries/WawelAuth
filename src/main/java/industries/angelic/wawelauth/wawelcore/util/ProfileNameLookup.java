package industries.angelic.wawelauth.wawelcore.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import industries.angelic.angellib.util.NetUtil;
import industries.angelic.angellib.util.StringUtil;

/**
 * Looks up a player's profile by name with a provider's bulk name lookup: Mojang's
 * {@code /minecraft/profile/lookup/bulk/byname} or authlib-injector's {@code /api/profiles/minecraft}. Candidate URLs
 * are tried in order, and an authlib-injector API location header adds that API's URLs.
 */
public final class ProfileNameLookup {

    public static final int MAX_JSON_BYTES = 1_048_576;

    private static final String BULK_BY_NAME = "/minecraft/profile/lookup/bulk/byname";
    private static final String AUTHLIB_INJECTOR = "/api/profiles/minecraft";

    /** Sends one lookup request. */
    public interface Transport {

        Response post(String url, byte[] body) throws IOException;
    }

    public static final class Response {

        final int status;
        final String body;
        final String apiLocation;

        /**
         * @param body        the response body, or null unless the status is 200
         * @param apiLocation the X-Authlib-Injector-API-Location header, or null
         */
        public Response(int status, String body, String apiLocation) {
            this.status = status;
            this.body = body;
            this.apiLocation = apiLocation;
        }
    }

    private ProfileNameLookup() {}

    /** Candidate lookup URLs for a provider's endpoints, any of which may be null. */
    public static List<String> candidateUrls(String apiRoot, String servicesUrl, String accountUrl, String sessionUrl) {
        List<String> urls = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        String root = NetUtil.normalizeHttpUrl(apiRoot);
        String services = NetUtil.normalizeHttpUrl(servicesUrl);
        String account = NetUtil.normalizeHttpUrl(accountUrl);
        String session = NetUtil.normalizeHttpUrl(sessionUrl);

        addIfNew(urls, seen, appendPath(root, AUTHLIB_INJECTOR));
        addIfNew(urls, seen, appendPath(root, BULK_BY_NAME));

        addIfNew(urls, seen, appendPath(services, BULK_BY_NAME));
        addIfNew(urls, seen, appendPath(services, AUTHLIB_INJECTOR));

        addIfNew(urls, seen, appendPath(account, BULK_BY_NAME));
        addIfNew(urls, seen, appendPath(account, AUTHLIB_INJECTOR));

        addIfNew(urls, seen, appendPath(session, BULK_BY_NAME));
        addIfNew(urls, seen, appendPath(session, AUTHLIB_INJECTOR));

        if (account != null && account.endsWith("/authserver")) {
            String accountRoot = account.substring(0, account.length() - "/authserver".length());
            addIfNew(urls, seen, appendPath(accountRoot, BULK_BY_NAME));
            addIfNew(urls, seen, appendPath(accountRoot, AUTHLIB_INJECTOR));
        }

        if (session != null && session.endsWith("/sessionserver")) {
            String sessionRoot = session.substring(0, session.length() - "/sessionserver".length());
            addIfNew(urls, seen, appendPath(sessionRoot, BULK_BY_NAME));
            addIfNew(urls, seen, appendPath(sessionRoot, AUTHLIB_INJECTOR));
        }

        return urls;
    }

    /** The profile for the name, or null when no candidate knows it. Network errors are thrown. */
    public static GameProfile lookup(String username, List<String> candidateUrls, Transport transport)
        throws IOException {
        if (candidateUrls == null || candidateUrls.isEmpty()) {
            return null;
        }

        List<String> urls = new ArrayList<>(candidateUrls);
        Set<String> seen = new LinkedHashSet<>(urls);
        byte[] payload = payload(username);

        for (int i = 0; i < urls.size(); i++) {
            Response response = transport.post(urls.get(i), payload);

            String discoveredApiRoot = NetUtil.normalizeHttpUrl(response.apiLocation);
            if (discoveredApiRoot != null) {
                addIfNew(urls, seen, appendPath(discoveredApiRoot, AUTHLIB_INJECTOR));
                addIfNew(urls, seen, appendPath(discoveredApiRoot, BULK_BY_NAME));
            }

            if (response.status == 200) {
                GameProfile profile = parseResponse(response.body, username);
                if (profile != null) {
                    return profile;
                }
            }
        }

        return null;
    }

    public static String readUtf8(InputStream in, int maxBytes) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException("Response body too large");
                }
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] payload(String username) {
        JsonArray request = new JsonArray();
        request.add(new JsonPrimitive(username));
        return request.toString()
            .getBytes(StandardCharsets.UTF_8);
    }

    static GameProfile parseResponse(String body, String username) {
        if (body == null || body.isEmpty()) return null;

        JsonElement root;
        try {
            root = new JsonParser().parse(body);
        } catch (Exception e) {
            return null;
        }

        if (root == null || root.isJsonNull()) {
            return null;
        }

        if (root.isJsonArray()) {
            return parseProfileArray(root.getAsJsonArray(), username);
        }

        if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("profiles") && object.get("profiles")
                .isJsonArray()) {
                return parseProfileArray(object.getAsJsonArray("profiles"), username);
            }
            return parseSingleProfile(object, username);
        }

        return null;
    }

    private static GameProfile parseProfileArray(JsonArray array, String username) {
        GameProfile first = null;
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) continue;
            GameProfile candidate = parseSingleProfile(element.getAsJsonObject(), username);
            if (candidate == null) continue;
            if (first == null) {
                first = candidate;
            }
            String candidateName = candidate.getName();
            if (candidateName != null && candidateName.equalsIgnoreCase(username)) {
                return candidate;
            }
        }
        return first;
    }

    private static GameProfile parseSingleProfile(JsonObject profile, String requestedUsername) {
        String id = JsonUtil.getString(profile, "id");
        if (id == null) return null;

        UUID uuid;
        try {
            uuid = UUIDTypeAdapter.fromString(id);
        } catch (Exception e) {
            return null;
        }

        String name = StringUtil.trimToNull(JsonUtil.getString(profile, "name"));
        if (name == null) {
            name = requestedUsername;
        }
        return new GameProfile(uuid, name);
    }

    private static void addIfNew(List<String> urls, Set<String> seen, String value) {
        if (value == null) return;
        if (seen.add(value)) {
            urls.add(value);
        }
    }

    private static String appendPath(String base, String suffix) {
        if (base == null || suffix == null) return null;
        return base + suffix;
    }
}
