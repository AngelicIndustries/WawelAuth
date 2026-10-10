package org.fentanylsolutions.wawelauth.mixins.early.minecraft;

import net.minecraft.client.settings.GameSettings;

import org.fentanylsolutions.wawelauth.client.skin.sync.ClientSkinSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameSettings.class, priority = 999)
public class MixinGameSettings {

    /** The vanilla packet carries the cape; the other parts follow in WA's packet. */
    @Inject(
        method = "sendSettingsToServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/NetHandlerPlayClient;addToSendQueue(Lnet/minecraft/network/Packet;)V",
            shift = At.Shift.AFTER))
    private void sendSettingsToServer(CallbackInfo ci) {
        ClientSkinSync.sendPartsToServer();
    }

}
