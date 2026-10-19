package industries.angelic.wawelauth.mixins.late.serverutilities;

import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.wawelauth.wawelclient.WawelClient;

@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "serverutils.client.tab.TabSkinCache", priority = 999, remap = false)
public abstract class MixinTabSkinCache {

    @Inject(method = "getOrLoadSkin", at = @At("HEAD"), cancellable = true, remap = false)
    private void wawelauth$resolveTabSkin(String playerName, CallbackInfoReturnable<ResourceLocation> cir) {
        WawelClient client = WawelClient.instance();
        if (client == null) return;
        ResourceLocation skin = client.getNameProfileResolver()
            .skin(playerName);
        if (skin != null) {
            cir.setReturnValue(skin);
        }
    }
}
