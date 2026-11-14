package industries.angelic.wawelauth.mixins.early.minecraft;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.client.gui.NewWorldFace;
import industries.angelic.wawelauth.wawelclient.NewWorldAccount;

/** The account face on the world creation screen; Create hands its pick to the world's launch. */
@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorld extends GuiScreen {

    @Shadow
    private boolean field_146344_y; // GuiCreateWorld.moreOptions

    @Shadow
    private GuiTextField field_146333_g; // GuiCreateWorld.worldNameField

    @Inject(method = "<init>", at = @At("RETURN"))
    private void wawelauth$startWithoutAPick(GuiScreen parent, CallbackInfo ci) {
        NewWorldAccount.reset();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void wawelauth$clickAccountFace(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton == 0 && !this.field_146344_y
            && NewWorldFace.click(this, mouseX, mouseY, this.field_146333_g.getText())) {
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void wawelauth$drawAccountFace(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (this.field_146344_y) return;
        NewWorldFace.draw(this, mouseX, mouseY);
        List<String> lines = NewWorldFace.tooltip(this, mouseX, mouseY);
        if (lines != null) {
            this.drawHoveringText(lines, mouseX, mouseY, this.fontRendererObj);
        }
    }

    @Inject(
        method = "actionPerformed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;launchIntegratedServer(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V"))
    private void wawelauth$markCreating(GuiButton button, CallbackInfo ci) {
        NewWorldAccount.creating();
    }
}
