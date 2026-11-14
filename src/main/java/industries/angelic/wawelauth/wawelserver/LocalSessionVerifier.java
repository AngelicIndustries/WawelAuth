package industries.angelic.wawelauth.wawelserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;

import industries.angelic.angellib.util.StringUtil;
import industries.angelic.wawelauth.Config;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelcore.config.ServerConfig;

/**
 * Server-side login verification. Queries local hasJoined endpoint first
 * when WawelAuth server module is enabled.
 */
public final class LocalSessionVerifier {

    private static final ThreadLocal<String> DISCONNECT_REASON = new ThreadLocal<String>();

    /** Player UUID -> session server URL of their auth provider. Set during hasJoined. */
    private static final ConcurrentHashMap<UUID, String> playerProviderSessionUrls = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> playerProviderNames = new ConcurrentHashMap<>();

    private LocalSessionVerifier() {}

    public static GameProfile hasJoinedServer(GameProfile user, String serverId, MinecraftSessionService vanillaService)
        throws AuthenticationUnavailableException {
        clearDisconnectReason();
        if (user == null || user.getName() == null
            || user.getName()
                .isEmpty()
            || serverId == null
            || serverId.isEmpty()) {
            return null;
        }
        if (isUsernameAlreadyOnline(user.getName())) {
            setDisconnectReason("A player with that username is already online.");
            WawelAuth.LOG.warn("Rejecting login for '{}' because that username is already online", user.getName());
            return null;
        }

        ServerConfig config = Config.server();
        boolean localEnabled = config != null && config.isWawelAuthEnabled() && config.isLocalAuthEnabled();

        if (config == null || !config.isWawelAuthEnabled()) {
            return vanillaService.hasJoinedServer(user, serverId);
        }

        // Try local auth first if enabled
        if (localEnabled) {
            if (WawelServer.instance() == null) {
                WawelAuth.LOG.warn("WawelAuth local auth is enabled but server module is not running");
            } else {
                GameProfile localResult = queryLocalHasJoined(user.getName(), serverId);
                if (localResult != null) {
                    String localSessionUrl = resolveLocalSessionServerUrl();
                    if (localResult.getId() != null && localSessionUrl != null) {
                        playerProviderSessionUrls.put(localResult.getId(), localSessionUrl);
                        playerProviderNames.put(localResult.getId(), "local");
                    }
                    return localResult;
                }
            }
        }

        // Try configured fallback session servers (Mojang must be listed explicitly)
        GameProfile fallbackResult = queryConfiguredFallbacks(user.getName(), serverId, config);
        if (fallbackResult != null) {
            return fallbackResult;
        }

        return null;
    }

    private static GameProfile queryLocalHasJoined(String username, String serverId)
        throws AuthenticationUnavailableException {
        WawelServer server = WawelServer.instance();
        if (server == null) {
            return null;
        }

        Map<String, Object> profile = server.consumeLocalHasJoinedProfile(username, serverId, null);
        return profileFromResponseMap(profile, username);
    }

    private static GameProfile queryConfiguredFallbacks(String username, String serverId, ServerConfig config) {
        if (config == null) {
            return null;
        }

        FallbackHasJoined.Hit hit = FallbackHasJoined.query(username, serverId, config.getFallbackServers());
        if (hit == null) {
            return null;
        }
        if (hit.profile.getId() != null) {
            playerProviderSessionUrls.put(hit.profile.getId(), hit.sessionUrl);
            String providerName = StringUtil.trimToNull(hit.fallback.getName());
            if (providerName != null) {
                playerProviderNames.put(hit.profile.getId(), providerName);
            }
        }
        return hit.profile;
    }

    public static String consumeDisconnectReason(String defaultReason) {
        String reason = StringUtil.trimToNull(DISCONNECT_REASON.get());
        DISCONNECT_REASON.remove();
        return reason == null ? defaultReason : reason;
    }

    private static void setDisconnectReason(String reason) {
        String normalized = StringUtil.trimToNull(reason);
        if (normalized == null) {
            DISCONNECT_REASON.remove();
        } else {
            DISCONNECT_REASON.set(normalized);
        }
    }

    private static void clearDisconnectReason() {
        DISCONNECT_REASON.remove();
    }

    private static boolean isUsernameAlreadyOnline(String username) {
        String normalizedTarget = StringUtil.trimToNull(username);
        if (normalizedTarget == null) return false;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return false;

        ServerConfigurationManager configManager = server.getConfigurationManager();
        if (configManager == null) return false;

        for (Object rawPlayer : configManager.playerEntityList) {
            if (!(rawPlayer instanceof EntityPlayerMP)) {
                continue;
            }
            EntityPlayerMP player = (EntityPlayerMP) rawPlayer;
            GameProfile profile = player.getGameProfile();
            String onlineName = profile == null ? StringUtil.trimToNull(player.getCommandSenderName())
                : StringUtil.trimToNull(profile.getName());
            if (onlineName != null && onlineName.equalsIgnoreCase(normalizedTarget)) {
                return true;
            }
        }

        return false;
    }

    private static GameProfile profileFromResponseMap(Map<String, Object> root, String fallbackName) {
        if (root == null) {
            return null;
        }

        String id = stringValue(root.get("id"));
        if (id == null || id.isEmpty()) {
            return null;
        }

        UUID uuid;
        try {
            uuid = UUIDTypeAdapter.fromString(id);
        } catch (Exception e) {
            WawelAuth.debug("Local hasJoined returned invalid UUID: " + id);
            return null;
        }

        String safeName = StringUtil.trimToNull(stringValue(root.get("name")));
        if (safeName == null) {
            safeName = StringUtil.trimToNull(fallbackName);
        }
        if (safeName == null) {
            safeName = "UnknownPlayer";
        }

        GameProfile result = new GameProfile(uuid, safeName);
        Object properties = root.get("properties");
        if (properties instanceof Iterable<?>) {
            for (Object element : (Iterable<?>) properties) {
                if (!(element instanceof Map<?, ?>)) {
                    continue;
                }
                Map<?, ?> property = (Map<?, ?>) element;
                String propName = stringValue(property.get("name"));
                String propValue = stringValue(property.get("value"));
                if (propName == null || propValue == null) {
                    continue;
                }

                // 1.7.10 S0CPacketSpawnPlayer needs signature as a mandatory string.
                // Null signature would crash packet encoding, so skip unsigned.
                String signature = StringUtil.trimToNull(stringValue(property.get("signature")));
                if (signature == null) {
                    continue;
                }
                result.getProperties()
                    .put(propName, new Property(propName, propValue, signature));
            }
        }
        return result;
    }

    private static String stringValue(Object value) {
        return value instanceof String ? (String) value : null;
    }

    // =========================================================================
    // Per-player provider tracking
    // =========================================================================

    /** Session server URL of the player's auth provider, or null. */
    public static String getPlayerProviderSessionUrl(UUID playerUuid) {
        return playerUuid == null ? null : playerProviderSessionUrls.get(playerUuid);
    }

    public static String getPlayerProviderName(UUID playerUuid) {
        return playerUuid == null ? null : playerProviderNames.get(playerUuid);
    }

    /**
     * Get all tracked player-provider associations.
     */
    public static Map<UUID, String> getAllPlayerProviderSessionUrls() {
        return new HashMap<>(playerProviderSessionUrls);
    }

    /** Remove tracking for a player (on disconnect). */
    public static void clearPlayer(UUID playerUuid) {
        if (playerUuid != null) {
            playerProviderSessionUrls.remove(playerUuid);
            playerProviderNames.remove(playerUuid);
        }
    }

    private static String resolveLocalSessionServerUrl() {
        String apiRoot = ApiUrlUtil.resolveLocalApiRoot();
        return apiRoot != null ? apiRoot + "/sessionserver" : null;
    }
}
