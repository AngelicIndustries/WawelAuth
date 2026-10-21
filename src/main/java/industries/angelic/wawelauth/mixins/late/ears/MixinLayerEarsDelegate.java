package industries.angelic.wawelauth.mixins.late.ears;

import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.wawelauth.compat.ears.EarsAnchors;

/**
 * Ears' render delegate, standalone and as FoamFix bundles it, opened by {@code EarsTransformerAccess}: its features go
 * on
 * the bones the player's skin layers were drawn on, and it leaves out the second layer once those are drawn.
 */
@Pseudo
@Mixin(
    targets = { "com.unascribed.ears.LayerEars$1", "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars$1" },
    remap = false)
public abstract class MixinLayerEarsDelegate {

    @Inject(
        method = {
            "doAnchorTo(Lcom/unascribed/ears/common/render/EarsRenderDelegate$BodyPart;Lnet/minecraft/client/model/ModelRenderer;)V",
            "doAnchorTo(Lpl/asie/foamfix/repack/com/unascribed/ears/common/render/EarsRenderDelegate$BodyPart;Lnet/minecraft/client/model/ModelRenderer;)V" },
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void wawelauth$anchorToBones(@Coerce Object part, ModelRenderer modelPart, CallbackInfo ci) {
        if (EarsAnchors.anchor(((Enum<?>) part).name(), modelPart)) ci.cancel();
    }

    @Inject(method = "needsSecondaryLayersDrawn()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void wawelauth$layersAlreadyDrawn(CallbackInfoReturnable<Boolean> cir) {
        if (EarsAnchors.drewLayers()) cir.setReturnValue(false);
    }

    @Inject(method = "isSlim()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void wawelauth$drawnSleeves(CallbackInfoReturnable<Boolean> cir) {
        Boolean slim = EarsAnchors.slim();
        if (slim != null) cir.setReturnValue(slim);
    }
}
