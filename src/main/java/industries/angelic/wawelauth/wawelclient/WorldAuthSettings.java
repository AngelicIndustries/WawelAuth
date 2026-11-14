package industries.angelic.wawelauth.wawelclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;

import industries.angelic.angellib.util.StringUtil;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;
import industries.angelic.wawelauth.wawelcore.config.JsonConfigIO;

/**
 * Wawel Auth's settings for one singleplayer world, kept in the world's folder so they move with it: the account it's
 * played with and who may join it over LAN. The account is stored by provider and UUID, not by this game's account
 * id, so a copied world finds the same account on another computer.
 */
public final class WorldAuthSettings {

    private static final String DIRECTORY = "wawelauth";
    private static final String FILE_NAME = "world.json";
    private static final Gson GSON = new Gson();
    private static final Map<File, Cached> CACHE = new HashMap<>();

    private WorldAuthSettings() {}

    public static final class Data {

        AccountRef account;
        /** Provider name, lower case, to ticked; only providers the player changed. */
        Map<String, Boolean> lanProviders = new LinkedHashMap<>();
        /** Lets players no ticked provider verifies join over LAN as offline players, as vanilla does. */
        Boolean lanLetOthersIn;

        public Data() {}

        public Map<String, Boolean> getLanProviders() {
            return lanProviders;
        }

        public boolean letsOthersIn() {
            return Boolean.TRUE.equals(lanLetOthersIn);
        }

        public void setLetsOthersIn(boolean letOthersIn) {
            this.lanLetOthersIn = letOthersIn ? Boolean.TRUE : null;
        }
    }

    static final class AccountRef {

        String provider;
        String uuid;
        String name;

        public AccountRef() {}
    }

    /** Where a world's account comes from. */
    public enum AccountSource {
        /** Chosen for this world. */
        WORLD,
        /** The singleplayer default chosen from the world list's Account button. */
        DEFAULT,
        /** The account the launcher started the game with. */
        AUTOMATIC
    }

    public static final class WorldAccount {

        public final ClientAccount account;
        public final AccountSource source;

        WorldAccount(ClientAccount account, AccountSource source) {
            this.account = account;
            this.source = source;
        }
    }

    /** The account a world is played with: its own, else the singleplayer default, else the automatic one. */
    public static WorldAccount resolveAccount(File worldDirectory, WawelClient client) {
        if (client == null) return null;
        ClientAccount account = worldDirectory != null ? account(worldDirectory, client.getAccountManager()) : null;
        if (account != null) return new WorldAccount(account, AccountSource.WORLD);
        account = SingleplayerAccountPersistence.resolveSelectedAccount(client.getAccountManager());
        if (account != null) return new WorldAccount(account, AccountSource.DEFAULT);
        SessionBridge.AutomaticAccount automatic = client.getSessionBridge()
            .peekAccountForUnboundServer(null);
        return automatic != null ? new WorldAccount(automatic.account, AccountSource.AUTOMATIC) : null;
    }

    public static File worldDirectory(String folderName) {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, "saves"), folderName);
    }

    /** The world's settings; a copy, read again only when the file changed. */
    public static synchronized Data load(File worldDirectory) {
        File file = file(worldDirectory);
        if (!file.isFile()) {
            CACHE.remove(file);
            return new Data();
        }
        Cached cached = CACHE.get(file);
        if (cached == null || cached.modified != file.lastModified() || cached.length != file.length()) {
            cached = new Cached(file.lastModified(), file.length(), read(file, worldDirectory));
            CACHE.put(file, cached);
        }
        return GSON.fromJson(cached.json, Data.class);
    }

    public static synchronized void save(File worldDirectory, Data data) {
        File file = file(worldDirectory);
        if (data.account == null && data.lanProviders.isEmpty() && !data.letsOthersIn() && !file.exists()) {
            return;
        }
        JsonConfigIO.save(file, data);
        CACHE.remove(file);
    }

    private static String read(File file, File worldDirectory) {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data == null) data = new Data();
            if (data.lanProviders == null) data.lanProviders = new LinkedHashMap<>();
            return GSON.toJson(data);
        } catch (IOException | RuntimeException e) {
            WawelAuth.LOG
                .warn("Couldn't read Wawel Auth's settings of world {}: {}", worldDirectory.getName(), e.getMessage());
            return GSON.toJson(new Data());
        }
    }

    private static final class Cached {

        final long modified;
        final long length;
        final String json;

        Cached(long modified, long length, String json) {
            this.modified = modified;
            this.length = length;
            this.json = json;
        }
    }

    /** The account chosen for the world, or null when it uses the default one or the account is gone. */
    public static ClientAccount account(File worldDirectory, AccountManager accountManager) {
        AccountRef ref = load(worldDirectory).account;
        if (ref == null || accountManager == null || ref.provider == null) {
            return null;
        }
        UUID uuid = parseUuid(ref.uuid);
        for (ClientAccount account : accountManager.listAccounts()) {
            if (!ref.provider.equalsIgnoreCase(account.getProviderName())) continue;
            if (uuid != null ? uuid.equals(account.getProfileUuid())
                : ref.name != null && ref.name.equalsIgnoreCase(account.getProfileName())) {
                return account;
            }
        }
        return null;
    }

    public static synchronized boolean hasAccount(File worldDirectory) {
        return load(worldDirectory).account != null;
    }

    /** Plays the world with an account, or with the default one for null. */
    public static synchronized void setAccount(File worldDirectory, ClientAccount account) {
        Data data = load(worldDirectory);
        if (account == null) {
            data.account = null;
        } else {
            AccountRef ref = new AccountRef();
            ref.provider = account.getProviderName();
            ref.uuid = account.getProfileUuid() != null ? account.getProfileUuid()
                .toString() : null;
            ref.name = account.getProfileName();
            data.account = ref;
        }
        save(worldDirectory, data);
    }

    private static File file(File worldDirectory) {
        return new File(new File(worldDirectory, DIRECTORY), FILE_NAME);
    }

    private static UUID parseUuid(String value) {
        String trimmed = StringUtil.trimToNull(value);
        if (trimmed == null) return null;
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
