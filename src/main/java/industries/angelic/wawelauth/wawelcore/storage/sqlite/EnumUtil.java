package industries.angelic.wawelauth.wawelcore.storage.sqlite;

import industries.angelic.wawelauth.WawelAuth;

/**
 * Safe enum parsing for database values.
 * Handles corrupted or migrated DB rows without crashing the entire DAO call.
 */
public final class EnumUtil {

    private EnumUtil() {}

    /**
     * Parse an enum value from a database string, returning a default if parsing fails.
     * Logs a warning on bad values instead of throwing.
     */
    public static <E extends Enum<E>> E parseOrDefault(Class<E> enumType, String value, E defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            WawelAuth.LOG.warn(
                "Unknown {} value in database: '{}', defaulting to {}",
                enumType.getSimpleName(),
                value,
                defaultValue);
            return defaultValue;
        }
    }
}
