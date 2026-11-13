package industries.angelic.wawelauth.wawelclient.http;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class JoinAddressFallbackTest {

    @Test
    public void retriesThroughTheJoinAddressAndKeepsUsingIt() throws IOException {
        JoinAddressFallback.register("http://public.example:50135/auth", "10.0.0.2:25565");
        List<String> tried = new ArrayList<>();
        JoinAddressFallback.Request<String, RuntimeException> request = url -> {
            tried.add(url);
            if (url.startsWith("http://public.example:50135")) throw new SocketTimeoutException("Read timed out");
            return "ok";
        };

        Assert.assertEquals("ok", JoinAddressFallback.send("http://public.example:50135/auth/textures/abc", request));
        Assert
            .assertEquals("ok", JoinAddressFallback.send("http://public.example:50135/auth/sessionserver/x", request));

        Assert.assertEquals(
            Arrays.asList(
                "http://public.example:50135/auth/textures/abc",
                "http://10.0.0.2:25565/auth/textures/abc",
                "http://10.0.0.2:25565/auth/sessionserver/x"),
            tried);
    }

    @Test
    public void doesNotRetryAnAnswer() {
        JoinAddressFallback.register("http://answers.example:50135/auth", "10.0.0.3");
        List<String> tried = new ArrayList<>();
        try {
            JoinAddressFallback.send("http://answers.example:50135/auth/x", url -> {
                tried.add(url);
                throw new IOException("HTTP 404 from " + url);
            });
            Assert.fail();
        } catch (IOException expected) {}

        Assert.assertEquals(1, tried.size());
    }

    @Test
    public void leavesOtherServersAlone() {
        List<String> tried = new ArrayList<>();
        try {
            JoinAddressFallback.send("http://unregistered.example/auth", url -> {
                tried.add(url);
                throw new SocketTimeoutException();
            });
            Assert.fail();
        } catch (IOException expected) {}

        Assert.assertEquals(Arrays.asList("http://unregistered.example/auth"), tried);
    }

    @Test
    public void readsJoinAddresses() {
        Assert.assertEquals("http://play.example:25565", JoinAddressFallback.joinOrigin("Play.Example"));
        Assert.assertEquals("http://10.0.0.2:1234", JoinAddressFallback.joinOrigin("10.0.0.2:1234"));
        Assert.assertEquals("http://[::1]:25565", JoinAddressFallback.joinOrigin("[::1]"));
        Assert.assertEquals("http://[::1]:4000", JoinAddressFallback.joinOrigin("[::1]:4000"));
    }
}
