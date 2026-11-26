package industries.angelic.wawelauth.wawelclient;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Which textures an account may upload, and so clear, on its provider. Offline accounts keep both as local files;
 * Microsoft takes skins only; every other provider says it in the profile's {@code uploadableTextures} property, as
 * authlib-injector specifies: without it, nothing can be uploaded.
 */
public final class TextureUploadSupport {

    public static final TextureUploadSupport NONE = new TextureUploadSupport(false, false);
    public static final TextureUploadSupport SKIN_ONLY = new TextureUploadSupport(true, false);
    public static final TextureUploadSupport CAPE_ONLY = new TextureUploadSupport(false, true);
    public static final TextureUploadSupport ALL = new TextureUploadSupport(true, true);

    private final boolean skin;
    private final boolean cape;

    private TextureUploadSupport(boolean skin, boolean cape) {
        this.skin = skin;
        this.cape = cape;
    }

    public boolean skin() {
        return skin;
    }

    public boolean cape() {
        return cape;
    }

    /** From a session server's profile response: its {@code uploadableTextures} property, a list like "skin,cape". */
    public static TextureUploadSupport fromProfile(JsonObject profile) {
        if (profile == null || !profile.has("properties")
            || !profile.get("properties")
                .isJsonArray()) {
            return NONE;
        }
        for (JsonElement element : profile.getAsJsonArray("properties")) {
            if (!element.isJsonObject()) continue;
            JsonObject property = element.getAsJsonObject();
            if (!property.has("name") || !property.has("value")) continue;
            try {
                if (!"uploadableTextures".equals(
                    property.get("name")
                        .getAsString())) {
                    continue;
                }
                boolean skin = false;
                boolean cape = false;
                for (String type : property.get("value")
                    .getAsString()
                    .split(",")) {
                    String name = type.trim()
                        .toLowerCase(Locale.ROOT);
                    skin |= name.equals("skin");
                    cape |= name.equals("cape");
                }
                return skin ? cape ? ALL : SKIN_ONLY : cape ? CAPE_ONLY : NONE;
            } catch (RuntimeException e) {
                return NONE;
            }
        }
        return NONE;
    }
}
