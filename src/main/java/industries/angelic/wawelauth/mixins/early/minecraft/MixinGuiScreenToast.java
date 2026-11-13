package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.client.gui.AccountToast;

@Mixin(GuiScreen.class)
public class MixinGuiScreenToast {

    /** A click on Wawel Auth's account toast opens the account manager instead of reaching the screen below. */
    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void wawelauth$clickAccountToast(CallbackInfo ci) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0
            && AccountToast.click(Mouse.getEventX(), Mouse.getEventY())) {
            ci.cancel();
        }
    }
}
