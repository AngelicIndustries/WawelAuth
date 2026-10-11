package org.fentanylsolutions.wawelauth.mixins.late.etfuturum;

import net.minecraft.client.entity.AbstractClientPlayer;

import org.fentanylsolutions.wawelauth.compat.etfuturum.EtFuturumCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import ganymedes01.etfuturum.client.renderer.entity.elytra.LayerBetterElytra;

/** func_152122_n (hasCape) has no MCP name, so the target is identical in dev and production. */
@Mixin(value = LayerBetterElytra.class, remap = false)
public class MixinLayerBetterElytra {

    @WrapOperation(
        method = "doRenderLayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;func_152122_n()Z"))
    private static boolean wawelauth$capeTextureOnlyIfShown(AbstractClientPlayer player, Operation<Boolean> original) {
        return EtFuturumCompat.elytraUsesCape(player, original.call(player));
    }
}
