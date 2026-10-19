package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.wawelauth.api.SkinLayersHelper;

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
