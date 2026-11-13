package industries.angelic.wawelauth.wawelclient;

import net.minecraft.client.Minecraft;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelclient.http.YggdrasilHttpClient;
import industries.angelic.wawelauth.wawelclient.storage.ClientAccountDAO;
import industries.angelic.wawelauth.wawelclient.storage.ClientProviderDAO;

/**
 * Optional client integrations that are not present in every distribution build.
 */
public final class ClientStartupExtensions {

    private ClientStartupExtensions() {}

    /** Registers settings in preInit, before GTNHLib drops config sections no mod has registered. */
    public static void preInit() {
        try {
            ConfigurationManager.registerConfig(LauncherImportConfig.class);
        } catch (ConfigException e) {
            WawelAuth.LOG.warn("Could not load the launcher import settings, using the defaults: {}", e.getMessage());
        }
    }

    public static void start(YggdrasilHttpClient httpClient, ClientProviderDAO providerDAO, ClientAccountDAO accountDAO,
        AccountManager accountManager, SessionBridge sessionBridge) {
        LauncherAccountImport.start(
            httpClient,
            providerDAO,
            accountDAO,
            accountManager,
            sessionBridge,
            Minecraft.getMinecraft()
                .getSession());
    }

    public static void stop() {
        LauncherAccountImport.stop();
    }
}
