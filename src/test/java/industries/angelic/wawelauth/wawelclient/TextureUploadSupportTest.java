package industries.angelic.wawelauth.wawelclient;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TextureUploadSupportTest {

    private static JsonObject profile(String properties) {
        return new JsonParser().parse("{\"id\":\"0\",\"name\":\"a\",\"properties\":[" + properties + "]}")
            .getAsJsonObject();
    }

    @Test
    public void readsTheUploadableTexturesProperty() {
        String textures = "{\"name\":\"textures\",\"value\":\"e30=\"}";
        Assert.assertSame(
            TextureUploadSupport.ALL,
            TextureUploadSupport
                .fromProfile(profile(textures + ",{\"name\":\"uploadableTextures\",\"value\":\"skin,cape\"}")));
        Assert.assertSame(
            TextureUploadSupport.SKIN_ONLY,
            TextureUploadSupport.fromProfile(profile("{\"name\":\"uploadableTextures\",\"value\":\"skin\"}")));
        Assert.assertSame(
            TextureUploadSupport.CAPE_ONLY,
            TextureUploadSupport.fromProfile(profile("{\"name\":\"uploadableTextures\",\"value\":\" Cape \"}")));
    }

    @Test
    public void withoutThePropertyNothingCanBeUploaded() {
        // As Ely.by and LittleSkin answer: only the textures.
        Assert.assertSame(
            TextureUploadSupport.NONE,
            TextureUploadSupport.fromProfile(profile("{\"name\":\"textures\",\"value\":\"e30=\"}")));
        Assert.assertSame(
            TextureUploadSupport.NONE,
            TextureUploadSupport.fromProfile(profile("{\"name\":\"uploadableTextures\",\"value\":\"\"}")));
        Assert.assertSame(TextureUploadSupport.NONE, TextureUploadSupport.fromProfile(null));
    }
}
