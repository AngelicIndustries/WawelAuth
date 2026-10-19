package industries.angelic.wawelauth.wawelclient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.api.WawelTextureResolver;
import industries.angelic.wawelauth.packet.NameLookupPacket;
import industries.angelic.wawelauth.packet.PacketHandler;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;
import industries.angelic.wawelauth.wawelclient.http.ProviderRoutedHttp;
import industries.angelic.wawelauth.wawelcore.util.ProfileNameLookup;

/**
 * Finds the profile and providers behind a bare player name, for code that only has a name, such as vanilla's
 * name-based skin path. A player in the world answers at once. Otherwise a Wawel Auth server is asked which of its
 * players goes by the name, since the same name can belong to different people at different providers. Names it does
 * not know are looked up in the background with the connection's trusted providers, or Mojang in singleplayer. Answers
 * are kept for a while.
 */
public final class NameProfileResolver {

    /** How long a found name keeps its profile, and how soon a name nobody knew is asked again. */
    private static final long FOUND_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long MISSING_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long MOJANG_PROVIDER_MS = TimeUnit.SECONDS.toMillis(10);
    /** How long to wait for the server before asking again. */
    private static final long SERVER_PENDING_MS = TimeUnit.SECONDS.toMillis(15);
    private static final int TIMEOUT_MS = 10_000;

    private static final String API_LOCATION_HEADER = "X-Authlib-Injector-API-Location";
    private static final String API_LOCATION_HEADER_ALT = "X-Authlib-Injector-Api-Location";

    /** A name's profile and the providers to fetch its textures from. */
    public static final class Match {

        public final GameProfile profile;
        public final List<ClientProvider> providers;

        Match(GameProfile profile, List<ClientProvider> providers) {
            this.profile = profile;
            this.providers = providers;
        }
    }

    /** What the server said about a name: its player, or none (profile null). */
    private static final class ServerAnswer {

        final GameProfile profile;
        final boolean pending;
        final long expiresAt;

        ServerAnswer(GameProfile profile, boolean pending, long expiresAt) {
            this.profile = profile;
            this.pending = pending;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Lookup {

        final Match match;
        final boolean pending;
        final long expiresAt;

        Lookup(Match match, boolean pending, long expiresAt) {
            this.match = match;
            this.pending = pending;
            this.expiresAt = expiresAt;
        }
    }

    private final WawelClient client;
    private final Map<String, Lookup> lookups = new ConcurrentHashMap<>();
    private final Map<String, ServerAnswer> serverAnswers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "WawelAuth-NameLookup");
        thread.setDaemon(true);
        return thread;
    });

    private ClientProvider mojangProvider;
    private long mojangProviderCheckedAt;

    NameProfileResolver(WawelClient client) {
        this.client = client;
    }

    /** The skin to draw for the name, or null to keep the default. Never blocks; call it as often as needed. */
    public ResourceLocation skin(String rawName) {
        Match match = resolve(rawName);
        if (match == null) return null;
        ResourceLocation skin = client.getTextureResolver()
            .getSkinFromAnyProvider(match.profile.getId(), match.profile.getName(), match.providers);
        if (skin == null || skin.equals(WawelTextureResolver.getDefaultSkin())
            || skin.equals(WawelTextureResolver.getLegacyDefaultSkin())) {
            return null;
        }
        return skin;
    }

    /** The name's match, or null while it is being looked up or when no provider knows it. Never blocks. */
    public Match resolve(String rawName) {
        String name = normalize(rawName);
        if (name == null) return null;

        GameProfile inWorld = profileInWorld(name);
        if (inWorld != null) {
            List<ClientProvider> providers = client.resolvePlayerProviderCandidates(inWorld.getId());
            return providers.isEmpty() ? null : new Match(inWorld, providers);
        }

        ConnectionProviderCache connection = client.getConnectionProviderCache();
        if (connection.hasServerNameLookup()) {
            ServerAnswer answer = askServer(name);
            if (answer.profile == null && answer.pending) return null;
            if (answer.profile != null) {
                List<ClientProvider> candidates = client.resolvePlayerProviderCandidates(answer.profile.getId());
                return candidates.isEmpty() ? null : new Match(answer.profile, candidates);
            }
            // Not one of the server's players: ask the providers
        }

        List<ClientProvider> providers = lookupProviders();
        if (providers.isEmpty()) return null;

        String key = key(name, providers);
        Lookup lookup = lookups.get(key);
        if (lookup != null && (lookup.pending || System.currentTimeMillis() < lookup.expiresAt)) {
            return lookup.match;
        }

        // Keep showing the old answer while a new one is fetched
        Lookup pending = new Lookup(lookup == null ? null : lookup.match, true, Long.MAX_VALUE);
        boolean claimed = lookup == null ? lookups.putIfAbsent(key, pending) == null
            : lookups.replace(key, lookup, pending);
        if (claimed) {
            try {
                executor.execute(() -> lookUp(key, name, providers));
            } catch (RejectedExecutionException e) {
                lookups.remove(key, pending);
            }
        }
        return pending.match;
    }

    /** The server's answer to a name lookup, on the client thread. */
    public void onServerAnswer(String rawName, GameProfile profile, String sessionUrl) {
        String name = normalize(rawName);
        if (name == null) return;
        if (profile != null && profile.getId() != null && sessionUrl != null) {
            ConnectionProviderCache connection = client.getConnectionProviderCache();
            ClientProvider provider = connection.findBySessionServerUrl(sessionUrl);
            if (provider != null) connection.associatePlayer(profile.getId(), provider);
        }
        boolean found = profile != null && profile.getId() != null;
        long now = System.currentTimeMillis();
        serverAnswers.put(
            serverKey(name),
            new ServerAnswer(found ? profile : null, false, now + (found ? FOUND_MS : MISSING_MS)));
    }

    void shutdown() {
        executor.shutdownNow();
    }

    /** The server's answer for the name, asking it when there is none or it is old. */
    private ServerAnswer askServer(String name) {
        String key = serverKey(name);
        ServerAnswer answer = serverAnswers.get(key);
        long now = System.currentTimeMillis();
        if (answer != null && now < answer.expiresAt) return answer;

        // Keep the old answer while the new one is on its way
        ServerAnswer pending = new ServerAnswer(answer == null ? null : answer.profile, true, now + SERVER_PENDING_MS);
        serverAnswers.put(key, pending);
        PacketHandler.sendToServer(new NameLookupPacket(name));
        return pending;
    }

    private static String serverKey(String name) {
        ServerData server = Minecraft.getMinecraft()
            .func_147104_D(); // getCurrentServerData
        String address = server == null || server.serverIP == null ? "" : server.serverIP.toLowerCase(Locale.ROOT);
        return address + "|" + name.toLowerCase(Locale.ROOT);
    }

    private void lookUp(String key, String name, List<ClientProvider> providers) {
        Match match = null;
        for (ClientProvider provider : providers) {
            List<String> urls = ProfileNameLookup.candidateUrls(
                provider.getApiRoot(),
                provider.getServicesUrl(),
                provider.getAuthServerUrl(),
                provider.getSessionServerUrl());
            try {
                GameProfile profile = ProfileNameLookup.lookup(name, urls, (url, body) -> post(provider, url, body));
                if (profile != null && profile.getId() != null) {
                    match = new Match(profile, Collections.singletonList(provider));
                    break;
                }
            } catch (IOException | RuntimeException e) {
                WawelAuth.debug("Name lookup for " + name + " at " + provider.getName() + " failed: " + e.getMessage());
            }
        }
        long now = System.currentTimeMillis();
        lookups.put(key, new Lookup(match, false, now + (match != null ? FOUND_MS : MISSING_MS)));
    }

    private static ProfileNameLookup.Response post(ClientProvider provider, String url, byte[] body)
        throws IOException {
        try (ProviderRoutedHttp.RoutedConnection routed = ProviderRoutedHttp
            .openConnection(url, provider, TIMEOUT_MS, TIMEOUT_MS, "WawelAuth", "Name lookup")) {
            HttpURLConnection conn = routed.getConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body);
            }

            int status = conn.getResponseCode();
            String apiLocation = conn.getHeaderField(API_LOCATION_HEADER);
            if (apiLocation == null) apiLocation = conn.getHeaderField(API_LOCATION_HEADER_ALT);
            String response = status == 200
                ? ProfileNameLookup.readUtf8(conn.getInputStream(), ProfileNameLookup.MAX_JSON_BYTES)
                : null;
            return new ProfileNameLookup.Response(status, response, apiLocation);
        }
    }

    /** The connection's trusted providers, or Mojang in singleplayer. */
    private List<ClientProvider> lookupProviders() {
        List<ClientProvider> trusted = client.getSessionBridge()
            .getTrustedProviders();
        if (!trusted.isEmpty()) return trusted;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !mc.isSingleplayer()) return Collections.emptyList();

        long now = System.currentTimeMillis();
        if (now - mojangProviderCheckedAt > MOJANG_PROVIDER_MS) {
            mojangProvider = client.getMojangProvider();
            mojangProviderCheckedAt = now;
        }
        return mojangProvider == null ? Collections.emptyList() : Collections.singletonList(mojangProvider);
    }

    private static String key(String name, List<ClientProvider> providers) {
        StringBuilder key = new StringBuilder(name.toLowerCase(Locale.ROOT));
        for (ClientProvider provider : providers) {
            key.append('|')
                .append(provider.getName());
        }
        return key.toString();
    }

    /** A loaded player, or the local one, with this name. */
    private static GameProfile profileInWorld(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return null;

        if (mc.theWorld != null) {
            EntityPlayer player = mc.theWorld.getPlayerEntityByName(name);
            if (player == null) {
                for (Object entry : mc.theWorld.playerEntities) {
                    if (!(entry instanceof EntityPlayer)) continue;
                    GameProfile profile = ((EntityPlayer) entry).getGameProfile();
                    if (profile != null && name.equals(profile.getName())) {
                        player = (EntityPlayer) entry;
                        break;
                    }
                }
            }
            if (player != null) {
                GameProfile profile = player.getGameProfile();
                if (profile != null && profile.getId() != null) return profile;
                UUID uuid = player.getUniqueID();
                if (uuid != null) return new GameProfile(uuid, player.getCommandSenderName());
            }
        }

        GameProfile local = localProfile(mc);
        return local != null && local.getId() != null && name.equals(local.getName()) ? local : null;
    }

    private static GameProfile localProfile(Minecraft mc) {
        if (mc.thePlayer != null && mc.thePlayer.getGameProfile() != null) {
            return mc.thePlayer.getGameProfile();
        }
        try {
            return mc.getSession() == null ? null
                : mc.getSession()
                    .func_148256_e();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String normalize(String name) {
        if (name == null) return null;
        String normalized = EnumChatFormatting.getTextWithoutFormattingCodes(name)
            .trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
