package industries.angelic.wawelauth.wawelcore.storage;

import java.util.Map;
import java.util.UUID;

import industries.angelic.wawelauth.wawelcore.data.ProviderAwareUserListType;

/**
 * Stores provider metadata for vanilla UserList entries.
 */
public interface UserListProviderBindingDAO {

    Map<UUID, String> findAllProviderKeys(ProviderAwareUserListType listType);

    void putProviderKey(ProviderAwareUserListType listType, UUID profileUuid, String providerKey);

    void delete(ProviderAwareUserListType listType, UUID profileUuid);
}
