package industries.angelic.wawelauth.mixins.early.minecraft;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.world.storage.SaveFormatComparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.factory.ClientGUI;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.client.gui.AccountManagerScreen;
import industries.angelic.wawelauth.client.gui.LogoButton;
import industries.angelic.wawelauth.client.gui.WorldRowIcons;
import industries.angelic.wawelauth.wawelclient.IGuiSelectWorldExt;
import industries.angelic.wawelauth.wawelclient.SingleplayerAccountPersistence;
import industries.angelic.wawelauth.wawelclient.WawelClient;

@Mixin(GuiSelectWorld.class)
public abstract class MixinGuiSelectWorld extends GuiScreen implements IGuiSelectWorldExt {

    @Shadow
    private List<?> field_146639_s; // GuiSelectWorld.saveList

    @Unique
    private static final int WAWELAUTH_ACCOUNTS_BUTTON_ID = 200;
    @Unique
    private LogoButton wawelauth$logoButton;

    @Override
    public SaveFormatComparator wawelauth$getWorld(int index) {
        List<?> worlds = this.field_146639_s;
        return worlds != null && index >= 0 && index < worlds.size() ? (SaveFormatComparator) worlds.get(index) : null;
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void wawelauth$onInitGui(CallbackInfo ci) {
        // Back from a world's account picker, show what was picked.
        WorldRowIcons.forgetAccounts();
        WorldRowIcons.beginFrame();
        WawelClient client = WawelClient.instance();
        if (client != null) {
            SingleplayerAccountPersistence.clearMissingSelection(client.getAccountManager());
        }
    }

    /** Wawel Auth's logo opening the account manager, as on the server list. */
    @Inject(method = "func_146618_g", at = @At("RETURN"))
    private void wawelauth$addAccountsButton(CallbackInfo ci) {
        this.wawelauth$logoButton = new LogoButton(WAWELAUTH_ACCOUNTS_BUTTON_ID, this.width - 5 - LogoButton.SIZE, 5);
        this.buttonList.add(this.wawelauth$logoButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void wawelauth$onAction(GuiButton button, CallbackInfo ci) {
        if (button.id == WAWELAUTH_ACCOUNTS_BUTTON_ID) {
            ClientGUI.open(new AccountManagerScreen());
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void wawelauth$beginWorldRows(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        WorldRowIcons.beginFrame();
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void wawelauth$drawTooltips(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        List<String> lines = WorldRowIcons.tooltip();
        if (lines != null) {
            this.drawHoveringText(lines, mouseX, mouseY, this.fontRendererObj);
        } else if (this.wawelauth$logoButton != null && this.wawelauth$logoButton.isHovered()) {
            this.drawHoveringText(Collections.singletonList(WawelAuth.MODNAME), mouseX, mouseY, this.fontRendererObj);
        }
    }
}
