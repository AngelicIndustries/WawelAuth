package industries.angelic.wawelauth.api.account;

import java.util.UUID;

/** An account as integrations see it. Immutable; a snapshot taken by {@link WawelAccounts#active()}. */
public final class AccountInfo {

    private final long id;
    private final String profileName;
    private final UUID profileUuid;
    private final String providerName;
    private final boolean offline;

    public AccountInfo(long id, String profileName, UUID profileUuid, String providerName, boolean offline) {
        this.id = id;
        this.profileName = profileName;
        this.profileUuid = profileUuid;
        this.providerName = providerName;
        this.offline = offline;
    }

    public long id() {
        return id;
    }

    public String profileName() {
        return profileName;
    }

    /** Null when the account has no bound profile yet. */
    public UUID profileUuid() {
        return profileUuid;
    }

    public String providerName() {
        return providerName;
    }

    /** Offline accounts keep their skin locally instead of uploading it. */
    public boolean offline() {
        return offline;
    }
}
