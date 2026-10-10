package org.fentanylsolutions.wawelauth.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

import org.fentanylsolutions.wawelauth.api.SkinLayersHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

    /** The client settings packet carries the wearer's own cape choice, not an equipment state. */
    @Redirect(
        method = "func_147100_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;setHideCape(IZ)V"))
    private void wawelauth$storeWearerCape(EntityPlayerMP player, int bit, boolean hidden) {
        SkinLayersHelper.setWearerCapeHidden(player, hidden);
    }
}
