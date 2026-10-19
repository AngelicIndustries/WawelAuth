package industries.angelic.wawelauth.wawelcore.skin;

/**
 * Duck interface injected into EntityPlayer. Tracks whether something other than the wearer's own choice hides the
 * cape, for example an elytra. Not synced; each side keeps its own value.
 */
public interface IPlayerCapeState {

    boolean wawelauth$isCapeHiddenByEquipment();

    void wawelauth$setCapeHiddenByEquipment(boolean hidden);
}
