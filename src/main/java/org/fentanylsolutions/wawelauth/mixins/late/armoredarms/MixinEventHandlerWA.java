package org.fentanylsolutions.wawelauth.mixins.late.armoredarms;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ArmoredArms' own Wawel Auth arm renderer calls internals that no longer exist. Without it, ArmoredArms draws the arm
 * with its regular player renderer, and {@link MixinArmModelRendererPlayer} adds the sleeve.
 */
@Pseudo
@Mixin(targets = "com.artur114.armoredarms.client.integration.wawelauth.EventHandlerWA", remap = false)
public abstract class MixinEventHandlerWA {

    @Inject(method = "initRenderContainersEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void wawelauth$keepRegularRenderer(CallbackInfo ci) {
        ci.cancel();
    }
}
