package industries.angelic.wawelauth.wawelserver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.mojang.authlib.GameProfile;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.common.ServerTaskScheduler;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorPlayerProfileCache;

/**
 * Which player a name without a UUID stands for, such as a player head's owner, by the rules of Wawel Auth's other
 * names. A plain name is one of this server's players: online, or in its user cache. The same name can belong to
 * different people at different providers, so it is never sent to Mojang or a provider. A qualified name,
 * {@code name@provider}, is looked up at that provider, off the server thread.
 */
public final class ServerNameLookup {

    /** How long a qualified name keeps its answer, found or not. */
    private static final long FOUND_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long MISSING_MS = TimeUnit.MINUTES.toMillis(5);

    private static final Map<String, Answer> ANSWERS = new ConcurrentHashMap<>();
    private static final Map<String, List<Consumer<GameProfile>>> WAITING = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "WawelAuth-ServerNameLookup");
        thread.setDaemon(true);
        return thread;
    });

    private ServerNameLookup() {}

    /** Whether the name names its provider, as {@code name@provider}. */
    public static boolean isQualified(String name) {
        return FallbackWhitelistLookup.isQualifiedProviderUsername(name);
    }

    /** The server's player going by a plain name, online or in the user cache; null if none. Never blocks. */
    public static GameProfile known(String name) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || name == null || name.isEmpty()) return null;

        EntityPlayerMP online = server.getConfigurationManager()
            .func_152612_a(name); // getPlayerForUsername
        if (online != null && online.getGameProfile() != null
            && online.getGameProfile()
                .getId() != null) {
            return online.getGameProfile();
        }

        LinkedList<GameProfile> cached = ((AccessorPlayerProfileCache) server.func_152358_ax()).wawelauth$getProfiles();
        synchronized (cached) {
            for (GameProfile profile : cached) {
                if (profile != null && profile.getId() != null && name.equalsIgnoreCase(profile.getName())) {
                    return profile;
                }
            }
        }
        return null;
    }

    /**
     * Looks up a qualified name at its provider and hands the profile, or null if the provider does not know it, to
     * {@code then} on the server thread. Answers are kept for a while; lookups of the same name share one request.
     */
    public static void qualified(String name, Consumer<GameProfile> then) {
        String key = name.toLowerCase(Locale.ROOT);
        Answer answer = ANSWERS.get(key);
        if (answer != null && System.currentTimeMillis() < answer.expiresAt) {
            ServerTaskScheduler.schedule(() -> then.accept(answer.profile));
            return;
        }

        boolean first;
        synchronized (WAITING) {
            List<Consumer<GameProfile>> waiting = WAITING.get(key);
            first = waiting == null;
            if (first) WAITING.put(key, waiting = new ArrayList<>());
            waiting.add(then);
        }
        if (!first) return;

        try {
            EXECUTOR.execute(() -> {
                GameProfile profile = null;
                try {
                    profile = FallbackWhitelistLookup.resolveQualifiedProfile(name);
                } catch (RuntimeException e) {
                    WawelAuth.debug("Lookup of " + name + " failed: " + e.getMessage());
                }
                if (profile != null && profile.getId() == null) profile = null;
                long now = System.currentTimeMillis();
                ANSWERS.put(key, new Answer(profile, now + (profile != null ? FOUND_MS : MISSING_MS)));
                deliver(key, profile);
            });
        } catch (RejectedExecutionException e) {
            deliver(key, null);
        }
    }

    /** The session server of the provider a qualified name names, for clients to fetch its textures; or null. */
    public static String sessionUrl(String qualifiedName) {
        return FallbackWhitelistLookup.resolveQualifiedSessionUrl(qualifiedName);
    }

    private static void deliver(String key, GameProfile profile) {
        List<Consumer<GameProfile>> waiting;
        synchronized (WAITING) {
            waiting = WAITING.remove(key);
        }
        if (waiting == null) return;
        ServerTaskScheduler.schedule(() -> { for (Consumer<GameProfile> then : waiting) then.accept(profile); });
    }

    private static final class Answer {

        final GameProfile profile;
        final long expiresAt;

        Answer(GameProfile profile, long expiresAt) {
            this.profile = profile;
            this.expiresAt = expiresAt;
        }
    }
}
