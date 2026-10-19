package industries.angelic.wawelauth.wawelclient;

import java.io.File;

import industries.angelic.wawelauth.Config;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;
import industries.angelic.wawelauth.wawelcore.config.JsonConfigIO;
import industries.angelic.wawelauth.wawelcore.config.LocalConfig;

/**
 * Persists the singleplayer account selected from the world list screen.
 */
public final class SingleplayerAccountPersistence {

    private static final String FILE_NAME = "singleplayer_account.json";
    private static SelectionConfig config;

    private SingleplayerAccountPersistence() {}

    public static synchronized long getSelectedAccountId() {
        return getConfig().accountId;
    }

    public static synchronized void setSelectedAccountId(long accountId) {
        SelectionConfig current = getConfig();
        current.accountId = accountId;
        save(current);
    }

    public static synchronized void clearSelection() {
        setSelectedAccountId(-1L);
    }

    public static synchronized ClientAccount resolveSelectedAccount(AccountManager accountManager) {
        if (accountManager == null) {
            return null;
        }

        long accountId = getSelectedAccountId();
        if (accountId < 0L) {
            return null;
        }

        ClientAccount account = accountManager.getAccount(accountId);
        if (account != null) {
            return account;
        }

        clearSelection();
        return null;
    }

    public static synchronized boolean clearMissingSelection(AccountManager accountManager) {
        if (accountManager == null) {
            return false;
        }

        long accountId = getSelectedAccountId();
        if (accountId < 0L) {
            return false;
        }

        if (accountManager.getAccount(accountId) != null) {
            return false;
        }

        clearSelection();
        return true;
    }

    private static SelectionConfig getConfig() {
        if (config == null) {
            config = JsonConfigIO.load(resolveStorageFile(), SelectionConfig.class);
        }
        return config;
    }

    private static void save(SelectionConfig current) {
        config = current;
        JsonConfigIO.save(resolveStorageFile(), current);
    }

    private static File resolveStorageFile() {
        File baseDir;
        LocalConfig local = Config.local();
        if (local != null && local.isUseOsConfigDir()) {
            baseDir = Config.getDataConfigDir();
        } else {
            baseDir = new File(Config.getConfigDir(), "client");
        }
        if (baseDir != null && !baseDir.exists() && !baseDir.mkdirs()) {
            WawelAuth.LOG.warn("Failed to create singleplayer account config directory: {}", baseDir);
        }
        return new File(baseDir, FILE_NAME);
    }

    public static final class SelectionConfig {

        public long accountId = -1L;

        public SelectionConfig() {}
    }
}
