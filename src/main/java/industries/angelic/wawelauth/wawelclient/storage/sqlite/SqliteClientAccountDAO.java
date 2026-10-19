package industries.angelic.wawelauth.wawelclient.storage.sqlite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import industries.angelic.wawelauth.wawelclient.data.AccountStatus;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;
import industries.angelic.wawelauth.wawelclient.storage.ClientAccountDAO;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.EnumUtil;
import industries.angelic.wawelauth.wawelcore.storage.sqlite.SqliteDatabase;

public class SqliteClientAccountDAO implements ClientAccountDAO {

    private final SqliteDatabase db;

    public SqliteClientAccountDAO(SqliteDatabase db) {
        this.db = db;
    }

    private ClientAccount mapRow(ResultSet rs) throws SQLException {
        ClientAccount a = new ClientAccount();
        a.setId(rs.getLong("id"));
        a.setProviderName(rs.getString("provider_name"));
        a.setUserUuid(rs.getString("user_uuid"));
        String profileUuid = rs.getString("profile_uuid");
        if (profileUuid != null) {
            a.setProfileUuid(UUID.fromString(profileUuid));
        }
        a.setProfileName(rs.getString("profile_name"));
        a.setAccessToken(rs.getString("access_token"));
        a.setRefreshToken(rs.getString("refresh_token"));
        a.setClientToken(rs.getString("client_token"));
        a.setUserPropertiesJson(rs.getString("user_properties"));
        a.setStatus(EnumUtil.parseOrDefault(AccountStatus.class, rs.getString("status"), AccountStatus.EXPIRED));
        a.setLastError(rs.getString("last_error"));
        a.setLastErrorAt(rs.getLong("last_error_at"));
        a.setLastRefreshAttemptAt(rs.getLong("last_refresh_attempt_at"));
        a.setConsecutiveFailures(rs.getInt("consecutive_failures"));
        a.setCreatedAt(rs.getLong("created_at"));
        a.setLastValidatedAt(rs.getLong("last_validated_at"));
        a.setTokenIssuedAt(rs.getLong("token_issued_at"));
        a.setLocalSkinPath(rs.getString("local_skin_path"));
        a.setLocalSkinModel(EnumUtil.parseOrDefault(SkinModel.class, rs.getString("local_skin_model"), null));
        a.setLocalCapePath(rs.getString("local_cape_path"));
        return a;
    }

    @Override
    public ClientAccount findById(long id) {
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapRow(rs) : null;
                }
            }
        });
    }

    @Override
    public ClientAccount findByProviderAndUser(String providerName, String userUuid) {
        return db.query(conn -> {
            try (PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM accounts WHERE provider_name = ? AND user_uuid = ?")) {
                ps.setString(1, providerName);
                ps.setString(2, userUuid);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapRow(rs) : null;
                }
            }
        });
    }

    @Override
    public ClientAccount findByProviderAndProfile(String providerName, UUID profileUuid) {
        return db.query(conn -> {
            try (PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM accounts WHERE provider_name = ? AND profile_uuid = ?")) {
                ps.setString(1, providerName);
                ps.setString(2, profileUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapRow(rs) : null;
                }
            }
        });
    }

    @Override
    public ClientAccount findUnboundByProviderAndUser(String providerName, String userUuid) {
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM accounts WHERE provider_name = ? AND user_uuid = ? AND profile_uuid IS NULL")) {
                ps.setString(1, providerName);
                ps.setString(2, userUuid);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapRow(rs) : null;
                }
            }
        });
    }

    @Override
    public List<ClientAccount> findByProvider(String providerName) {
        return db.query(conn -> {
            try (PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM accounts WHERE provider_name = ? ORDER BY last_validated_at DESC")) {
                ps.setString(1, providerName);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ClientAccount> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(mapRow(rs));
                    }
                    return result;
                }
            }
        });
    }

    @Override
    public List<ClientAccount> listAll() {
        return db.query(conn -> {
            try (PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM accounts ORDER BY last_validated_at DESC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<ClientAccount> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(mapRow(rs));
                    }
                    return result;
                }
            }
        });
    }

    @Override
    public long create(ClientAccount a) {
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO accounts (provider_name, user_uuid, profile_uuid, profile_name, "
                    + "access_token, refresh_token, client_token, user_properties, "
                    + "status, last_error, last_error_at, last_refresh_attempt_at, consecutive_failures, "
                    + "created_at, last_validated_at, token_issued_at, local_skin_path, local_skin_model, local_cape_path) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, a.getProviderName());
                ps.setString(2, a.getUserUuid());
                ps.setString(
                    3,
                    a.getProfileUuid() != null ? a.getProfileUuid()
                        .toString() : null);
                ps.setString(4, a.getProfileName());
                ps.setString(5, a.getAccessToken());
                ps.setString(6, a.getRefreshToken());
                ps.setString(7, a.getClientToken());
                ps.setString(8, a.getUserPropertiesJson());
                ps.setString(
                    9,
                    a.getStatus()
                        .name());
                ps.setString(10, a.getLastError());
                ps.setLong(11, a.getLastErrorAt());
                ps.setLong(12, a.getLastRefreshAttemptAt());
                ps.setInt(13, a.getConsecutiveFailures());
                ps.setLong(14, a.getCreatedAt());
                ps.setLong(15, a.getLastValidatedAt());
                ps.setLong(16, a.getTokenIssuedAt());
                ps.setString(17, a.getLocalSkinPath());
                ps.setString(
                    18,
                    a.getLocalSkinModel() != null ? a.getLocalSkinModel()
                        .name() : null);
                ps.setString(19, a.getLocalCapePath());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getLong(1);
                    throw new RuntimeException("No generated key returned for account insert");
                }
            }
        });
    }

    @Override
    public void update(ClientAccount a) {
        db.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE accounts SET provider_name = ?, user_uuid = ?, profile_uuid = ?, profile_name = ?, "
                    + "access_token = ?, refresh_token = ?, client_token = ?, user_properties = ?, status = ?, last_error = ?, last_error_at = ?, "
                    + "last_refresh_attempt_at = ?, consecutive_failures = ?, created_at = ?, "
                    + "last_validated_at = ?, token_issued_at = ?, local_skin_path = ?, local_skin_model = ?, local_cape_path = ? WHERE id = ?")) {
                ps.setString(1, a.getProviderName());
                ps.setString(2, a.getUserUuid());
                ps.setString(
                    3,
                    a.getProfileUuid() != null ? a.getProfileUuid()
                        .toString() : null);
                ps.setString(4, a.getProfileName());
                ps.setString(5, a.getAccessToken());
                ps.setString(6, a.getRefreshToken());
                ps.setString(7, a.getClientToken());
                ps.setString(8, a.getUserPropertiesJson());
                ps.setString(
                    9,
                    a.getStatus()
                        .name());
                ps.setString(10, a.getLastError());
                ps.setLong(11, a.getLastErrorAt());
                ps.setLong(12, a.getLastRefreshAttemptAt());
                ps.setInt(13, a.getConsecutiveFailures());
                ps.setLong(14, a.getCreatedAt());
                ps.setLong(15, a.getLastValidatedAt());
                ps.setLong(16, a.getTokenIssuedAt());
                ps.setString(17, a.getLocalSkinPath());
                ps.setString(
                    18,
                    a.getLocalSkinModel() != null ? a.getLocalSkinModel()
                        .name() : null);
                ps.setString(19, a.getLocalCapePath());
                ps.setLong(20, a.getId());
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Account not found: " + a.getId());
            }
        });
    }

    @Override
    public void delete(long id) {
        db.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void deleteByProvider(String providerName) {
        db.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE provider_name = ?")) {
                ps.setString(1, providerName);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public long count() {
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM accounts");
                ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        });
    }
}
