package industries.angelic.wawelauth.wawelserver;

import com.google.gson.JsonObject;

import industries.angelic.angellib.services.S00PacketServerInfoModifyService;
import industries.angelic.wawelauth.Config;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelcore.ping.WawelPingPayload;

/**
 * Registers server-side ping capability advertisement via AngelLib.
 */
public final class WawelPingServerHooks {

    private static volatile boolean registered;

    private WawelPingServerHooks() {}

    public static synchronized void register() {
        if (registered) return;

        S00PacketServerInfoModifyService.registerHandler((response, angelLibPresent) -> {
            if (!angelLibPresent) {
                return null;
            }

            String publicKeyBase64 = null;
            WawelServer server = WawelServer.instance();
            if (server != null && server.getKeyManager() != null) {
                publicKeyBase64 = server.getKeyManager()
                    .getPublicKeyBase64();
            }

            JsonObject payload = WawelPingPayload.buildServerPayload(Config.server(), publicKeyBase64);
            return new S00PacketServerInfoModifyService.KeyValue(WawelAuth.MODID, payload);
        });

        registered = true;
        WawelAuth.debug("Registered WawelAuth server ping capability handler");
    }
}
