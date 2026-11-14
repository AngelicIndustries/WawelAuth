package industries.angelic.wawelauth.mixins.early.minecraft;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelclient.NewWorldAccount;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.WorldAuthSettings;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;

@Mixin(Minecraft.class)
public class MixinMinecraftSingleplayerAccount {

    @Inject(method = "launchIntegratedServer", at = @At("HEAD"))
    private void wawelauth$activateSingleplayerAccount(String folderName, String worldName,
        WorldSettings worldSettingsIn, CallbackInfo ci) {
        WawelClient client = WawelClient.instance();
        if (client == null) {
            return;
        }

        // A world created just now gets the account picked for it, in the folder it really launches in.
        File worldDirectory = WorldAuthSettings.worldDirectory(folderName);
        NewWorldAccount.bindIfCreating(worldDirectory, client.getAccountManager());
        ClientAccount account = WorldAuthSettings.account(worldDirectory, client.getAccountManager());
        if (account == null) {
            account = WorldAuthSettings.oldDefaultAccount(worldDirectory, client.getAccountManager());
        }
        if (account == null) {
            // Without a chosen account, play as the account the launcher started the game with, as on servers.
            long automatic = client.getSessionBridge()
                .findAccountForUnboundServer(null);
            if (automatic < 0) {
                client.getSessionBridge()
                    .clearActiveAccount();
                return;
            }
            client.getSessionBridge()
                .activateAccount(automatic);
            WawelAuth.debug(
                "Prepared automatic singleplayer account " + automatic + " before launching world '" + worldName + "'");
            return;
        }

        client.getSessionBridge()
            .activateAccount(account.getId());
        WawelAuth.debug(
            "Prepared singleplayer account '" + account.getProfileName()
                + "' before launching world '"
                + worldName
                + "'");
    }
}
