package org.fentanylsolutions.wawelauth.api.skin;

/**
 * Version of the skin API ({@code api.skin} and {@code api.account}). Integrations check it before use; it changes
 * whenever an existing signature or contract changes. Additions keep the version.
 */
public final class WawelSkinApi {

    public static final int VERSION = 1;

    private WawelSkinApi() {}
}
