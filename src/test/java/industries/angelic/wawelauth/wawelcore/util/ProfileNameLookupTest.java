package industries.angelic.wawelauth.wawelcore.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.mojang.authlib.GameProfile;

public class ProfileNameLookupTest {

    private static final String ID = "069a79f444e94726a5befca90e38aaf5";
    private static final UUID UUID_ = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    public void serverFallbackUrlsKeepTheirOrder() {
        List<String> urls = ProfileNameLookup.candidateUrls(
            null,
            "https://api.minecraftservices.com",
            "https://example.org/authserver",
            "https://example.org/sessionserver");

        Assert.assertEquals(
            Arrays.asList(
                "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname",
                "https://api.minecraftservices.com/api/profiles/minecraft",
                "https://example.org/authserver/minecraft/profile/lookup/bulk/byname",
                "https://example.org/authserver/api/profiles/minecraft",
                "https://example.org/sessionserver/minecraft/profile/lookup/bulk/byname",
                "https://example.org/sessionserver/api/profiles/minecraft",
                "https://example.org/minecraft/profile/lookup/bulk/byname",
                "https://example.org/api/profiles/minecraft"),
            urls);
    }

    @Test
    public void apiRootComesFirst() {
        List<String> urls = ProfileNameLookup.candidateUrls("https://drasl.example/authlib-injector", null, null, null);

        Assert.assertEquals("https://drasl.example/authlib-injector/api/profiles/minecraft", urls.get(0));
    }

    @Test
    public void followsTheApiLocationHeader() throws Exception {
        List<String> asked = new ArrayList<>();
        GameProfile profile = ProfileNameLookup
            .lookup("notch", Collections.singletonList("https://example.org/start"), (url, body) -> {
                asked.add(url);
                if (url.equals("https://example.org/start")) {
                    return new ProfileNameLookup.Response(404, null, "https://example.org/api-root/");
                }
                return new ProfileNameLookup.Response(200, "[{\"id\":\"" + ID + "\",\"name\":\"Notch\"}]", null);
            });

        Assert.assertEquals(
            Arrays.asList("https://example.org/start", "https://example.org/api-root/api/profiles/minecraft"),
            asked);
        Assert.assertEquals(UUID_, profile.getId());
        Assert.assertEquals("Notch", profile.getName());
    }

    @Test
    public void unknownNameIsNull() throws Exception {
        GameProfile profile = ProfileNameLookup.lookup(
            "nobody",
            Arrays.asList("https://a.example/x", "https://b.example/x"),
            (url, body) -> new ProfileNameLookup.Response(200, "[]", null));

        Assert.assertNull(profile);
    }

    @Test
    public void exactNameWinsOverOthers() {
        GameProfile profile = ProfileNameLookup.parseResponse(
            "{\"profiles\":[{\"id\":\"00000000000000000000000000000001\",\"name\":\"Other\"}," + "{\"id\":\""
                + ID
                + "\",\"name\":\"Notch\"}]}",
            "NOTCH");

        Assert.assertEquals(UUID_, profile.getId());
    }
}
