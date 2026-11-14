package industries.angelic.wawelauth.wawelclient.lan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import industries.angelic.angellib.util.StringUtil;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.WorldAuthSettings;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;
import industries.angelic.wawelauth.wawelclient.data.ProviderType;

/**
 * Which providers' players may join a world opened to LAN, chosen per world. The providers Wawel Auth ships (and
 * those a pack adds to default-providers) start ticked, the ones a player added start unticked, and the provider of
 * the host's own account is always accepted. Players no accepted provider verifies are turned away unless the world
 * lets them in as offline players, as vanilla LAN worlds do. Choices live in the world's {@link WorldAuthSettings}.
 */
public final class LanProviders {

    private LanProviders() {}

    /** One provider as the world sees it. */
    public static final class Row {

        public final ClientProvider provider;
        public final boolean accepted;
        /** The host plays with this provider's account, so it can't be unticked. */
        public final boolean hostAccount;

        Row(ClientProvider provider, boolean accepted, boolean hostAccount) {
            this.provider = provider;
            this.accepted = accepted;
            this.hostAccount = hostAccount;
        }
    }

    /** Every provider whose players could join, in the client's order. */
    public static synchronized List<Row> rows(File worldDirectory, ClientProvider hostProvider) {
        List<Row> rows = new ArrayList<>();
        WawelClient client = WawelClient.instance();
        if (client == null) {
            return rows;
        }

        Map<String, Boolean> choices = WorldAuthSettings.load(worldDirectory)
            .getLanProviders();
        for (ClientProvider provider : client.getProviderDAO()
            .listAll()) {
            if (!canVouchForPlayers(provider)) continue;
            boolean hostAccount = sameProvider(provider, hostProvider);
            Boolean chosen = choices.get(key(provider));
            boolean accepted = hostAccount || (chosen != null ? chosen : startsAccepted(provider));
            rows.add(new Row(provider, accepted, hostAccount));
        }
        return rows;
    }

    public static List<ClientProvider> accepted(File worldDirectory, ClientProvider hostProvider) {
        List<ClientProvider> accepted = new ArrayList<>();
        for (Row row : rows(worldDirectory, hostProvider)) {
            if (row.accepted) accepted.add(row.provider);
        }
        return accepted;
    }

    public static synchronized void setAccepted(File worldDirectory, ClientProvider provider, boolean accepted) {
        WorldAuthSettings.Data data = WorldAuthSettings.load(worldDirectory);
        if (accepted == startsAccepted(provider)) {
            data.getLanProviders()
                .remove(key(provider));
        } else {
            data.getLanProviders()
                .put(key(provider), accepted);
        }
        WorldAuthSettings.save(worldDirectory, data);
    }

    /** Whether players no accepted provider verifies may join as offline players; off unless ticked. */
    public static boolean letsOthersIn(File worldDirectory) {
        return WorldAuthSettings.load(worldDirectory)
            .letsOthersIn();
    }

    public static synchronized void setLetsOthersIn(File worldDirectory, boolean letOthersIn) {
        WorldAuthSettings.Data data = WorldAuthSettings.load(worldDirectory);
        data.setLetsOthersIn(letOthersIn);
        WorldAuthSettings.save(worldDirectory, data);
    }

    static boolean startsAccepted(ClientProvider provider) {
        return provider.getType() == ProviderType.DEFAULT;
    }

    /** The offline provider can't confirm a join, and neither can one without a session server. */
    static boolean canVouchForPlayers(ClientProvider provider) {
        return provider != null && provider.getType() != ProviderType.BUILTIN
            && StringUtil.trimToNull(provider.getSessionServerUrl()) != null;
    }

    static boolean sameProvider(ClientProvider a, ClientProvider b) {
        return a != null && b != null
            && a.getName() != null
            && a.getName()
                .equalsIgnoreCase(b.getName());
    }

    private static String key(ClientProvider provider) {
        return provider.getName()
            .toLowerCase(Locale.ROOT);
    }
}
