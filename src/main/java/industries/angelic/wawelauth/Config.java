package industries.angelic.wawelauth;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import industries.angelic.wawelauth.wawelclient.OsConfigDir;
import industries.angelic.wawelauth.wawelcore.config.FallbackServersConfig;
import industries.angelic.wawelauth.wawelcore.config.JsonConfigIO;
import industries.angelic.wawelauth.wawelcore.config.LocalConfig;
import industries.angelic.wawelauth.wawelcore.config.ServerConfig;
import industries.angelic.wawelauth.wawelserver.ServerStartup;

/**
 * Top-level config loader.
 * <p>
 * Local bootstrap config (always local):
 * <p>
 * - local.json (debug mode + useOsConfigDir)
 * <p>
 * Data configs (local or OS-shared based on local.json):
 * <p>
 * - server.json (Yggdrasil server module settings)
 */
public class Config {

    private static final String CONFIG_DIR_NAME = "wawelauth";
    private static final String LOCAL_CONFIG_FILE = "local.json";
    private static final String SERVER_CONFIG_FILE = "server.json";
    private static final String FALLBACK_SERVERS_CONFIG_FILE = "fallback-servers.json";
    private static final String BUNDLED_FALLBACK_SERVERS = "/assets/wawelauth/default-fallback-servers.json";
    private static File minecraftConfigRoot;
    private static File localConfigDir;
    private static File dataConfigDir;

    // JSON configs
    private static LocalConfig localConfig;
    private static ServerConfig serverConfig;
    private static FallbackServersConfig fallbackServersConfig;

    /**
     * Load all configs from config/wawelauth/.
     *
     * @param mcConfigDir Minecraft's config/ directory
     */
    public static void loadAll(File mcConfigDir) {
        minecraftConfigRoot = mcConfigDir;
        localConfigDir = ensureDirectory(new File(mcConfigDir, CONFIG_DIR_NAME));
        migrateLegacyCfgFile(mcConfigDir);
        File localConfigFile = new File(localConfigDir, LOCAL_CONFIG_FILE);
        boolean localConfigExisted = localConfigFile.exists();
        try {
            localConfig = JsonConfigIO.load(localConfigFile, LocalConfig.class);
        } catch (RuntimeException e) {
            WawelAuth.LOG.warn(
                "Wawel Auth's {} can't be read, so its defaults stand in until it is fixed: {}",
                LOCAL_CONFIG_FILE,
                e.getMessage() != null ? e.getMessage() : e.toString());
            localConfig = new LocalConfig();
        }
        if (!localConfigExisted) {
            importLegacyBootstrapSettings(localConfig);
            JsonConfigIO.save(localConfigFile, localConfig);
        }

        dataConfigDir = ensureDirectory(resolveDataConfigDir(localConfig.isUseOsConfigDir()));

        try {
            serverConfig = JsonConfigIO.load(new File(dataConfigDir, SERVER_CONFIG_FILE), ServerConfig.class);

            // Load fallback servers from separate file. Seed from bundled default if missing.
            File fallbackFile = new File(dataConfigDir, FALLBACK_SERVERS_CONFIG_FILE);
            if (!fallbackFile.exists()) {
                seedFallbackServersConfig(fallbackFile);
            }
            fallbackServersConfig = JsonConfigIO.load(fallbackFile, FallbackServersConfig.class);
            serverConfig.setFallbackServers(fallbackServersConfig.getEnabledFallbackServers());

            serverConfig.validateOrThrow();
            serverConfig.ensureApiRootInSkinDomains();
        } catch (RuntimeException e) {
            serverConfigUnusable(e);
            serverConfig = new ServerConfig();
            fallbackServersConfig = new FallbackServersConfig();
            serverConfig.setFallbackServers(fallbackServersConfig.getEnabledFallbackServers());
        }
    }

    /**
     * server.json or fallback-servers.json can't be read or doesn't check out. A dedicated server refuses to start
     * until it is fixed; elsewhere the server module doesn't run, so its defaults stand in, and the files stay as they
     * are.
     */
    private static void serverConfigUnusable(RuntimeException e) {
        String reason = e.getMessage() != null ? e.getMessage() : e.toString();
        if (!FMLLaunchHandler.side()
            .isServer()) {
            WawelAuth.LOG.warn("Wawel Auth's server config can't be used; only dedicated servers need it: {}", reason);
            return;
        }
        WawelAuth.LOG.error("============================================================");
        WawelAuth.LOG.error("Wawel Auth server module cannot start with its config.");
        WawelAuth.LOG.error("  {}", reason);
        WawelAuth.LOG.error("Required fix:");
        WawelAuth.LOG
            .error("  Correct {} or {} in {},", SERVER_CONFIG_FILE, FALLBACK_SERVERS_CONFIG_FILE, dataConfigDir);
        WawelAuth.LOG.error("  or delete the broken file to regenerate its defaults.");
        WawelAuth.LOG.error("============================================================");
        ServerStartup.refuse();
    }

    public static void reload() {
        if (minecraftConfigRoot == null) {
            WawelAuth.LOG.warn("Config.reload() called before initial loadAll().");
            return;
        }
        loadAll(minecraftConfigRoot);
    }

    /**
     * Resolves the directory used for non-bootstrap JSON configs.
     * On dedicated server this always resolves to local config/wawelauth/.
     */
    public static File resolveDataConfigDir(boolean useOsConfigDir) {
        if (useOsConfigDir && FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            return OsConfigDir.resolve();
        }
        if (localConfigDir != null) {
            return localConfigDir;
        }
        if (minecraftConfigRoot != null) {
            return new File(minecraftConfigRoot, CONFIG_DIR_NAME);
        }
        return new File(CONFIG_DIR_NAME);
    }

    /**
     * Migrate legacy config/wawelauth.cfg to config/wawelauth/wawelauth.cfg.
     * Never overwrites an existing destination file.
     */
    private static void migrateLegacyCfgFile(File mcConfigDir) {
        File legacyFile = new File(mcConfigDir, CONFIG_DIR_NAME + ".cfg");
        if (!legacyFile.exists()) {
            return;
        }
        File destination = new File(localConfigDir, CONFIG_DIR_NAME + ".cfg");
        if (destination.exists()) {
            return;
        }
        if (legacyFile.renameTo(destination)) {
            WawelAuth.LOG.info("Migrated {} -> {}", legacyFile.getAbsolutePath(), destination.getAbsolutePath());
        } else {
            WawelAuth.LOG.warn(
                "Failed to migrate legacy config file {} to {}",
                legacyFile.getAbsolutePath(),
                destination.getAbsolutePath());
        }
    }

    private static File ensureDirectory(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            WawelAuth.LOG.error("Failed to create config directory: {}", dir);
        }
        return dir;
    }

    private static void seedFallbackServersConfig(File destination) {
        try (java.io.InputStream in = Config.class.getResourceAsStream(BUNDLED_FALLBACK_SERVERS)) {
            if (in == null) {
                WawelAuth.LOG.warn("Bundled fallback-servers.json not found, creating empty");
                JsonConfigIO.save(destination, new FallbackServersConfig());
                return;
            }
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int read;
            while ((read = in.read(tmp)) != -1) {
                buf.write(tmp, 0, read);
            }
            java.nio.file.Files.write(destination.toPath(), buf.toByteArray());
            WawelAuth.LOG.info("Created default {}", FALLBACK_SERVERS_CONFIG_FILE);
        } catch (Exception e) {
            WawelAuth.LOG.warn("Failed to seed {}: {}", FALLBACK_SERVERS_CONFIG_FILE, e.getMessage());
            JsonConfigIO.save(destination, new FallbackServersConfig());
        }
    }

    private static void importLegacyBootstrapSettings(LocalConfig target) {
        if (target == null || localConfigDir == null) {
            return;
        }
        boolean legacyUseOs = target.isUseOsConfigDir();
        boolean legacyDebug = target.isDebugMode();

        JsonObject legacyClient = parseJsonObject(new File(localConfigDir, "client.json"));
        if (legacyClient != null) {
            if (legacyClient.has("useOsConfigDir")) {
                legacyUseOs = getBoolean(legacyClient, "useOsConfigDir", legacyUseOs);
            }
            if (legacyClient.has("debugMode")) {
                legacyDebug = legacyDebug || getBoolean(legacyClient, "debugMode", false);
            }
        }

        JsonObject legacyServer = parseJsonObject(new File(localConfigDir, SERVER_CONFIG_FILE));
        if (legacyServer != null && legacyServer.has("debugMode")) {
            legacyDebug = legacyDebug || getBoolean(legacyServer, "debugMode", false);
        }

        target.setUseOsConfigDir(legacyUseOs);
        target.setDebugMode(legacyDebug);
    }

    private static JsonObject parseJsonObject(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            WawelAuth.LOG.warn("Failed to parse legacy config '{}': {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        try {
            return obj.get(key)
                .getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static LocalConfig local() {
        return localConfig;
    }

    public static ServerConfig server() {
        return serverConfig;
    }

    public static File getLocalConfigDir() {
        return localConfigDir;
    }

    public static File getDataConfigDir() {
        return dataConfigDir;
    }

    /** server.json, which holds the server module's settings. */
    public static File serverConfigFile() {
        return new File(dataConfigDir, SERVER_CONFIG_FILE);
    }

    public static File getMinecraftConfigRoot() {
        return minecraftConfigRoot;
    }

    /**
     * Compatibility alias: returns the active data config dir.
     */
    public static File getConfigDir() {
        return dataConfigDir;
    }
}
