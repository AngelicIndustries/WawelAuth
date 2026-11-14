package industries.angelic.wawelauth.wawelclient;

import java.io.File;

import industries.angelic.wawelauth.wawelclient.data.ClientAccount;

/**
 * The account picked on the world creation screen, held until the world is created, then stored as the world's own
 * account in the folder the world is launched in. Mods may still rename that folder when Create is pressed
 * (Hodgepodge does if it exists), so the pick waits for the launch.
 */
public final class NewWorldAccount {

    private static long accountId = -1L;
    /** Create was pressed: the next world launched is the new one. */
    private static boolean creating;

    private NewWorldAccount() {}

    /** A new world creation screen starts without a pick. */
    public static void reset() {
        accountId = -1L;
        creating = false;
    }

    /** Create was pressed on the world creation screen. */
    public static void creating() {
        creating = accountId >= 0;
    }

    public static void set(ClientAccount account) {
        accountId = account != null ? account.getId() : -1L;
    }

    public static long id() {
        return accountId;
    }

    /** The picked account, or null when there's none or it was removed meanwhile. */
    public static ClientAccount get(AccountManager accountManager) {
        return accountId < 0 || accountManager == null ? null : accountManager.getAccount(accountId);
    }

    /** When a world is launched right after Create, stores the pick as its account before the world loads. */
    public static void bindIfCreating(File worldDirectory, AccountManager accountManager) {
        if (!creating) return;
        ClientAccount account = get(accountManager);
        if (account != null) {
            WorldAuthSettings.setAccount(worldDirectory, account);
        }
        reset();
    }
}
