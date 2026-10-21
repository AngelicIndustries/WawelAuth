package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.client.skin.translucent.TranslucentLayers;

/**
 * Paints the held-back see-through skin layer texels after the translucent terrain and the second entity pass, right
 * before the render pass is reset. Without this, they are drawn in place.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererTranslucentLayers {

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;setRenderPass(I)V",
            ordinal = 3,
            remap = false),
        require = 0)
    private void wawelauth$paintSeeThroughLayers(float partialTicks, long finishTime, CallbackInfo ci) {
        TranslucentLayers.flush();
    }
}
