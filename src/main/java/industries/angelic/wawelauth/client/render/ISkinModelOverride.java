package industries.angelic.wawelauth.client.render;

import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/**
 * Optional hook for entities that want to force a resolved skin model.
 */
public interface ISkinModelOverride {

    SkinModel wawelauth$getForcedSkinModel();
}
