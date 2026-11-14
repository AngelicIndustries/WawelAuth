package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.client.FMLClientHandler;
import industries.angelic.wawelauth.client.gui.ServerAccountPickerScreen;
import industries.angelic.wawelauth.wawelclient.IServerDataExt;
import industries.angelic.wawelauth.wawelclient.ServerBindingPersistence;
import industries.angelic.wawelauth.wawelclient.ServerCapabilities;
import industries.angelic.wawelauth.wawelclient.SessionBridge;
import industries.angelic.wawelauth.wawelclient.WawelClient;

/**
 * Joining a server with no account bound and none to pick automatically, though the player has accounts it accepts,
 * asks for one first, instead of joining with the game's own session. Every join from the server list, Direct Connect
 * and LAN worlds comes through here.
 */
@Mixin(value = FMLClientHandler.class, remap = false)
public class MixinFMLClientHandlerChooseAccount {

    @Inject(method = "connectToServer", at = @At("HEAD"), cancellable = true)
    private void wawelauth$chooseAccountFirst(GuiScreen parent, ServerData serverData, CallbackInfo ci) {
        WawelClient client = WawelClient.instance();
        if (client == null || !(serverData instanceof IServerDataExt ext)) {
            return;
        }
        if (ext.getWawelAccountId() >= 0
            && !ServerBindingPersistence.clearMissingBinding(serverData, client.getAccountManager())) {
            return;
        }
        SessionBridge bridge = client.getSessionBridge();
        ServerCapabilities capabilities = ext.getWawelCapabilities();
        if (bridge.findAccountForUnboundServer(capabilities) >= 0 || !bridge.hasAcceptedAccount(capabilities)) {
            return;
        }
        ci.cancel();
        ServerAccountPickerScreen.openForJoin(parent, serverData);
    }
}
