package industries.angelic.wawelauth.wawelclient.lan;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import industries.angelic.angellib.services.S00PacketServerInfoModifyService;
import industries.angelic.angellib.util.StringUtil;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.client.gui.ProviderDisplayName;
import industries.angelic.wawelauth.compat.authlibinjector.AuthlibInjectorCompat;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.WorldAuthSettings;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;
import industries.angelic.wawelauth.wawelcore.config.FallbackServer;
import industries.angelic.wawelauth.wawelcore.config.ServerConfig;
import industries.angelic.wawelauth.wawelcore.ping.PlayerProviderSyncPayload;
import industries.angelic.wawelauth.wawelcore.ping.WawelCapabilitySyncPayload;
import industries.angelic.wawelauth.wawelcore.ping.WawelPingPayload;
import industries.angelic.wawelauth.wawelserver.FallbackHasJoined;

/**
 * Wawel Auth in a singleplayer world opened to LAN. The host's game checks joining players against the providers
 * picked for the world (see {@link LanProviders}) instead of only the game's own login, and tells every player which
 * provider each one is from so skins resolve, as a Wawel Auth server does. It runs none of the server module: no web
 * API and no local accounts, so it works over LAN and through relays such as e4mc.
 */
public final class LanHost {

    /** Turns a player away whatever the world allows: the name or the account is already playing. */
    private static final ThreadLocal<String> REFUSAL = new ThreadLocal<>();
    /** Why a player wasn't verified, shown when the world turns them away. */
    private static final ThreadLocal<String> UNVERIFIED = new ThreadLocal<>();
    /** Joined players to the session server of the provider that let them in. */
    private static final Map<UUID, String> PLAYER_SESSION_URLS = new ConcurrentHashMap<>();
    private static volatile MinecraftServer trackedServer;
    /**
     * The host's signed textures from their provider. The host's profile comes from the game's session without them,
     * and other players' games only look up textures for their own player, so they'd see the default skin.
     */
    private static volatile Property hostTextures;
    private static volatile UUID hostTexturesFor;

    private LanHost() {}

    public static void registerPingHandler() {
        S00PacketServerInfoModifyService.registerHandler((response, angelLibPresent) -> {
            if (!angelLibPresent) return null;
            IntegratedServer server = openServer();
            if (server == null) return null;
            return new S00PacketServerInfoModifyService.KeyValue(
                WawelAuth.MODID,
                WawelPingPayload.buildServerPayload(capabilities(server), null));
        });
    }

    /** Checks a joining player, in place of the game's own check. */
    public static GameProfile hasJoinedServer(GameProfile user, String serverId, MinecraftSessionService gameService)
        throws AuthenticationUnavailableException {
        REFUSAL.remove();
        UNVERIFIED.remove();
        IntegratedServer server = currentServer();
        if (server == null || user == null || StringUtil.trimToNull(user.getName()) == null || serverId == null) {
            return gameService.hasJoinedServer(user, serverId);
        }
        if (isOnline(server, user.getName(), null)) {
            REFUSAL.set("A player with that username is already online.");
            return null;
        }
        fetchHostTextures(server);

        List<ClientProvider> accepted = acceptedProviders(server);
        FallbackHasJoined.Hit hit = FallbackHasJoined.query(user.getName(), serverId, toFallbacks(accepted));
        GameProfile profile = hit != null ? hit.profile : null;
        // The game's own login may be another server a launcher put in, which Wawel Auth may not know; it keeps
        // letting its players in as before. Mojang's is the Mojang provider, which the world's choice covers.
        if (profile == null && !checksWithMojang(gameService)) {
            profile = gameService.hasJoinedServer(user, serverId);
        }
        if (profile == null) {
            UNVERIFIED.set("Failed to verify username! This world takes accounts from " + providerNames(accepted));
            WawelAuth.LOG.info("LAN world: none of the world's providers verified {}", user.getName());
            return null;
        }
        if (profile.getId() != null && isOnline(server, null, profile.getId())) {
            // The game would kick the player already playing with that UUID, the host too.
            REFUSAL.set("That account is already playing in this world.");
            return null;
        }

        if (hit != null && profile.getId() != null) {
            PLAYER_SESSION_URLS.put(profile.getId(), hit.sessionUrl);
        }
        WawelAuth.LOG.info(
            "LAN world: {} joined through {}",
            profile.getName(),
            hit != null ? StringUtil.trimToNull(hit.fallback.getName()) : "the game's own login");
        return profile;
    }

    /**
     * Whether a player the check didn't verify still joins, as an offline player. Vanilla LAN worlds let everyone in;
     * a world with Wawel Auth only does when it's ticked for the world.
     */
    public static boolean letsUnverifiedIn(MinecraftServer server) {
        if (REFUSAL.get() != null) {
            return false;
        }
        IntegratedServer current = currentServer();
        if (current == null || current != server) {
            return server.isSinglePlayer();
        }
        return LanProviders.letsOthersIn(worldDirectory(current));
    }

    public static String consumeDisconnectReason(String defaultReason) {
        String reason = REFUSAL.get() != null ? REFUSAL.get() : UNVERIFIED.get();
        REFUSAL.remove();
        UNVERIFIED.remove();
        return reason != null ? reason : defaultReason;
    }

    /** Sends a joining player, and the host the first time, which providers players are from. */
    public static void onPlayerJoining(NetworkManager netManager, EntityPlayerMP player,
        NetHandlerPlayServer netHandler, List<?> players) {
        IntegratedServer server = currentServer();
        if (server == null || netManager.isLocalChannel()) {
            return;
        }

        byte[] capabilities = encodeCapabilities(server);
        Map<UUID, String> playerProviders = playerProviders(players);
        // The host joined before the world was opened, and the world's providers may have changed since.
        EntityPlayerMP host = findHost(players);
        if (host != null) {
            applyHostTextures(host);
            send(host, WawelCapabilitySyncPayload.CHANNEL, capabilities);
            send(host, PlayerProviderSyncPayload.CHANNEL, PlayerProviderSyncPayload.encode(playerProviders));
        }

        netHandler.sendPacket(new S3FPacketCustomPayload(WawelCapabilitySyncPayload.CHANNEL, capabilities));
        if (!playerProviders.isEmpty()) {
            netHandler.sendPacket(
                new S3FPacketCustomPayload(
                    PlayerProviderSyncPayload.CHANNEL,
                    PlayerProviderSyncPayload.encode(playerProviders)));
        }

        UUID joining = player.getGameProfile() != null ? player.getGameProfile()
            .getId() : null;
        String joiningProvider = joining != null ? PLAYER_SESSION_URLS.get(joining) : null;
        if (joiningProvider != null) {
            byte[] single = PlayerProviderSyncPayload.encodeSingle(joining, joiningProvider);
            for (Object raw : players) {
                if (raw instanceof EntityPlayerMP other && other != player) {
                    send(other, PlayerProviderSyncPayload.CHANNEL, single);
                }
            }
        }
        WawelAuth.debug(
            "LAN world: sent provider info to " + player.getCommandSenderName()
                + " ("
                + playerProviders.size()
                + " players)");
    }

    public static void onPlayerLeft(EntityPlayerMP player) {
        GameProfile profile = player.getGameProfile();
        if (profile != null && profile.getId() != null) {
            PLAYER_SESSION_URLS.remove(profile.getId());
        }
    }

    private static IntegratedServer currentServer() {
        MinecraftServer server = MinecraftServer.getServer();
        if (!(server instanceof IntegratedServer)) {
            return null;
        }
        if (server != trackedServer) {
            synchronized (LanHost.class) {
                if (server != trackedServer) {
                    PLAYER_SESSION_URLS.clear();
                    hostTextures = null;
                    hostTexturesFor = null;
                    trackedServer = server;
                }
            }
        }
        return (IntegratedServer) server;
    }

    /** On the login thread, so the server's ticks don't wait for the provider. */
    private static void fetchHostTextures(IntegratedServer server) {
        EntityPlayerMP host = findHost(new ArrayList<Object>(server.getConfigurationManager().playerEntityList));
        GameProfile profile = host != null ? host.getGameProfile() : null;
        ClientProvider provider = hostProvider();
        if (profile == null || profile.getId() == null
            || profile.getId()
                .equals(hostTexturesFor)
            || profile.getProperties()
                .containsKey("textures")
            || !LanProviders.canVouchForPlayers(provider)) {
            return;
        }
        GameProfile filled = FallbackHasJoined
            .profile(provider.getSessionServerUrl(), profile.getId(), profile.getName());
        if (filled == null || filled.getProperties()
            .get("textures")
            .isEmpty()) {
            WawelAuth.debug("LAN world: no textures for the host from " + provider.getName());
            return;
        }
        hostTextures = filled.getProperties()
            .get("textures")
            .iterator()
            .next();
        hostTexturesFor = profile.getId();
    }

    /** On the server thread, before the joining player is sent the host. */
    private static void applyHostTextures(EntityPlayerMP host) {
        Property textures = hostTextures;
        GameProfile profile = host.getGameProfile();
        if (textures != null && profile != null
            && profile.getId() != null
            && profile.getId()
                .equals(hostTexturesFor)
            && !profile.getProperties()
                .containsKey("textures")) {
            profile.getProperties()
                .put("textures", textures);
        }
    }

    private static IntegratedServer openServer() {
        IntegratedServer server = currentServer();
        return server != null && server.getPublic() ? server : null;
    }

    private static List<ClientProvider> acceptedProviders(IntegratedServer server) {
        return LanProviders.accepted(worldDirectory(server), hostProvider());
    }

    private static File worldDirectory(IntegratedServer server) {
        return server.worldServers != null && server.worldServers.length > 0 && server.worldServers[0] != null
            ? server.worldServers[0].getSaveHandler()
                .getWorldDirectory()
            : WorldAuthSettings.worldDirectory(server.getFolderName());
    }

    private static String providerNames(List<ClientProvider> providers) {
        StringBuilder names = new StringBuilder();
        for (ClientProvider provider : providers) {
            if (names.length() > 0) names.append(", ");
            names.append(ProviderDisplayName.displayName(provider.getName()));
        }
        return names.toString();
    }

    private static ClientProvider hostProvider() {
        WawelClient client = WawelClient.instance();
        return client != null ? client.getSessionBridge()
            .getActiveProvider() : null;
    }

    /** The world as a Wawel Auth server that accepts the chosen providers and has nothing of its own. */
    private static ServerConfig capabilities(IntegratedServer server) {
        ServerConfig config = new ServerConfig();
        config.setWawelAuthEnabled(true);
        config.setLocalAuthEnabled(false);
        config.setFallbackServers(toFallbacks(acceptedProviders(server)));
        return config;
    }

    /** Without the name lookup a server offers: this world doesn't answer it. */
    private static byte[] encodeCapabilities(IntegratedServer server) {
        return WawelPingPayload.buildServerPayload(capabilities(server), null)
            .toString()
            .getBytes(StandardCharsets.UTF_8);
    }

    private static List<FallbackServer> toFallbacks(List<ClientProvider> providers) {
        List<FallbackServer> fallbacks = new ArrayList<>();
        for (ClientProvider provider : providers) {
            FallbackServer fallback = new FallbackServer();
            fallback.setName(provider.getName());
            fallback.setApiRoot(orEmpty(provider.getApiRoot()));
            fallback.setSessionServerUrl(provider.getSessionServerUrl());
            fallback.setAccountUrl(orEmpty(provider.getAuthServerUrl()));
            fallback.setServicesUrl(orEmpty(provider.getServicesUrl()));
            fallback.setSignaturePublicKeyBase64(orEmpty(provider.getPublicKeyBase64()));
            fallback.setSkinDomains(provider.getSkinDomainList());
            fallbacks.add(fallback);
        }
        return fallbacks;
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    /** The joined players' providers, and the host's. */
    private static Map<UUID, String> playerProviders(List<?> players) {
        Map<UUID, String> providers = new HashMap<>(PLAYER_SESSION_URLS);
        EntityPlayerMP host = findHost(players);
        ClientProvider hostProvider = hostProvider();
        if (host != null && host.getGameProfile() != null
            && host.getGameProfile()
                .getId() != null
            && LanProviders.canVouchForPlayers(hostProvider)) {
            providers.put(
                host.getGameProfile()
                    .getId(),
                hostProvider.getSessionServerUrl());
        }
        return providers;
    }

    private static EntityPlayerMP findHost(List<?> players) {
        for (Object raw : players) {
            if (raw instanceof EntityPlayerMP player && player.playerNetServerHandler != null
                && player.playerNetServerHandler.func_147362_b() // NetHandlerPlayServer.getNetworkManager
                    .isLocalChannel()) {
                return player;
            }
        }
        return null;
    }

    private static boolean isOnline(MinecraftServer server, String name, UUID id) {
        if (server.getConfigurationManager() == null) {
            return false;
        }
        for (Object raw : server.getConfigurationManager().playerEntityList) {
            if (!(raw instanceof EntityPlayerMP player)) continue;
            if (name != null && name.equalsIgnoreCase(player.getCommandSenderName())) return true;
            if (id != null && id.equals(player.getUniqueID())) return true;
        }
        return false;
    }

    private static void send(EntityPlayerMP player, String channel, byte[] payload) {
        try {
            player.playerNetServerHandler.sendPacket(new S3FPacketCustomPayload(channel, payload));
        } catch (Exception e) {
            WawelAuth.debug("LAN world: couldn't send " + channel + " to " + player.getCommandSenderName());
        }
    }

    /** Whether the game checks joins with Mojang's session server, as it does unless a launcher changed it. */
    private static boolean checksWithMojang(MinecraftSessionService gameService) {
        if (AuthlibInjectorCompat.isActive()) {
            return false;
        }
        // Mojang's authlib keeps the address in a static BASE_URL, the modern one on the session service.
        String url = stringField(null, "BASE_URL");
        if (url == null && gameService instanceof YggdrasilMinecraftSessionService) {
            url = stringField(gameService, "baseUrl");
        }
        try {
            return url != null && "sessionserver.mojang.com".equalsIgnoreCase(new URI(url.trim()).getHost());
        } catch (Exception e) {
            return false;
        }
    }

    private static String stringField(Object target, String name) {
        try {
            Field field = YggdrasilMinecraftSessionService.class.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);
            return value instanceof String ? (String) value : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }
}
