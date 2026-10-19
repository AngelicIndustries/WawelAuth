package industries.angelic.wawelauth.mixins.late.dynmap;

import net.minecraft.entity.player.EntityPlayer;

import org.dynmap.forge.DynmapPlugin.ForgePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.wawelauth.compat.dynmap.DynmapSkinUrlResolver;

@Mixin(value = ForgePlayer.class, remap = false)
public abstract class MixinDynmapForgePlayer {

    @Shadow(remap = false)
    private EntityPlayer player;

    @Inject(method = "getSkinURL", at = @At("RETURN"), cancellable = true, remap = false)
    private void wawelauth$resolveLatestSkinUrl(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(DynmapSkinUrlResolver.resolve(player, cir.getReturnValue()));
    }
}
