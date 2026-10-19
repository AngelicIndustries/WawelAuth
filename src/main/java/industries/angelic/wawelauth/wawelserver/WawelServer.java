package industries.angelic.wawelauth.wawelserver;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import industries.angelic.wawelauth.Config;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelcore.config.ServerConfig;
import industries.angelic.wawelauth.wawelcore.crypto.KeyManager;
import industries.angelic.wawelauth.wawelcore.crypto.PropertySigner;
import industries.angelic.wawelauth.wawelcore.storage.InviteDAO;
import industries.angelic.wawelauth.wawelcore.storage.ProfileDAO;
import industries.angelic.wawelauth.wawelcore.storage.SessionDAO;
import industries.angelic.wawelauth.wawelcore.storage.TokenDAO;
import industries.angelic.wawelauth.wawelcore.storage.UserDAO;
import industries.angelic.wawelauth.wawelcore.storage.UserListProviderBindingDAO;
import industries.angelic.wawelauth.wawelcore.storage.memory.InMemorySessionDAO;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteDatabase;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteInviteDAO;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteProfileDAO;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteTokenDAO;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteUserDAO;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteUserListProviderBindingDAO;
import industries.angelic.wawelauth.wawelnet.HttpRouter;

/**
 * Singleton composition root for the Yggdrasil server module.
 * <p>
 * Initializes database, crypto, DAOs, services, and the HTTP router.
 */
public class WawelServer {

    private static final int HTTP_WORKER_THREADS = 8;
    private static final int HTTP_WORKER_QUEUE_CAPACITY = 128;

    private static WawelServer instance;

    private final ServerConfig serverConfig;
    private final SqliteDatabase database;
    private final KeyManager keyManager;
    private final HttpRouter router;
    private final UserDAO userDAO;
    private final ProfileDAO profileDAO;
    private final TokenDAO tokenDAO;
    private final InviteDAO inviteDAO;
    private final UserListProviderBindingDAO userListProviderBindingDAO;
    private final PublicPageService publicPageService;
    private final ExecutorService httpWorkerPool;
    private final SessionService sessionService;

    private WawelServer(File stateDir) {
        WawelAuth.LOG.info("Starting Wawel Auth server module...");

        // Bounded pool keeps blocking route handlers (PBKDF2, fallback proxy, ImageIO)
        // off the Netty event loops shared with player connections.
        AtomicInteger threadIndex = new AtomicInteger(1);
        httpWorkerPool = new ThreadPoolExecutor(
            HTTP_WORKER_THREADS,
            HTTP_WORKER_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(HTTP_WORKER_QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable, "WawelAuth-HTTP-Worker-" + threadIndex.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            });

        if (!stateDir.exists() && !stateDir.mkdirs()) {
            throw new RuntimeException("Failed to create state directory: " + stateDir.getAbsolutePath());
        }

        ServerConfig config = Config.server();
        this.serverConfig = config;

        // Database
        database = new SqliteDatabase(new File(stateDir, "wawelauth.db"));
        database.initialize();

        // Discover fallback providers' public signing keys
        FallbackKeyDiscovery.discoverKeys(config);

        // Crypto
        keyManager = new KeyManager(stateDir);
        keyManager.loadOrGenerate();
        PropertySigner signer = new PropertySigner(keyManager);

        // DAOs
        userDAO = new SqliteUserDAO(database);
        tokenDAO = new SqliteTokenDAO(database);
        profileDAO = new SqliteProfileDAO(database);
        inviteDAO = new SqliteInviteDAO(database);
        userListProviderBindingDAO = new SqliteUserListProviderBindingDAO(database);
        SessionDAO sessionDAO = new InMemorySessionDAO(
            config.getTokens()
                .getSessionTimeoutMs(),
            10_000);

        // File stores
        TextureFileStore textureFileStore = new TextureFileStore(stateDir);
        sweepOrphanedTextures(textureFileStore);

        // Services
        ProfileService profileService = new ProfileService(signer);
        RequestRateLimiter rateLimiter = new RequestRateLimiter();
        AuthService authService = new AuthService(
            userDAO,
            tokenDAO,
            profileDAO,
            inviteDAO,
            profileService,
            rateLimiter);
        FallbackProxyService fallbackProxyService = new FallbackProxyService(config);
        sessionService = new SessionService(tokenDAO, profileDAO, sessionDAO, profileService, fallbackProxyService);
        TextureService textureService = new TextureService(tokenDAO, profileDAO, textureFileStore);
        AdminWebService adminWebService = new AdminWebService(
            config,
            userDAO,
            profileDAO,
            tokenDAO,
            inviteDAO,
            rateLimiter,
            textureFileStore);
        publicPageService = new PublicPageService(config);

        // Router
        router = new HttpRouter();
        YggdrasilRoutes.register(
            router,
            config,
            keyManager,
            authService,
            sessionService,
            fallbackProxyService,
            textureService,
            profileService,
            profileDAO,
            textureFileStore);
        adminWebService.registerRoutes(router);
        publicPageService.registerRoutes(router);

        WawelAuth.LOG.info(
            "WawelAuth server module started. Public key: {}...{}",
            keyManager.getPublicKeyBase64()
                .substring(0, 16),
            keyManager.getPublicKeyBase64()
                .substring(
                    keyManager.getPublicKeyBase64()
                        .length() - 8));
    }

    private void sweepOrphanedTextures(TextureFileStore textureFileStore) {
        int deleted = 0;
        for (String hash : textureFileStore.listStoredHashes()) {
            if (!profileDAO.isTextureHashReferenced(hash)) {
                textureFileStore.delete(hash);
                deleted++;
            }
        }
        if (deleted > 0) {
            WawelAuth.LOG.info("Deleted {} orphaned texture file(s).", deleted);
        }
    }

    public static synchronized void start(File stateDir) {
        if (instance != null) {
            WawelAuth.LOG.warn("WawelServer already running, ignoring start()");
            return;
        }
        instance = new WawelServer(stateDir);
    }

    public static synchronized void stop() {
        if (instance == null) return;
        WawelAuth.LOG.info("Stopping WawelAuth server module...");
        instance.httpWorkerPool.shutdownNow();
        try {
            instance.database.close();
        } catch (Exception e) {
            WawelAuth.LOG.warn("Error closing database: {}", e.getMessage());
        }
        instance = null;
    }

    public static WawelServer instance() {
        return instance;
    }

    public HttpRouter getRouter() {
        return router;
    }

    public ExecutorService getHttpWorkerPool() {
        return httpWorkerPool;
    }

    public Map<String, Object> consumeLocalHasJoinedProfile(String username, String serverId, String ip) {
        return sessionService.consumeLocalHasJoinedProfile(username, serverId, ip);
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public String getApiLocationHeaderValue() {
        String prefix = serverConfig.getApiRoutePrefix();
        return prefix.isEmpty() ? "/" : prefix;
    }

    public void refreshPublicPageCaches() {
        publicPageService.refreshAllCaches();
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public ProfileDAO getProfileDAO() {
        return profileDAO;
    }

    public TokenDAO getTokenDAO() {
        return tokenDAO;
    }

    public InviteDAO getInviteDAO() {
        return inviteDAO;
    }

    public UserListProviderBindingDAO getUserListProviderBindingDAO() {
        return userListProviderBindingDAO;
    }

    /**
     * Executes the given action inside a database transaction.
     * Rolls back on any exception.
     */
    public void runInTransaction(Runnable action) {
        database.runInTransaction(action);
    }
}
