package industries.angelic.wawelauth.wawelclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

import industries.angelic.wawelauth.wawelclient.data.AccountStatus;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;
import industries.angelic.wawelauth.wawelclient.http.YggdrasilHttpClient;
import industries.angelic.wawelauth.wawelclient.http.YggdrasilRequestException;
import industries.angelic.wawelauth.wawelclient.storage.ClientAccountDAO;
import industries.angelic.wawelauth.wawelclient.storage.ClientProviderDAO;

public class AccountManagerTest {

    private static final String AUTH = "https://alpha.example/authserver";

    private final RecordingHttpClient http = new RecordingHttpClient();
    private final InMemoryAccountDao accounts = new InMemoryAccountDao();
    private final AccountManager manager = new AccountManager(accounts, new SingleProviderDao(), http);

    @After
    public void shutdown() {
        manager.shutdown();
    }

    @Test
    public void launcherSessionIsNotRefreshed() throws Exception {
        long id = accounts.create(account(null));

        AccountStatus status = manager.refreshAccount(id)
            .get(5, TimeUnit.SECONDS);

        Assert.assertEquals(AccountStatus.EXPIRED, status);
        Assert.assertEquals(Arrays.asList(AUTH + "/validate"), http.urls);
        Assert.assertEquals(
            "launcher-token",
            accounts.findById(id)
                .getAccessToken());
    }

    @Test
    public void ownSessionIsRefreshed() throws Exception {
        long id = accounts.create(account("own-client"));

        AccountStatus status = manager.refreshAccount(id)
            .get(5, TimeUnit.SECONDS);

        Assert.assertEquals(AccountStatus.REFRESHED, status);
        Assert.assertEquals(Arrays.asList(AUTH + "/validate", AUTH + "/refresh"), http.urls);
        Assert.assertEquals(
            "refreshed-token",
            accounts.findById(id)
                .getAccessToken());
    }

    @Test
    public void removingLauncherSessionKeepsItValid() throws Exception {
        long id = accounts.create(account(null));

        manager.removeAccount(id)
            .get(5, TimeUnit.SECONDS);

        Assert.assertTrue(http.urls.isEmpty());
        Assert.assertNull(accounts.findById(id));
    }

    @Test
    public void removingOwnSessionInvalidatesIt() throws Exception {
        long id = accounts.create(account("own-client"));

        manager.removeAccount(id)
            .get(5, TimeUnit.SECONDS);

        Assert.assertEquals(Arrays.asList(AUTH + "/invalidate"), http.urls);
    }

    private static ClientAccount account(String clientToken) {
        ClientAccount account = new ClientAccount();
        account.setProviderName("Alpha");
        account.setUserUuid("aaaaaaaa111122223333bbbbbbbbbbbb");
        account.setProfileUuid(UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb"));
        account.setProfileName("Profile");
        account.setAccessToken("launcher-token");
        account.setClientToken(clientToken);
        account.setStatus(AccountStatus.VALID);
        return account;
    }

    /** Rejects every token on /validate and hands out a new one on /refresh. */
    private static final class RecordingHttpClient extends YggdrasilHttpClient {

        private final List<String> urls = new ArrayList<>();

        @Override
        public JsonObject postJson(ClientProvider provider, String url, JsonObject body)
            throws YggdrasilRequestException {
            urls.add(url);
            if (url.endsWith("/validate")) {
                throw new YggdrasilRequestException(403, "ForbiddenOperationException", "Invalid token.");
            }
            if (url.endsWith("/refresh")) {
                JsonObject response = new JsonObject();
                response.addProperty("accessToken", "refreshed-token");
                response.addProperty("clientToken", "own-client");
                return response;
            }
            return null;
        }
    }

    private static final class SingleProviderDao implements ClientProviderDAO {

        private final ClientProvider provider = new ClientProvider();

        private SingleProviderDao() {
            provider.setName("Alpha");
            provider.setAuthServerUrl(AUTH);
        }

        @Override
        public ClientProvider findByName(String name) {
            return provider.getName()
                .equals(name) ? provider : null;
        }

        @Override
        public List<ClientProvider> listAll() {
            return Collections.singletonList(provider);
        }

        @Override
        public void create(ClientProvider provider) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(ClientProvider provider) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rename(String oldName, String newName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            return 1;
        }
    }

    private static final class InMemoryAccountDao implements ClientAccountDAO {

        private final Map<Long, ClientAccount> accounts = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public synchronized ClientAccount findById(long id) {
            return accounts.get(id);
        }

        @Override
        public ClientAccount findByProviderAndUser(String providerName, String userUuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientAccount findByProviderAndProfile(String providerName, UUID profileUuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientAccount findUnboundByProviderAndUser(String providerName, String userUuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ClientAccount> findByProvider(String providerName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized List<ClientAccount> listAll() {
            return new ArrayList<>(accounts.values());
        }

        @Override
        public synchronized long create(ClientAccount account) {
            account.setId(nextId++);
            accounts.put(account.getId(), account);
            return account.getId();
        }

        @Override
        public synchronized void update(ClientAccount account) {
            accounts.put(account.getId(), account);
        }

        @Override
        public synchronized void delete(long id) {
            accounts.remove(id);
        }

        @Override
        public void deleteByProvider(String providerName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized long count() {
            return accounts.size();
        }
    }
}
