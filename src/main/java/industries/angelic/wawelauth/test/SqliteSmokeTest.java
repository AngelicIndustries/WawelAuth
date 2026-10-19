package industries.angelic.wawelauth.test;

import java.io.File;
import java.util.UUID;

import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelcore.data.WawelUser;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteDatabase;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteUserDAO;

public class SqliteSmokeTest {

    public static void run(File configDir) {
        File dbFile = new File(configDir, "wawelauth_test.db");
        SqliteDatabase db = new SqliteDatabase(dbFile);
        try {
            db.initialize();

            SqliteUserDAO userDAO = new SqliteUserDAO(db);
            UUID testUuid = UUID.nameUUIDFromBytes("sqlite-smoke-test".getBytes());

            // Clean up any previous test run
            WawelUser existing = userDAO.findByUuid(testUuid);
            if (existing != null) {
                userDAO.delete(testUuid);
            }

            // Create a test user
            WawelUser user = new WawelUser();
            user.setUuid(testUuid);
            user.setUsername("TestPlayer");
            user.setPasswordHash("not-a-real-hash");
            user.setPasswordSalt("not-a-real-salt");
            user.setCreatedAt(System.currentTimeMillis());
            userDAO.create(user);
            WawelAuth.LOG.info("[SQLite Test] Created test user: {} ({})", user.getUsername(), user.getUuid());

            // Read it back
            WawelUser fetched = userDAO.findByUuid(testUuid);
            if (fetched != null) {
                WawelAuth.LOG.info(
                    "[SQLite Test] Read back user: {} | admin={} | locked={}",
                    fetched.getUsername(),
                    fetched.isAdmin(),
                    fetched.isLocked());
            } else {
                WawelAuth.LOG.error("[SQLite Test] Failed to read back test user!");
            }

            WawelAuth.LOG.info("[SQLite Test] Total users in DB: {}", userDAO.count());

            // Clean up
            userDAO.delete(testUuid);
            WawelAuth.LOG.info("[SQLite Test] Cleaned up test user. Remaining: {}", userDAO.count());
        } catch (Exception e) {
            WawelAuth.LOG.error("[SQLite Test] SQLite smoke test failed!", e);
        } finally {
            db.close();
            if (dbFile.exists()) dbFile.delete();
            new File(dbFile.getPath() + "-wal").delete();
            new File(dbFile.getPath() + "-shm").delete();
            WawelAuth.LOG.info("[SQLite Test] Test DB cleaned up.");
        }
    }
}
