package industries.angelic.wawelauth.mixins.late.betterquesting;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import betterquesting.network.handlers.NetPartySync;
import industries.angelic.wawelauth.compat.betterquesting.BetterQuestingProviderSync;

@Mixin(value = NetPartySync.class, remap = false)
public abstract class MixinNetPartySync {

    @Inject(method = "sendSync", at = @At("HEAD"), remap = false)
    private static void wawelauth$syncPartyProviders(EntityPlayerMP[] players, int[] partyIDs, CallbackInfo ci) {
        BetterQuestingProviderSync.syncParties(players, partyIDs);
    }
}
