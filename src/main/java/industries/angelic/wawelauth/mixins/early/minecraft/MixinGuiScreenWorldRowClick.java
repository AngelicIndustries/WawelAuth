package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.client.gui.WorldRowIcons;

@Mixin(GuiScreen.class)
public class MixinGuiScreenWorldRowClick {

    /**
     * A click on a world's account icons opens its account picker. Caught here rather than in the world list's own
     * mouseClicked, which vanilla doesn't have and Et Futurum adds.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void wawelauth$clickWorldAccount(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton == 0 && (Object) this instanceof GuiSelectWorld && WorldRowIcons.click()) {
            ci.cancel();
        }
    }
}
