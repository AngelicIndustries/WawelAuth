package industries.angelic.wawelauth.api.skin;

/**
 * Version of the skin API ({@code api.skin} and {@code api.account}). It changes whenever an existing signature or
 * contract changes; additions keep it.
 * <p>
 * Integrations check {@link #version()} before use, not {@link #VERSION}, which compiles into the integration as the
 * version it was built against. A Wawel Auth from before the API has no such class, so make the call where a
 * {@link LinkageError} is caught.
 */
public final class WawelSkinApi {

    /** The version this build provides. */
    public static final int VERSION = 1;

    private WawelSkinApi() {}

    /** The version of the installed Wawel Auth. */
    public static int version() {
        return VERSION;
    }
}
