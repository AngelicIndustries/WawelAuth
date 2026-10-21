package industries.angelic.wawelauth.core;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.compat.ears.EarsTransformerAccess;

@SuppressWarnings("unused")
@LateMixin
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins." + WawelAuth.MODID + ".late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        // Before Ears loads its renderer, which happens once mods are constructed.
        if (FMLLaunchHandler.side()
            .isClient()) EarsTransformerAccess.open();
        return Mixins.getLateMixinsForLoader(loadedCoreMods);
    }
}
