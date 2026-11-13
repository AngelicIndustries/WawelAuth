package industries.angelic.wawelauth.wawelcore.util;

import org.junit.Assert;
import org.junit.Test;

public class AddressDiagnosticsTest {

    @Test
    public void acceptsWhatJavaAccepts() {
        for (String host : new String[] { "example.com", "1.2.3.4", "localhost", "[::1]", "10.0.0.255", "1.2.3",
            "127.1", "mc2.example.com" }) {
            Assert.assertNull(host, AddressDiagnostics.hostProblem(host));
        }
    }

    @Test
    public void explainsWhatJavaRefuses() {
        Assert.assertEquals("it ends with a dot", AddressDiagnostics.hostProblem("1.2.3.4."));
        Assert.assertEquals("it ends with a dot", AddressDiagnostics.hostProblem("example.com."));
        Assert.assertEquals("part 1 is over 255", AddressDiagnostics.hostProblem("300.1.1.1"));
        Assert.assertEquals("part 4 is over 255", AddressDiagnostics.hostProblem("10.0.0.256"));
        Assert.assertEquals("part 3 is over 65535", AddressDiagnostics.hostProblem("1.2.70000"));
        Assert.assertEquals("it has 5 parts, an IPv4 address has 4", AddressDiagnostics.hostProblem("1.2.3.4.5"));
        Assert.assertEquals("part 2 is empty", AddressDiagnostics.hostProblem("1..2.3"));
        Assert.assertTrue(
            AddressDiagnostics.hostProblem("play.server.2")
                .endsWith("part 1 isn't a number"));
    }

    @Test
    public void findsTheHostInUrls() {
        Assert.assertEquals("it ends with a dot", AddressDiagnostics.problem("http://1.2.3.4.:25565/auth"));
        Assert.assertEquals("part 1 is over 255", AddressDiagnostics.problem("300.1.1.1:25565"));
        Assert.assertNull(AddressDiagnostics.problem("https://user@example.com:8443/api?x=1"));
    }

    @Test
    public void describesTheAddressWithoutShowingIt() {
        String message = AddressDiagnostics.describe("server.json publicBaseUrl", "http://203.0.113.300:25565");

        Assert.assertTrue(message, message.contains("NNN.N.NNN.NNN"));
        Assert.assertFalse(message, message.contains("203"));
        Assert.assertFalse(message, message.contains("113"));
        Assert.assertEquals("aaaa.aaaaaaa.N", AddressDiagnostics.shape("play.example.2"));
    }
}
