package industries.angelic.wawelauth.wawelclient.http;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import industries.angelic.wawelauth.WawelAuth;

/**
 * A second way to reach a Wawel Auth server's web API: the address the player joins it with. The server serves its
 * API on the game port, so where its advertised address doesn't answer, requests are retried through the join
 * address. That happens to players inside the server's own network whose router doesn't loop the public address
 * back to it. Which address works is remembered for the rest of the session.
 */
public final class JoinAddressFallback {

    /** Advertised origin to join origin, both as scheme://host:port. */
    private static final Map<String, String> JOIN_ORIGINS = new ConcurrentHashMap<>();
    /** Advertised origins that only answered through the join address. */
    private static final Set<String> USE_JOIN = ConcurrentHashMap.newKeySet();

    private JoinAddressFallback() {}

    /** A request to one URL. */
    public interface Request<T, E extends Exception> {

        T send(String url) throws IOException, E;
    }

    /**
     * Records that the web API a server advertises can also be reached at the address the player joins it with, as
     * {@code host} or {@code host:port}.
     */
    public static void register(String advertisedApiRoot, String joinAddress) {
        String advertised = origin(advertisedApiRoot);
        String join = joinOrigin(joinAddress);
        if (advertised == null || join == null || advertised.equals(join)) return;
        JOIN_ORIGINS.put(advertised, join);
    }

    /** Sends a request, through the join address when the advertised one didn't answer, or retrying through it. */
    public static <T, E extends Exception> T send(String url, Request<T, E> request) throws IOException, E {
        String advertised = origin(url);
        String join = advertised == null ? null : JOIN_ORIGINS.get(advertised);
        if (join == null) return request.send(url);
        if (USE_JOIN.contains(advertised)) return request.send(join + url.substring(advertised.length()));
        try {
            return request.send(url);
        } catch (IOException e) {
            if (!isUnreachable(e)) throw e;
            T result = request.send(join + url.substring(advertised.length()));
            if (USE_JOIN.add(advertised)) {
                WawelAuth.LOG.info(
                    "A server's advertised Wawel Auth address didn't answer ({}), using the address it was joined with",
                    e.getClass()
                        .getSimpleName());
            }
            return result;
        }
    }

    private static boolean isUnreachable(IOException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof ConnectException
                || cause instanceof NoRouteToHostException
                || cause instanceof UnknownHostException
                || cause.getClass()
                    .getSimpleName()
                    .contains("Timeout")) {
                return true;
            }
        }
        return false;
    }

    /**
     * scheme://authority of a URL as written, lower case, or null. The server builds every URL from its one advertised
     * address, so the same spelling matches.
     */
    static String origin(String url) {
        if (url == null) return null;
        try {
            String trimmed = url.trim();
            URI uri = new URI(trimmed);
            if (uri.getScheme() == null || uri.getRawAuthority() == null) return null;
            String origin = (uri.getScheme() + "://" + uri.getRawAuthority()).toLowerCase(Locale.ROOT);
            return trimmed.toLowerCase(Locale.ROOT)
                .startsWith(origin) ? origin : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** http://host:port for a join address such as {@code play.example.com} or {@code 10.0.0.2:25565}. */
    static String joinOrigin(String joinAddress) {
        if (joinAddress == null || joinAddress.trim()
            .isEmpty()) return null;
        String address = joinAddress.trim();
        String host;
        int port = 25565;
        if (address.startsWith("[")) {
            int close = address.indexOf(']');
            if (close < 0) return null;
            host = address.substring(0, close + 1);
            if (address.length() > close + 2 && address.charAt(close + 1) == ':') {
                port = parsePort(address.substring(close + 2), port);
            }
        } else {
            int colon = address.lastIndexOf(':');
            if (colon > 0 && address.indexOf(':') == colon) {
                host = address.substring(0, colon);
                port = parsePort(address.substring(colon + 1), port);
            } else {
                host = address;
            }
        }
        return "http://" + host.toLowerCase(Locale.ROOT) + ":" + port;
    }

    private static int parsePort(String text, int fallback) {
        try {
            int port = Integer.parseInt(text.trim());
            return port > 0 && port < 65536 ? port : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
