package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.wawelauth.wawelclient.lan.LanWorld;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServerLan {

    /** Once opened, the world's LAN settings are Wawel Auth's to manage. */
    @Inject(method = "shareToLAN", at = @At("RETURN"))
    private void wawelauth$onOpenedToLan(WorldSettings.GameType type, boolean allowCheats,
        CallbackInfoReturnable<String> cir) {
        if (cir.getReturnValue() != null) {
            LanWorld.onOpened((IntegratedServer) (Object) this, cir.getReturnValue(), allowCheats);
        }
    }
}
