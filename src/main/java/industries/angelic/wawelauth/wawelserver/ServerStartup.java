package industries.angelic.wawelauth.wawelserver;

import industries.angelic.wawelauth.WawelAuth;

/**
 * Whether the dedicated server may start. When Wawel Auth's server module can't run as configured, it logs why and
 * refuses: the server then stops as when it fails to start, before loading any world, instead of exiting mid-startup.
 */
public final class ServerStartup {

    private static boolean refused;

    private ServerStartup() {}

    /** Refuses startup, after the reason has been logged. */
    public static void refuse() {
        if (!refused) WawelAuth.LOG.error("Wawel Auth stops the server instead of starting it.");
        refused = true;
    }

    public static boolean refused() {
        return refused;
    }
}
