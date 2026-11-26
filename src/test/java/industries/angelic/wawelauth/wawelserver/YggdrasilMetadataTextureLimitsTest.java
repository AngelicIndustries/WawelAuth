package industries.angelic.wawelauth.wawelserver;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.wawelauth.wawelcore.config.ServerConfig;
import industries.angelic.wawelauth.wawelcore.crypto.KeyManager;

public class YggdrasilMetadataTextureLimitsTest {

    @Test
    @SuppressWarnings("unchecked")
    public void metadataTellsWhatTextureUploadsTake() throws Exception {
        File keys = Files.createTempDirectory("wawelauth-metadata-keys")
            .toFile();
        KeyManager keyManager = new KeyManager(keys);
        keyManager.loadOrGenerate();
        ServerConfig config = new ServerConfig();
        config.getTextures()
            .setMaxCapeWidth(256);
        config.getTextures()
            .setMaxCapeHeight(128);

        Method build = YggdrasilRoutes.class.getDeclaredMethod("buildMetadata", ServerConfig.class, KeyManager.class);
        build.setAccessible(true);
        Map<String, Object> root = (Map<String, Object>) build.invoke(null, config, keyManager);
        Map<String, Object> meta = (Map<String, Object>) root.get("meta");
        Map<String, Object> textures = (Map<String, Object>) ((Map<String, Object>) meta.get("wawelauth"))
            .get("textures");

        Assert.assertEquals(256, textures.get("maxCapeWidth"));
        Assert.assertEquals(128, textures.get("maxCapeHeight"));
        Assert.assertEquals(Boolean.TRUE, textures.get("allowAnimatedCapes"));
        Assert.assertEquals(256, textures.get("maxCapeFrameCount"));
        Assert.assertEquals(10_485_760, textures.get("maxAnimatedCapeFileSizeBytes"));
    }
}
