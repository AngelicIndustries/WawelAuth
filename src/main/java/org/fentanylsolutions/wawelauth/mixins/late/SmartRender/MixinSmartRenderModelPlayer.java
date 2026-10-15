package org.fentanylsolutions.wawelauth.mixins.late.SmartRender;

import net.minecraft.client.model.ModelBiped;

import org.fentanylsolutions.wawelauth.compat.smartmoving.SmartRenderModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smart Render copies the model's parts into its own renderers when it creates its {@code SmartRenderModel}; the parts
 * are rebuilt on the 64x64 layout just before. The main player model is the one built without inflation.
 */
@Pseudo
@Mixin(targets = "net.smart.render.ModelPlayer", remap = false)
public abstract class MixinSmartRenderModelPlayer {

    @Inject(method = "<init>(F)V", at = @At(value = "NEW", target = "net/smart/render/SmartRenderModel"), remap = false)
    private void wawelauth$modernize(float scale, CallbackInfo ci) {
        if (scale == 0.0F) SmartRenderModels.modernize((ModelBiped) (Object) this);
    }
}
