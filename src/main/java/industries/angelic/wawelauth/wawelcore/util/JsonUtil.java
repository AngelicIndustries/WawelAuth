package industries.angelic.wawelauth.wawelcore.util;

import com.google.gson.JsonObject;

/**
 * Shared Gson utilities used across the WawelAuth codebase.
 */
public final class JsonUtil {

    private JsonUtil() {}

    /**
     * Safely extracts a string field from a JsonObject.
     *
     * @return the string value, or null if the field is missing, null, or not a string
     */
    public static String getString(JsonObject obj, String key) {
        if (obj == null || key == null
            || !obj.has(key)
            || obj.get(key)
                .isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key)
                .getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
