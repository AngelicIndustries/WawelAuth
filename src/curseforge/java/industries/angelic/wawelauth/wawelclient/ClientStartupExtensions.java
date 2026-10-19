package industries.angelic.wawelauth.wawelclient;

import industries.angelic.wawelauth.wawelclient.http.YggdrasilHttpClient;
import industries.angelic.wawelauth.wawelclient.storage.ClientAccountDAO;
import industries.angelic.wawelauth.wawelclient.storage.ClientProviderDAO;

/**
 * CurseForge distribution: privacy-sensitive launcher integrations are omitted.
 */
public final class ClientStartupExtensions {

    private ClientStartupExtensions() {}

    public static void start(YggdrasilHttpClient httpClient, ClientProviderDAO providerDAO, ClientAccountDAO accountDAO,
        AccountManager accountManager) {}

    public static void stop() {}
}
