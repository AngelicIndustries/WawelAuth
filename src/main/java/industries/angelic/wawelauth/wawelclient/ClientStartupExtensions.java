package industries.angelic.wawelauth.wawelclient;

import net.minecraft.client.Minecraft;

import industries.angelic.wawelauth.wawelclient.http.YggdrasilHttpClient;
import industries.angelic.wawelauth.wawelclient.storage.ClientAccountDAO;
import industries.angelic.wawelauth.wawelclient.storage.ClientProviderDAO;

/**
 * Optional client integrations that are not present in every distribution build.
 */
public final class ClientStartupExtensions {

    private ClientStartupExtensions() {}

    public static void start(YggdrasilHttpClient httpClient, ClientProviderDAO providerDAO, ClientAccountDAO accountDAO,
        AccountManager accountManager) {
        LauncherAccountImport.start(
            httpClient,
            providerDAO,
            accountDAO,
            accountManager,
            Minecraft.getMinecraft()
                .getSession());
    }

    public static void stop() {
        LauncherAccountImport.stop();
    }
}
