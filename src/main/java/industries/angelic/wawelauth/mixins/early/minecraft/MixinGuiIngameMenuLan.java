package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.client.gui.GuiManageLan;
import industries.angelic.wawelauth.wawelclient.lan.LanWorld;

/** Once the world is open to LAN, the pause menu's Open to LAN button, which vanilla greys out, manages it instead. */
@Mixin(GuiIngameMenu.class)
public abstract class MixinGuiIngameMenuLan extends GuiScreen {

    @Unique
    private static final int WAWELAUTH_SHARE_TO_LAN_BUTTON_ID = 7;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void wawelauth$manageLanButton(CallbackInfo ci) {
        if (LanWorld.server() == null) return;
        for (Object raw : this.buttonList) {
            if (raw instanceof GuiButton button && button.id == WAWELAUTH_SHARE_TO_LAN_BUTTON_ID) {
                button.enabled = true;
                button.displayString = GuiText.tr("wawelauth.gui.lan.manage");
            }
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void wawelauth$openManageLan(GuiButton button, CallbackInfo ci) {
        if (button.id == WAWELAUTH_SHARE_TO_LAN_BUTTON_ID && LanWorld.server() != null) {
            this.mc.displayGuiScreen(new GuiManageLan(this));
            ci.cancel();
        }
    }
}
