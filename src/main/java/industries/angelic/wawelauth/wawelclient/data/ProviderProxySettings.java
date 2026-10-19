package industries.angelic.wawelauth.wawelclient.data;

/**
 * Provider-scoped proxy configuration persisted with a client provider.
 */
public class ProviderProxySettings {

    private boolean enabled;
    private ProviderProxyType type = ProviderProxyType.HTTP;
    private String host;
    private Integer port;
    private String username;
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ProviderProxyType getType() {
        return type != null ? type : ProviderProxyType.HTTP;
    }

    public void setType(ProviderProxyType type) {
        this.type = type != null ? type : ProviderProxyType.HTTP;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasCredentials() {
        return username != null && !username.trim()
            .isEmpty();
    }

    public boolean hasEndpoint() {
        return host != null && !host.trim()
            .isEmpty() && port != null && port.intValue() > 0;
    }
}
