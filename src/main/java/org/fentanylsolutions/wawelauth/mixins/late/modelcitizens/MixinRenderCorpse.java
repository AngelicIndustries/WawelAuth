package org.fentanylsolutions.wawelauth.mixins.late.modelcitizens;

import net.minecraft.client.model.ModelBiped;

import org.fentanylsolutions.wawelauth.api.skin.DrawKind;
import org.fentanylsolutions.wawelauth.api.skin.LayerDraw;
import org.fentanylsolutions.wawelauth.api.skin.SkinLayers;
import org.fentanylsolutions.wawelauth.client.skin.model.ModelPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

/**
 * Model Citizens draws its figures with a vanilla 64x32 model, part by part. A figure wearing a 64x64 skin gets Wawel
 * Auth's player model instead, chosen from the bound texture's shape, and its overlay layers after the last part.
 * Figures aren't players, so the layers are flat; the vanilla headwear part already drew the hat.
 */
@Pseudo
@Mixin(targets = "net.fybertech.modelcitizens.RenderCorpse", remap = false)
public abstract class MixinRenderCorpse {

    @Unique
    private static final float wawelauth$SCALE = 0.0625F;

    @Shadow(remap = false)
    public ModelBiped modelMain;

    @Unique
    private ModelBiped wawelauth$legacyModel;

    @Unique
    private final ModelPlayer wawelauth$modernModel = new ModelPlayer(0.0F);

    /** Right after the figure's skin is bound and before the model is posed. */
    @Inject(
        method = "func_147500_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fybertech/modelcitizens/RenderCorpse;func_147499_a(Lnet/minecraft/util/ResourceLocation;)V",
            ordinal = 0,
            shift = At.Shift.AFTER),
        remap = false)
    private void wawelauth$pickModel(CallbackInfo ci) {
        if (this.wawelauth$legacyModel == null) this.wawelauth$legacyModel = this.modelMain;
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        this.modelMain = width > 0 && width == height ? this.wawelauth$modernModel : this.wawelauth$legacyModel;
    }

    /** After the seventh part, the headwear. */
    @Inject(
        method = "func_147500_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V",
            ordinal = 6,
            shift = At.Shift.AFTER,
            remap = true),
        remap = false)
    private void wawelauth$layers(CallbackInfo ci, @Local ModelBiped drawn) {
        if (drawn != this.wawelauth$modernModel) return;
        try (LayerDraw draw = SkinLayers.begin(null, DrawKind.WORLD, wawelauth$SCALE)) {
            this.wawelauth$modernModel.drawLayers(draw, wawelauth$SCALE, false);
        }
    }
}
