package industries.angelic.wawelauth.client.render;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import industries.angelic.wawelauth.wawelclient.WawelClient;

/**
 * Stands in for vanilla's skin download by player name, from skins.minecraft.net, which is gone. It draws the skin
 * Wawel Auth finds for the name, and the fallback until then. Mods such as Model Citizens use that path.
 */
public class NameSkinTexture extends ThreadDownloadImageData {

    private final String name;
    /** Loading uploads the fallback to this texture's id, which must then be its own. */
    private boolean loading;

    public NameSkinTexture(String name, ResourceLocation fallback) {
        super(null, null, fallback, null);
        this.name = name;
    }

    /** Vanilla would start the download from the dead URL here. */
    @Override
    protected void func_152433_a() {}

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        loading = true;
        try {
            super.loadTexture(resourceManager);
        } finally {
            loading = false;
        }
    }

    /** The found skin's texture, so binding this one draws it; the fallback's until then, and while loading. */
    @Override
    public int getGlTextureId() {
        if (!loading) {
            WawelClient client = WawelClient.instance();
            ResourceLocation skin = client == null ? null
                : client.getNameProfileResolver()
                    .skin(name);
            if (skin != null) {
                ITextureObject texture = Minecraft.getMinecraft()
                    .getTextureManager()
                    .getTexture(skin);
                if (texture != null && texture != this) return texture.getGlTextureId();
            }
        }
        return super.getGlTextureId();
    }
}
