package industries.angelic.wawelauth.mixins.early.minecraft.compat.ears;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.compat.ears.EarsCompat;

/**
 * Ears picks the arm model with Mojang's legacy lookup when a player draw starts. These hooks run after that and
 * before the model is drawn, so WawelAuth's provider-aware slim metadata wins. No-ops without Ears.
 */
@Mixin(RenderPlayer.class)
public class MixinRenderPlayerEars {

    @Inject(
        method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V"))
    private void wawelauth$applyEarsSkinModel(AbstractClientPlayer player, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo ci) {
        EarsCompat.applySkinModel((RenderPlayer) (Object) this, player);
    }

    @Inject(
        method = "renderFirstPersonArm",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V",
            shift = At.Shift.BEFORE))
    private void wawelauth$applyEarsFirstPersonModel(EntityPlayer player, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer clientPlayer) {
            EarsCompat.applySkinModel((RenderPlayer) (Object) this, clientPlayer);
        }
    }
}
