package industries.angelic.wawelauth.mixins.late.armoredarms;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ArmoredArms' own Wawel Auth arm renderer calls internals that no longer exist, and Wawel Auth draws no arms. Without
 * it, ArmoredArms draws the arm with its regular player renderer, where a skin renderer such as Salamander adds the
 * sleeve.
 */
@Pseudo
@Mixin(targets = "com.artur114.armoredarms.client.integration.wawelauth.EventHandlerWA", remap = false)
public abstract class MixinEventHandlerWA {

    @Inject(method = "initRenderContainersEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void wawelauth$keepRegularRenderer(CallbackInfo ci) {
        ci.cancel();
    }
}
