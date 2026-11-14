package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.wawelclient.lan.LanWorld;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChatE4mc {

    /** e4mc posts the address it got for the world here and keeps it nowhere else. */
    @Inject(method = "printChatMessage", at = @At("HEAD"))
    private void wawelauth$noteE4mcAddress(IChatComponent message, CallbackInfo ci) {
        LanWorld.noteChat(message);
    }
}
