package industries.angelic.wawelauth.wawelcore.util;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import industries.angelic.wawelauth.WawelAuth;

/**
 * Explains Java's "illegal IPv4 address" without showing the address. With Forge's security manager installed,
 * every web request checks its host: a host whose last dot-separated part is all digits, or empty after a trailing
 * dot, must be a valid IPv4 address, or the request fails. So {@code 300.1.1.1}, {@code 10.0.0.1.},
 * {@code example.com.} and {@code play.server.2} all fail.
 */
public final class AddressDiagnostics {

    private static final Set<String> WARNED = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AddressDiagnostics() {}

    /** Why Java would refuse requests to this URL's host as an invalid IPv4 address, or null when it wouldn't. */
    public static String problem(String url) {
        String host = host(url);
        return host == null ? null : hostProblem(host);
    }

    /** Why Java would refuse this host as an invalid IPv4 address, or null when it wouldn't. */
    public static String hostProblem(String host) {
        if (host == null || host.length() <= 1 || host.startsWith("[")) return null;
        int lastDot = host.lastIndexOf('.');
        if (lastDot < 0) return null;
        for (int i = lastDot + 1; i < host.length(); i++) {
            if (!isDigit(host.charAt(i))) return null; // an ordinary host name
        }
        if (lastDot == host.length() - 1) return "it ends with a dot";

        String[] parts = host.split("\\.", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) return "part " + (i + 1) + " is empty";
            for (int c = 0; c < parts[i].length(); c++) {
                if (!isDigit(parts[i].charAt(c))) {
                    return "its last part is a number, so Java reads it as an IPv4 address, but part " + (i + 1)
                        + " isn't a number";
                }
            }
        }
        if (parts.length > 4) return "it has " + parts.length + " parts, an IPv4 address has 4";
        if (host.length() > 15) return "it's longer than 15 characters";
        for (int i = 0; i < parts.length - 1; i++) {
            if (Long.parseLong(parts[i]) > 255) return "part " + (i + 1) + " is over 255";
        }
        // Java also takes the old shorter forms, where the last part fills the remaining bytes.
        long limit = 1L << ((5 - parts.length) * 8);
        if (Long.parseLong(parts[parts.length - 1]) >= limit) {
            return "part " + parts.length + " is over " + (limit - 1);
        }
        return null;
    }

    /** The host with letters shown as {@code a} and digits as {@code N}, to describe it without showing it. */
    public static String shape(String host) {
        if (host == null) return "?";
        StringBuilder shape = new StringBuilder(host.length());
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            shape.append(isDigit(c) ? 'N' : Character.isLetter(c) ? 'a' : c);
        }
        return shape.toString();
    }

    /**
     * A message for a URL Java will refuse, naming where it came from, or null when the URL is fine. The host is
     * only described by its shape.
     */
    public static String describe(String source, String url) {
        String host = host(url);
        String problem = host == null ? null : hostProblem(host);
        if (problem == null) return null;
        return source + ": the address "
            + shape(host)
            + " isn't a valid IPv4 address ("
            + problem
            + "), so requests to it fail with \"illegal IPv4 address\"";
    }

    /**
     * Logs a warning, once per source and URL, when Java will refuse the URL's host. Returns whether it will. Java
     * only checks with a security manager installed, which Forge always has.
     */
    public static boolean warnIfRefused(String source, String url) {
        if (System.getSecurityManager() == null) return false;
        String message = describe(source, url);
        if (message == null) return false;
        if (WARNED.add(source + '\n' + url)) {
            WawelAuth.LOG.warn(message);
        }
        return true;
    }

    /**
     * Throws the error Java would, with the reason in it, before a request to a host Java refuses. Also logs it once.
     */
    public static void checkRequest(String source, String url) {
        if (warnIfRefused(source, url)) {
            throw new IllegalArgumentException(describe(source, url));
        }
    }

    /** Logs, once, the Java and security manager behind the check, for reports about it. */
    public static void logEnvironment() {
        if (WARNED.add("environment")) {
            SecurityManager securityManager = System.getSecurityManager();
            WawelAuth.LOG.warn(
                "Java {} ({}), security manager: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                securityManager != null ? securityManager.getClass()
                    .getName() : "none");
        }
    }

    private static String host(String url) {
        if (url == null || url.trim()
            .isEmpty()) return null;
        String trimmed = url.trim();
        try {
            String host = new URI(trimmed.contains("://") ? trimmed : "https://" + trimmed).getHost();
            if (host != null) return host;
        } catch (Exception ignored) {}
        // URI gives no host for some of the addresses Java refuses, such as one ending in a dot plus a port.
        int start = trimmed.indexOf("://");
        String rest = start >= 0 ? trimmed.substring(start + 3) : trimmed;
        int end = rest.length();
        for (char stop : new char[] { '/', '?', '#' }) {
            int index = rest.indexOf(stop);
            if (index >= 0 && index < end) end = index;
        }
        String authority = rest.substring(0, end);
        int at = authority.lastIndexOf('@');
        if (at >= 0) authority = authority.substring(at + 1);
        if (authority.startsWith("[")) return null;
        int colon = authority.indexOf(':');
        String host = colon >= 0 ? authority.substring(0, colon) : authority;
        return host.isEmpty() ? null : host;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
