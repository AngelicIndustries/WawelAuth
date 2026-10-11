package org.fentanylsolutions.wawelauth.mixins.early.minecraft;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.wawelauth.api.skin.DrawKind;
import org.fentanylsolutions.wawelauth.api.skin.LayerDraw;
import org.fentanylsolutions.wawelauth.api.skin.SkinLayers;
import org.fentanylsolutions.wawelauth.client.render.SkinModelHelper;
import org.fentanylsolutions.wawelauth.client.render.skinlayers.SkinLayers3DConfig;
import org.fentanylsolutions.wawelauth.client.skin.model.ModelPlayer;
import org.fentanylsolutions.wawelauth.compat.ears.EarsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Installs {@link ModelPlayer} in the vanilla player renderer and keeps it on each drawn player's skin model. */
@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

    @Shadow
    public ModelBiped modelBipedMain;

    /**
     * The constructor's first model becomes both {@code mainModel} and {@code modelBipedMain}. Subclasses that install
     * their own model afterwards keep it. With Ears active, Ears owns the player model.
     */
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/model/ModelBiped", ordinal = 0))
    private static ModelBiped wawelauth$installModelPlayer(float scale) {
        if (!SkinLayers3DConfig.modernSkinSupport || EarsCompat.isRendererActive()) {
            return new ModelBiped(scale);
        }
        return new ModelPlayer(scale);
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"))
    private void wawelauth$applySkinModel(AbstractClientPlayer player, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo ci) {
        if (this.modelBipedMain instanceof ModelPlayer model) {
            model.setSkinModel(SkinModelHelper.getSkinModel(player));
        }
    }

    /**
     * The first-person arm gets its sleeve; the draw stays open around the arm so the arm's renderers can suppress it.
     */
    @WrapMethod(method = "renderFirstPersonArm")
    private void wawelauth$firstPersonSleeve(EntityPlayer player, Operation<Void> original) {
        if (!(this.modelBipedMain instanceof ModelPlayer model) || !(player instanceof AbstractClientPlayer client)) {
            original.call(player);
            return;
        }
        model.setSkinModel(SkinModelHelper.getSkinModel(client));
        try (LayerDraw draw = SkinLayers.begin(client, DrawKind.FIRST_PERSON, 0.0625F)) {
            original.call(player);
            if (!draw.suppressed()) model.drawFirstPersonSleeve(draw, 0.0625F);
        }
    }
}
