package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiShareToLan;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.client.gui.LanAccessScreen;
import industries.angelic.wawelauth.wawelclient.lan.LanWorld;

/** Open to LAN also sets which providers' players may join. */
@Mixin(GuiShareToLan.class)
public abstract class MixinGuiShareToLan extends GuiScreen {

    @Unique
    private static final int WAWELAUTH_LAN_ACCESS_BUTTON_ID = 205;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void wawelauth$addLanAccessButton(CallbackInfo ci) {
        this.buttonList.add(
            new GuiButton(
                WAWELAUTH_LAN_ACCESS_BUTTON_ID,
                this.width / 2 - 75,
                124,
                150,
                20,
                GuiText.tr("wawelauth.gui.lan_providers.button")));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void wawelauth$openLanAccess(GuiButton button, CallbackInfo ci) {
        if (button.id == WAWELAUTH_LAN_ACCESS_BUTTON_ID) {
            LanAccessScreen.open(LanWorld.worldDirectory());
            ci.cancel();
        }
    }
}
