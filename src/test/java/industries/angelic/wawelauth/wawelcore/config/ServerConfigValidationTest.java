package industries.angelic.wawelauth.wawelcore.config;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.wawelauth.WawelAuth;

public class ServerConfigValidationTest {

    @Test
    public void defaultConfigIsValid() {
        new ServerConfig().validateOrThrow();
    }

    @Test
    public void implementationNameUsesModName() {
        Assert.assertEquals(
            WawelAuth.MODNAME,
            new ServerConfig().getMeta()
                .getImplementationName());
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsZeroTlsHandshakeTimeout() {
        ServerConfig config = new ServerConfig();
        config.getHttp()
            .setTlsHandshakeTimeoutSeconds(0);

        config.validateOrThrow();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsExcessiveTlsHandshakeTimeout() {
        ServerConfig config = new ServerConfig();
        config.getHttp()
            .setTlsHandshakeTimeoutSeconds(ServerConfig.MAX_TLS_HANDSHAKE_TIMEOUT_SECONDS + 1);

        config.validateOrThrow();
    }
}
